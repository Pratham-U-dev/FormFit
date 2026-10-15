package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FormFitRepository(
    private val workoutDao: WorkoutDao,
    private val userStatsDao: UserStatsDao,
    private val userProfileDao: UserProfileDao,
    private val nutritionDao: NutritionDao
) {
    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()

    val userBodyProfile: Flow<UserBodyProfile> = userProfileDao.getUserProfile().map { profile ->
        profile ?: UserBodyProfile()
    }

    suspend fun getUserBodyProfileDirect(): UserBodyProfile {
        return userProfileDao.getUserProfileDirect() ?: UserBodyProfile()
    }

    suspend fun updateUserBodyProfile(profile: UserBodyProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    fun getNutritionLogsForDate(date: String): Flow<List<NutritionLog>> =
        nutritionDao.getLogsForDate(date)

    fun getTotalCaloriesForDate(date: String): Flow<Int?> =
        nutritionDao.getTotalCaloriesForDate(date)

    val loggedNutritionDates: Flow<List<String>> =
        nutritionDao.getLoggedDates()

    suspend fun insertNutritionLog(log: NutritionLog) {
        nutritionDao.insertLog(log)
    }

    suspend fun deleteNutritionLog(id: Int) {
        nutritionDao.deleteLog(id)
    }

    val userStats: Flow<UserStats> = userStatsDao.getUserStats().map { stats ->
        stats ?: UserStats() // Default stats if null
    }

    suspend fun insertSession(session: WorkoutSession) {
        workoutDao.insertSession(session)
        // Automatically reward XP and process stats
        addXp(session.xpEarned, session.averageScore)
    }

    suspend fun getUserStatsDirect(): UserStats {
        val current = userStatsDao.getUserStatsDirect() ?: UserStats()
        return checkPassiveHeartRegeneration(current)
    }

    private suspend fun checkPassiveHeartRegeneration(current: UserStats): UserStats {
        if (current.isSuperSubscriber || current.hearts >= current.maxHearts) {
            return current
        }
        val now = System.currentTimeMillis()
        val thirtyMinutesMillis = 30 * 60 * 1000L
        if (current.lastHeartRefillTimestamp == 0L) {
            val updated = current.copy(lastHeartRefillTimestamp = now)
            userStatsDao.insertOrUpdateStats(updated)
            return updated
        }

        val elapsed = now - current.lastHeartRefillTimestamp
        val heartsToRestore = (elapsed / thirtyMinutesMillis).toInt()
        if (heartsToRestore > 0) {
            val newHearts = (current.hearts + heartsToRestore).coerceAtMost(current.maxHearts)
            val remainderMillis = elapsed % thirtyMinutesMillis
            val newTimestamp = now - remainderMillis
            val updated = current.copy(
                hearts = newHearts,
                lastHeartRefillTimestamp = if (newHearts >= current.maxHearts) 0L else newTimestamp
            )
            userStatsDao.insertOrUpdateStats(updated)
            return updated
        }
        return current
    }

    suspend fun addXp(xpGained: Int, averageScore: Int = 85) {
        val current = getUserStatsDirect()
        var newXp = current.currentXp + xpGained
        var newLevel = current.level
        var levelsGained = 0
        
        // Dynamic level-up math: each level requires level * 100 XP
        while (newXp >= newLevel * 100) {
            newXp -= newLevel * 100
            newLevel++
            levelsGained++
        }

        // Streak check: compare calendar days accurately
        val now = System.currentTimeMillis()
        var newStreak = current.streakDays
        var streakFreezes = current.streakFreezesEquipped

        if (current.lastWorkoutTimestamp == 0L) {
            newStreak = 1
        } else {
            val calLast = java.util.Calendar.getInstance().apply { timeInMillis = current.lastWorkoutTimestamp }
            val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }

            val lastYear = calLast.get(java.util.Calendar.YEAR)
            val lastDayOfYear = calLast.get(java.util.Calendar.DAY_OF_YEAR)
            val nowYear = calNow.get(java.util.Calendar.YEAR)
            val nowDayOfYear = calNow.get(java.util.Calendar.DAY_OF_YEAR)

            if (nowYear == lastYear && nowDayOfYear == lastDayOfYear) {
                // Same day workout: maintain streak
            } else if ((nowYear == lastYear && nowDayOfYear == lastDayOfYear + 1) ||
                       (nowYear == lastYear + 1 && lastDayOfYear == calLast.getActualMaximum(java.util.Calendar.DAY_OF_YEAR) && nowDayOfYear == 1)) {
                // Consecutive day! Increment streak!
                newStreak++
            } else {
                // Missed day(s)! Check if Streak Freeze is equipped
                if (streakFreezes > 0) {
                    streakFreezes-- // Consume freeze to save streak
                    // Streak preserved!
                } else {
                    newStreak = 1 // Reset streak
                }
            }
        }

        // Diamond (Gems) rewards from workout
        var gemsEarned = 25 // Base reward for completing workout
        gemsEarned += (newStreak * 5).coerceAtMost(50) // Streak bonus
        if (levelsGained > 0) {
            gemsEarned += levelsGained * 50 // Level-up bonus
        }
        if (averageScore >= 90) {
            gemsEarned += 20 // Clean form master bonus
        }

        // Evaluate achievements
        val badges = current.unlockedBadgesCsv.split(",").filter { it.isNotEmpty() }.toMutableSet()
        badges.add("first_workout")
        if (newStreak >= 7) badges.add("streak_7")
        if (newStreak >= 30) badges.add("streak_30")
        if (averageScore >= 90) badges.add("form_master")

        val updatedStats = current.copy(
            level = newLevel,
            currentXp = newXp,
            streakDays = newStreak,
            streakFreezesEquipped = streakFreezes,
            gems = current.gems + gemsEarned,
            lastWorkoutTimestamp = now,
            unlockedBadgesCsv = badges.joinToString(",")
        )

        userStatsDao.insertOrUpdateStats(updatedStats)
    }

    suspend fun updateStreak(streak: Int) {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(streakDays = streak))
    }

    suspend fun addGems(amount: Int) {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(gems = (current.gems + amount).coerceAtLeast(0)))
    }

    suspend fun spendGems(amount: Int): Boolean {
        val current = getUserStatsDirect()
        if (current.gems >= amount) {
            userStatsDao.insertOrUpdateStats(current.copy(gems = current.gems - amount))
            return true
        }
        return false
    }

    suspend fun deductHeartOnMistake(): Pair<Boolean, Int> {
        val current = getUserStatsDirect()
        if (current.isSuperSubscriber) {
            return Pair(false, current.hearts) // Unlimited hearts!
        }
        if (current.formShieldActive) {
            // Shield absorbed the mistake
            userStatsDao.insertOrUpdateStats(current.copy(formShieldActive = false))
            return Pair(false, current.hearts)
        }
        if (current.hearts > 0) {
            val newHearts = (current.hearts - 1).coerceAtLeast(0)
            val now = System.currentTimeMillis()
            val newTimestamp = if (current.hearts == current.maxHearts) now else current.lastHeartRefillTimestamp
            userStatsDao.insertOrUpdateStats(current.copy(hearts = newHearts, lastHeartRefillTimestamp = newTimestamp))
            return Pair(true, newHearts)
        }
        return Pair(true, 0)
    }

    suspend fun practiceToEarnHeart(): Boolean {
        val current = getUserStatsDirect()
        if (current.hearts < current.maxHearts) {
            val newHearts = current.hearts + 1
            userStatsDao.insertOrUpdateStats(current.copy(hearts = newHearts))
            return true
        }
        return false
    }

    suspend fun updateHearts(newHearts: Int) {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(hearts = newHearts.coerceIn(0, current.maxHearts)))
    }

    suspend fun refillHearts() {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(hearts = current.maxHearts, lastHeartRefillTimestamp = 0L))
    }

    suspend fun setStreakFreezes(count: Int) {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(streakFreezesEquipped = count.coerceIn(0, 2)))
    }

    suspend fun setSuperSubscriber(isSuper: Boolean) {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(
            current.copy(
                isSuperSubscriber = isSuper,
                hearts = if (isSuper) 5 else current.hearts
            )
        )
    }

    suspend fun activateDoubleXp(durationMinutes: Int = 30) {
        val current = getUserStatsDirect()
        val until = System.currentTimeMillis() + durationMinutes * 60 * 1000L
        userStatsDao.insertOrUpdateStats(current.copy(doubleXpActiveUntil = until))
    }

    suspend fun activateFormShield() {
        val current = getUserStatsDirect()
        userStatsDao.insertOrUpdateStats(current.copy(formShieldActive = true))
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
        userProfileDao.deleteUserProfile()
        nutritionDao.deleteAllNutritionLogs()
    }
}
