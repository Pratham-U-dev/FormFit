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

    private val TAG = "CentralLeaderboard"
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

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val MASTER_REGISTRY_ID = "ff8081819f7e10ae019f9f66fe912cfe"
        private const val BASE_URL = "https://api.restful-api.dev/objects"
    }

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
                delay(10000) // refresh every 10 seconds
            }
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

        val registeredObjectIds = getMasterRegistryMemberIds()
        val usersList = mutableListOf<FirestoreUser>()

        if (registeredObjectIds.isNotEmpty()) {
            val queriedUsers = fetchUsersFromCloud(registeredObjectIds)
            usersList.addAll(queriedUsers)
        }

        val myUid = _currentUid.value
        val myProfile = _currentUserProfile.value

        // Ensure current user is included if local user exists and not yet fetched
        if (myProfile != null && myProfile.displayName.isNotBlank()) {
            val existsInRemote = usersList.any { it.uid == myUid || (it.displayName.equals(myProfile.displayName, ignoreCase = true)) }
            if (!existsInRemote) {
                usersList.add(myProfile)
            }
        }

        if (usersList.isEmpty()) {
            if (myProfile != null && myProfile.displayName.isNotBlank()) {
                usersList.add(myProfile)
            }
        }

        // Sort by XP descending
        val sortedUsers = usersList.distinctBy { if (it.uid.isNotBlank()) it.uid else it.displayName }
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

        val cloudObjectId = prefs.getString("cloud_object_id", "") ?: ""

        val dataJson = JSONObject().apply {
            put("uid", user.uid)
            put("displayName", user.displayName)
            put("level", user.level)
            put("xp", user.xp)
            put("workoutCount", user.workoutCount)
            put("totalReps", user.totalReps)
            put("averageFormScore", user.averageFormScore)
            put("streak", user.streak)
            put("lastWorkoutDate", user.lastWorkoutDate)
            put("avatarIcon", user.avatarIcon)
            put("updatedAt", System.currentTimeMillis())
        }

        val payload = JSONObject().apply {
            put("name", "FormFitUser")
            put("data", dataJson)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()

        if (cloudObjectId.isBlank()) {
            // POST new object
            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .post(payload.toString().toRequestBody(mediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string() ?: ""
                        val respJson = JSONObject(respStr)
                        val newId = respJson.optString("id")
                        if (newId.isNotBlank()) {
                            prefs.edit().putString("cloud_object_id", newId).apply()
                            registerObjectIdInMasterRegistry(newId)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error posting user profile to cloud: ${e.message}")
            }
        } else {
            // PUT update existing object
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/$cloudObjectId")
                    .put(payload.toString().toRequestBody(mediaType))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        registerObjectIdInMasterRegistry(cloudObjectId)
                    } else if (response.code == 404) {
                        // Object expired or lost, clear ID to recreate
                        prefs.edit().remove("cloud_object_id").apply()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user profile in cloud: ${e.message}")
            }
        }
    }

    private fun getMasterRegistryMemberIds(): List<String> {
        val list = mutableListOf<String>()
        try {
            val request = Request.Builder()
                .url("$BASE_URL/$MASTER_REGISTRY_ID")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val json = JSONObject(respStr)
                    val dataObj = json.optJSONObject("data")
                    val array = dataObj?.optJSONArray("memberObjectIds")
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val id = array.optString(i)
                            if (id.isNotBlank()) list.add(id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading master registry: ${e.message}")
        }
        return list
    }

    private fun registerObjectIdInMasterRegistry(newObjectId: String) {
        try {
            val currentIds = getMasterRegistryMemberIds().toMutableList()
            if (!currentIds.contains(newObjectId)) {
                currentIds.add(newObjectId)

                val regData = JSONObject().apply {
                    put("memberObjectIds", JSONArray(currentIds))
                }
                val regPayload = JSONObject().apply {
                    put("name", "FormFitMasterRegistry")
                    put("data", regData)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val request = Request.Builder()
                    .url("$BASE_URL/$MASTER_REGISTRY_ID")
                    .put(regPayload.toString().toRequestBody(mediaType))
                    .build()

                okHttpClient.newCall(request).execute().close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering object in master registry: ${e.message}")
        }
    }

    private fun fetchUsersFromCloud(objectIds: List<String>): List<FirestoreUser> {
        val result = mutableListOf<FirestoreUser>()
        if (objectIds.isEmpty()) return result

        try {
            // Query objects by IDs
            val queryParams = objectIds.joinToString("&") { "id=$it" }
            val url = "$BASE_URL?$queryParams"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respStr = response.body?.string() ?: ""
                    val array = JSONArray(respStr)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val data = item.optJSONObject("data") ?: continue
                        val uid = data.optString("uid")
                        val displayName = data.optString("displayName")
                        if (displayName.isBlank()) continue

                        val level = data.optInt("level", 1)
                        val xp = data.optInt("xp", 0)
                        val workoutCount = data.optInt("workoutCount", 0)
                        val totalReps = data.optInt("totalReps", 0)
                        val avgForm = data.optInt("averageFormScore", 0)
                        val streak = data.optInt("streak", 1)
                        val lastDate = data.optString("lastWorkoutDate", "")
                        val avatar = data.optString("avatarIcon", "💪")

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
                                lastWorkoutDate = lastDate,
                                avatarIcon = avatar
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users from cloud: ${e.message}")
        }

        return result
    }
}

