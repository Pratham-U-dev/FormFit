package com.example.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirestoreUser(
    val uid: String = "",
    val displayName: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val workoutCount: Int = 0,
    val totalReps: Int = 0,
    val averageFormScore: Int = 0,
    val streak: Int = 1,
    val lastWorkoutDate: String = "",
    val achievements: List<String> = emptyList(),
    val avatarIcon: String = "💪"
)
