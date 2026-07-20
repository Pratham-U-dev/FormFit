package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FormFitRepository(
    private val workoutDao: WorkoutDao,
    private val userStatsDao: UserStatsDao
) {
    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()

    val userStats: Flow<UserStats> = userStatsDao.getUserStats().map { stats ->
        stats ?: UserStats() // Default stats if null
    }

    suspend fun insertSession(session: WorkoutSession) {
        workoutDao.insertSession(session)
        // Automatically reward XP and process stats
        addXp(session.xpEarned)
    }

    suspend fun getUserStatsDirect(): UserStats {
        return userStatsDao.getUserStatsDirect() ?: UserStats()
    }

    suspend fun addXp(xpGained: Int) {
        val current = getUserStatsDirect()
        var newXp = current.currentXp + xpGained
        var newLevel = current.level
        
        // Dynamic level-up math: each level requires level * 100 XP
        // Level 1: 100 XP
        // Level 2: 200 XP, etc.
        while (newXp >= newLevel * 100) {
            newXp -= newLevel * 100
            newLevel++
        }

        // Streak check: if last workout was yesterday, streak increments.
        // If today, streak stays the same. If more than 1 day ago, reset or set to 1.
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        var newStreak = current.streakDays

        if (current.lastWorkoutTimestamp == 0L) {
            newStreak = 1
        } else {
            val diff = now - current.lastWorkoutTimestamp
            if (diff in (oneDayMillis until (oneDayMillis * 2))) {
                newStreak++
            } else if (diff >= (oneDayMillis * 2)) {
                newStreak = 1 // reset
            } else if (current.streakDays == 0) {
                newStreak = 1
            }
        }

        // Evaluate achievements
        val badges = current.unlockedBadgesCsv.split(",").filter { it.isNotEmpty() }.toMutableSet()
        
        // 1. First Workout
        badges.add("first_workout")

        // 2. 7-Day Streak
        if (newStreak >= 7) {
            badges.add("streak_7")
        }

        // 3. Form Master (we will check in ViewModel when completing with high form score, 
        // but let's pre-unlock badges if we satisfy some criteria)
        
        val updatedStats = current.copy(
            level = newLevel,
            currentXp = newXp,
            streakDays = newStreak,
            lastWorkoutTimestamp = now,
            unlockedBadgesCsv = badges.joinToString(",")
        )

        userStatsDao.insertOrUpdateStats(updatedStats)
    }

    suspend fun updateStreak(streak: Int) {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(streakDays = streak))
    }

    suspend fun unlockBadge(badgeId: String) {
        val current = getUserStatsDirect()
        val badges = current.unlockedBadgesCsv.split(",").filter { it.isNotEmpty() }.toMutableSet()
        if (badges.add(badgeId)) {
            userStatsDao.insertOrUpdateStats(
                current.copy(unlockedBadgesCsv = badges.joinToString(","))
            )
        }
    }

    suspend fun resetAllData() {
        workoutDao.deleteAllSessions()
        userStatsDao.deleteUserStats()
    }
}
