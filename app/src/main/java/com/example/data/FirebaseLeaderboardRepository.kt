package com.example.data

import android.content.Context
import android.util.Log
import com.example.viewmodel.LeaderboardEntry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseLeaderboardRepository(private val context: Context) {

    private val TAG = "FirebaseLeaderboard"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUid = MutableStateFlow<String?>(null)
    val currentUid: StateFlow<String?> = _currentUid.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<FirestoreUser?>(null)
    val currentUserProfile: StateFlow<FirestoreUser?> = _currentUserProfile.asStateFlow()

    private val _needsDisplayNamePrompt = MutableStateFlow(false)
    val needsDisplayNamePrompt: StateFlow<Boolean> = _needsDisplayNamePrompt.asStateFlow()

    private val _leaderboardEntries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardEntries: StateFlow<List<LeaderboardEntry>> = _leaderboardEntries.asStateFlow()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    init {
        initFirebaseAndAuth()
    }

    private fun initFirebaseAndAuth() {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey("AIzaSyFormFitLeaderboardKey")
                        .setApplicationId("1:123456789012:android:formfitwqzpxk")
                        .setProjectId("formfit-app-online")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }

                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()

                try {
                    firestore.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    }
                } catch (e: Exception) {
                    // Ignore if settings already set
                }

                ensureAnonymousUser()
                listenToLeaderboard()
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            }
        }
    }

    private suspend fun ensureAnonymousUser() {
        try {
            var user = auth.currentUser
            if (user == null) {
                val authResult = auth.signInAnonymously().await()
                user = authResult.user
            }

            val uid = user?.uid ?: return
            _currentUid.value = uid

            // Fetch user profile from Firestore
            val userDocRef = firestore.collection("users").document(uid)
            val snapshot = userDocRef.get().await()

            if (snapshot.exists()) {
                val profile = snapshot.toObject(FirestoreUser::class.java)
                if (profile != null) {
                    _currentUserProfile.value = profile
                    if (profile.displayName.isBlank()) {
                        _needsDisplayNamePrompt.value = true
                    }
                } else {
                    _needsDisplayNamePrompt.value = true
                }
            } else {
                // First time user doc does not exist
                _needsDisplayNamePrompt.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during anonymous auth: ${e.message}", e)
        }
    }

    fun saveDisplayName(
        displayName: String,
        initialStats: UserStats? = null,
        totalWorkoutSessions: Int = 0,
        totalRepsCount: Int = 0,
        averageForm: Int = 0
    ) {
        val uid = _currentUid.value ?: auth.currentUser?.uid ?: return
        val cleanName = displayName.trim()
        if (cleanName.isBlank()) return

        scope.launch {
            try {
                val existing = _currentUserProfile.value
                val updatedProfile = FirestoreUser(
                    uid = uid,
                    displayName = cleanName,
                    level = existing?.level ?: (initialStats?.level ?: 1),
                    xp = existing?.xp ?: (initialStats?.currentXp ?: 0),
                    workoutCount = existing?.workoutCount ?: totalWorkoutSessions,
                    totalReps = existing?.totalReps ?: totalRepsCount,
                    averageFormScore = existing?.averageFormScore ?: averageForm,
                    streak = existing?.streak ?: (initialStats?.streakDays ?: 1),
                    lastWorkoutDate = existing?.lastWorkoutDate ?: "",
                    achievements = existing?.achievements ?: (initialStats?.unlockedBadgesCsv?.split(",")?.filter { it.isNotBlank() } ?: emptyList()),
                    avatarIcon = existing?.avatarIcon ?: "💪"
                )

                firestore.collection("users").document(uid).set(updatedProfile).await()
                _currentUserProfile.value = updatedProfile
                _needsDisplayNamePrompt.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error saving display name: ${e.message}", e)
            }
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
        val uid = _currentUid.value ?: auth.currentUser?.uid ?: return
        scope.launch {
            try {
                val currentDocRef = firestore.collection("users").document(uid)
                val snapshot = currentDocRef.get().await()

                val currentProfile = if (snapshot.exists()) {
                    snapshot.toObject(FirestoreUser::class.java)
                } else {
                    _currentUserProfile.value
                }

                val currentName = currentProfile?.displayName?.ifBlank { "Trainer" } ?: "Trainer"
                val newXp = (currentProfile?.xp ?: 0) + xpEarnedInWorkout
                val newLevel = (newXp / 350) + 1

                val updatedProfile = FirestoreUser(
                    uid = uid,
                    displayName = currentName,
                    level = newLevel,
                    xp = newXp,
                    workoutCount = totalWorkoutCount,
                    totalReps = totalRepsCount,
                    averageFormScore = overallAvgFormScore,
                    streak = streakCount,
                    lastWorkoutDate = todayDateStr,
                    achievements = unlockedBadgesList,
                    avatarIcon = currentProfile?.avatarIcon ?: "💪"
                )

                currentDocRef.set(updatedProfile).await()
                _currentUserProfile.value = updatedProfile
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing workout completion to Firestore: ${e.message}", e)
            }
        }
    }

    private fun listenToLeaderboard() {
        firestore.collection("users")
            .orderBy("xp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Leaderboard listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val uid = _currentUid.value
                    val usersList = snapshot.toObjects(FirestoreUser::class.java)

                    if (usersList.isEmpty()) {
                        // Seed default benchmark users if collection is completely empty
                        seedDefaultBenchmarkUsers()
                    } else {
                        val entries = usersList.mapIndexed { index, user ->
                            val isMe = user.uid == uid || (uid != null && user.uid == uid)
                            val icon = when {
                                user.avatarIcon.isNotBlank() -> user.avatarIcon
                                isMe -> "💪"
                                index == 0 -> "🦉"
                                index == 1 -> "👧"
                                index == 2 -> "👱‍♀️"
                                else -> "🏋️"
                            }
                            LeaderboardEntry(
                                name = if (isMe && user.displayName.isBlank()) "You" else user.displayName,
                                xp = user.xp,
                                rank = index + 1,
                                characterIcon = icon,
                                isUser = isMe
                            )
                        }
                        _leaderboardEntries.value = entries
                    }
                }
            }
    }

    private fun seedDefaultBenchmarkUsers() {
        scope.launch {
            try {
                val benchmarks = listOf(
                    FirestoreUser("bench_1", "Duo the Owl", 4, 1250, 25, 450, 95, 12, "", listOf("first_workout"), "🦉"),
                    FirestoreUser("bench_2", "Lily (Goth)", 3, 980, 18, 320, 88, 7, "", listOf("first_workout"), "👧"),
                    FirestoreUser("bench_3", "Zari (Enthusiast)", 3, 750, 15, 280, 90, 5, "", listOf("first_workout"), "👱‍♀️"),
                    FirestoreUser("bench_4", "Vikram (Reader)", 2, 450, 9, 150, 84, 3, "", listOf("first_workout"), "🧔"),
                    FirestoreUser("bench_5", "Oscar (Artist)", 1, 220, 5, 80, 80, 2, "", listOf("first_workout"), "👨‍🎨")
                )

                for (bench in benchmarks) {
                    firestore.collection("users").document(bench.uid).set(bench).await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error seeding benchmark users: ${e.message}", e)
            }
        }
    }
}
