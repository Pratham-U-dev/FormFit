package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.viewmodel.LeaderboardEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class FirebaseLeaderboardRepository(private val context: Context) {

    private val TAG = "SupabaseLeaderboard"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("formfit_leaderboard_prefs", Context.MODE_PRIVATE)

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<FirestoreUser?>(null)
    val currentUserProfile: StateFlow<FirestoreUser?> = _currentUserProfile.asStateFlow()

    private val _needsDisplayNamePrompt = MutableStateFlow(false)
    val needsDisplayNamePrompt: StateFlow<Boolean> = _needsDisplayNamePrompt.asStateFlow()

    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private val _supabaseUrl = MutableStateFlow(prefs.getString("supabase_url", "") ?: "")
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseAnonKey = MutableStateFlow(prefs.getString("supabase_anon_key", "") ?: "")
    val supabaseAnonKey: StateFlow<String> = _supabaseAnonKey.asStateFlow()

    private val _isSupabaseConnected = MutableStateFlow(false)
    val isSupabaseConnected: StateFlow<Boolean> = _isSupabaseConnected.asStateFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        initRepository()
    }

    private fun initRepository() {
        var localUid = prefs.getString("local_user_uid", null)
        if (localUid.isNullOrBlank()) {
            localUid = "u_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString("local_user_uid", localUid).apply()
        }
        _currentUid.value = localUid

        val savedName = prefs.getString("local_display_name", "") ?: ""
        val savedXp = prefs.getInt("local_user_xp", 0)
        val savedLevel = (savedXp / 350) + 1
        val savedWorkouts = prefs.getInt("local_user_workouts", 0)
        val savedReps = prefs.getInt("local_user_reps", 0)
        val savedAvgForm = prefs.getInt("local_user_avg_form", 0)
        val savedStreak = prefs.getInt("local_user_streak", 1)
        val savedAvatar = prefs.getString("local_user_avatar", "💪") ?: "💪"

        if (savedName.isBlank()) {
            _needsDisplayNamePrompt.value = true
        } else {
            _needsDisplayNamePrompt.value = false
            _currentUserProfile.value = FirestoreUser(
                uid = localUid,
                displayName = savedName,
                level = savedLevel,
                xp = savedXp,
                workoutCount = savedWorkouts,
                totalReps = savedReps,
                averageFormScore = savedAvgForm,
                streak = savedStreak,
                avatarIcon = savedAvatar
            )
        }

        // Start background synchronization & live listener loop
        scope.launch {
            while (isActive) {
                try {
                    syncAndFetchLeaderboard()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in leaderboard sync loop: ${e.message}")
                }
                delay(8000) // Refresh every 8 seconds
            }
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanKey = anonKey.trim()

        prefs.edit()
            .putString("supabase_url", cleanUrl)
            .putString("supabase_anon_key", cleanKey)
            .apply()

        _supabaseUrl.value = cleanUrl
        _supabaseAnonKey.value = cleanKey

        scope.launch {
            syncAndFetchLeaderboard()
        }
    }

    fun saveDisplayName(
        displayName: String,
        initialStats: UserStats? = null,
        totalWorkoutSessions: Int = 0,
        totalRepsCount: Int = 0,
        averageForm: Int = 0
    ) {
        val cleanName = displayName.trim()
        if (cleanName.isBlank()) return

        val uid = _currentUid.value ?: return

        val currentXp = initialStats?.currentXp ?: prefs.getInt("local_user_xp", 0)
        val currentLevel = (currentXp / 350) + 1
        val streak = initialStats?.streakDays ?: prefs.getInt("local_user_streak", 1)

        prefs.edit()
            .putString("local_display_name", cleanName)
            .putInt("local_user_xp", currentXp)
            .putInt("local_user_workouts", totalWorkoutSessions)
            .putInt("local_user_reps", totalRepsCount)
            .putInt("local_user_avg_form", averageForm)
            .putInt("local_user_streak", streak)
            .apply()

        val updatedProfile = FirestoreUser(
            uid = uid,
            displayName = cleanName,
            level = currentLevel,
            xp = currentXp,
            workoutCount = totalWorkoutSessions,
            totalReps = totalRepsCount,
            averageFormScore = averageForm,
            streak = streak,
            avatarIcon = "💪"
        )

        _currentUserProfile.value = updatedProfile
        _needsDisplayNamePrompt.value = false

        scope.launch {
            syncUserToCloudInternal(updatedProfile)
            syncAndFetchLeaderboard()
        }
    }

    fun syncUserWorkoutCompleted(
        xpEarnedInWorkout: Int,
        totalWorkoutCount: Int,
        totalRepsCount: Int,
        overallAvgFormScore: Int,
        streakCount: Int,
        todayDateStr: String,
        unlockedBadgesList: List<String>
    ) {
        val uid = _currentUid.value ?: return
        val currentName = prefs.getString("local_display_name", "") ?: ""

        val currentXp = prefs.getInt("local_user_xp", 0) + xpEarnedInWorkout
        val newLevel = (currentXp / 350) + 1

        prefs.edit()
            .putInt("local_user_xp", currentXp)
            .putInt("local_user_workouts", totalWorkoutCount)
            .putInt("local_user_reps", totalRepsCount)
            .putInt("local_user_avg_form", overallAvgFormScore)
            .putInt("local_user_streak", streakCount)
            .apply()

        val updatedProfile = FirestoreUser(
            uid = uid,
            displayName = currentName,
            level = newLevel,
            xp = currentXp,
            workoutCount = totalWorkoutCount,
            totalReps = totalRepsCount,
            averageFormScore = overallAvgFormScore,
            streak = streakCount,
            lastWorkoutDate = todayDateStr,
            achievements = unlockedBadgesList,
            avatarIcon = _currentUserProfile.value?.avatarIcon ?: "💪"
        )

        _currentUserProfile.value = updatedProfile

        if (currentName.isNotBlank()) {
            scope.launch {
                syncUserToCloudInternal(updatedProfile)
                syncAndFetchLeaderboard()
            }
        }
    }

    private suspend fun syncAndFetchLeaderboard() = withContext(Dispatchers.IO) {
        val currentProfile = _currentUserProfile.value
        if (currentProfile != null && currentProfile.displayName.isNotBlank()) {
            syncUserToCloudInternal(currentProfile)
        }

        val url = _supabaseUrl.value
        val anonKey = _supabaseAnonKey.value

        var remoteUsers: List<FirestoreUser>? = null

        if (url.isNotBlank() && anonKey.isNotBlank()) {
            remoteUsers = fetchUsersFromSupabase(url, anonKey)
            if (remoteUsers != null) {
                _isSupabaseConnected.value = true
            } else {
                _isSupabaseConnected.value = false
            }
        } else {
            _isSupabaseConnected.value = false
        }

        val usersList = mutableListOf<FirestoreUser>()

        if (remoteUsers != null && remoteUsers.isNotEmpty()) {
            usersList.addAll(remoteUsers)
        } else {
            // Default benchmark players when Supabase is not configured yet
            usersList.addAll(getBenchmarkUsers())
        }

        val myUid = _currentUid.value
        val myProfile = _currentUserProfile.value

        // Ensure current user is included if local profile exists
        if (myProfile != null && myProfile.displayName.isNotBlank()) {
            val exists = usersList.any { it.uid == myUid || (it.displayName.equals(myProfile.displayName, ignoreCase = true)) }
            if (!exists) {
                usersList.add(myProfile)
            } else {
                // Update local user's entry with latest stats
                val idx = usersList.indexOfFirst { it.uid == myUid || (it.displayName.equals(myProfile.displayName, ignoreCase = true)) }
                if (idx >= 0) {
                    usersList[idx] = myProfile
                }
            }
        }

        // Sort by XP descending
        val sortedUsers = usersList
            .distinctBy { if (it.uid.isNotBlank()) it.uid else it.displayName }
            .sortedByDescending { it.xp }

        val entries = sortedUsers.mapIndexed { index, user ->
            val isMe = (user.uid == myUid) || (myProfile != null && user.displayName.equals(myProfile.displayName, ignoreCase = true))
            LeaderboardEntry(
                name = if (isMe && user.displayName.isBlank()) "You" else user.displayName,
                xp = user.xp,
                rank = index + 1,
                characterIcon = if (user.avatarIcon.isNotBlank()) user.avatarIcon else "💪",
                isUser = isMe
            )
        }

        _leaderboardEntries.value = entries
    }

    private fun syncUserToCloudInternal(user: FirestoreUser) {
        if (user.displayName.isBlank()) return

        val url = _supabaseUrl.value
        val anonKey = _supabaseAnonKey.value

        if (url.isNotBlank() && anonKey.isNotBlank()) {
            syncToSupabase(url, anonKey, user)
        }
    }

    private fun syncToSupabase(baseUrl: String, anonKey: String, user: FirestoreUser) {
        try {
            val bodyJson = JSONObject().apply {
                put("uid", user.uid)
                put("display_name", user.displayName)
                put("xp", user.xp)
                put("level", user.level)
                put("workout_count", user.workoutCount)
                put("total_reps", user.totalReps)
                put("average_form_score", user.averageFormScore)
                put("streak", user.streak)
                put("avatar_icon", user.avatarIcon)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/leaderboard")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .header("Prefer", "resolution=merge-duplicates")
                .post(bodyJson.toString().toRequestBody(mediaType))
                .build()

            okHttpClient.newCall(request).execute().close()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Supabase: ${e.message}")
        }
    }

    private fun fetchUsersFromSupabase(baseUrl: String, anonKey: String): List<FirestoreUser>? {
        val result = mutableListOf<FirestoreUser>()
        try {
            val request = Request.Builder()
                .url("$baseUrl/rest/v1/leaderboard?select=*&order=xp.desc&limit=100")
                .header("apikey", anonKey)
                .header("Authorization", "Bearer $anonKey")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val respStr = response.body?.string() ?: return null
                val array = JSONArray(respStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val uid = obj.optString("uid")
                    val displayName = obj.optString("display_name")
                    if (displayName.isBlank()) continue

                    val xp = obj.optInt("xp", 0)
                    val level = obj.optInt("level", 1)
                    val workoutCount = obj.optInt("workout_count", 0)
                    val totalReps = obj.optInt("total_reps", 0)
                    val avgForm = obj.optInt("average_form_score", 0)
                    val streak = obj.optInt("streak", 1)
                    val avatar = obj.optString("avatar_icon", "💪")

                    result.add(
                        FirestoreUser(
                            uid = uid,
                            displayName = displayName,
                            level = level,
                            xp = xp,
                            workoutCount = workoutCount,
                            totalReps = totalReps,
                            averageFormScore = avgForm,
                            streak = streak,
                            avatarIcon = avatar
                        )
                    )
                }
                return result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Supabase: ${e.message}")
            return null
        }
    }

    private fun getBenchmarkUsers(): List<FirestoreUser> {
        return listOf(
            FirestoreUser("bench_1", "Alex (Apex)", 4, 1450, 28, 520, 96, 14, "", listOf(), "🦉"),
            FirestoreUser("bench_2", "Sarah (Pro)", 3, 1120, 21, 390, 91, 8, "", listOf(), "👧"),
            FirestoreUser("bench_3", "Devin (Beast)", 3, 850, 16, 310, 89, 6, "", listOf(), "👱‍♀️"),
            FirestoreUser("bench_4", "Marcus (Titan)", 2, 540, 10, 180, 85, 4, "", listOf(), "🧔"),
            FirestoreUser("bench_5", "Elena (Speed)", 1, 310, 6, 95, 82, 2, "", listOf(), "👨‍🎨")
        )
    }
}


