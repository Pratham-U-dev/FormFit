package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseType: String,
    val totalReps: Int,
    val durationSeconds: Int,
    val averageScore: Int,
    val bestScore: Int,
    val mistakeCount: Int,
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val currentXp: Int = 0,
    val streakDays: Int = 0,
    val lastWorkoutTimestamp: Long = 0L,
    val unlockedBadgesCsv: String = "" // e.g. "first_workout,streak_7,perfect_squats"
)

@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val mealName: String,
    val calories: Int,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val photoPath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession)

    @Query("SELECT SUM(xpEarned) FROM workout_sessions")
    fun getTotalXpFlow(): Flow<Int?>

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsDirect(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: UserStats)

    @Query("DELETE FROM user_stats")
    suspend fun deleteUserStats()
}

@Dao
interface NutritionDao {
    @Query("SELECT * FROM nutrition_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getLogsForDate(date: String): Flow<List<NutritionLog>>

    @Query("SELECT SUM(calories) FROM nutrition_logs WHERE date = :date")
    fun getTotalCaloriesForDate(date: String): Flow<Int?>

    @Query("SELECT DISTINCT date FROM nutrition_logs ORDER BY date DESC")
    fun getLoggedDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NutritionLog)

    @Query("DELETE FROM nutrition_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)

    @Query("DELETE FROM nutrition_logs")
    suspend fun deleteAllNutritionLogs()
}

@Database(entities = [WorkoutSession::class, UserStats::class, NutritionLog::class], version = 2, exportSchema = false)
abstract class FormFitDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun nutritionDao(): NutritionDao
}
