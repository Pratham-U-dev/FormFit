package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FormFitDatabase
import com.example.data.FormFitRepository
import com.example.data.UserStats
import com.example.data.WorkoutSession
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LeaderboardEntry(
    val name: String,
    val xp: Int,
    val rank: Int,
    val characterIcon: String,
    val isUser: Boolean = false
)

data class Badge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val requirement: String
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val database = androidx.room.Room.databaseBuilder(
        application,
        FormFitDatabase::class.java,
        "formfit_database"
    ).fallbackToDestructiveMigration().build()

    private val repository = FormFitRepository(
        database.workoutDao(),
        database.userStatsDao(),
        database.nutritionDao()
    )

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = sdf.format(Date())

    private val _selectedNutritionDate = MutableStateFlow(todayDateString)
    val selectedNutritionDate = _selectedNutritionDate.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val nutritionLogsForSelectedDate: StateFlow<List<com.example.data.NutritionLog>> = _selectedNutritionDate
        .flatMapLatest { date -> repository.getNutritionLogsForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val totalCaloriesForSelectedDate: StateFlow<Int> = _selectedNutritionDate
        .flatMapLatest { date -> repository.getTotalCaloriesForDate(date) }
        .map { it ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val loggedNutritionDates: StateFlow<List<String>> = repository.loggedNutritionDates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentDailyCaloriesGoal = MutableStateFlow(2000)

    fun setSelectedNutritionDate(date: String) {
        _selectedNutritionDate.value = date
    }

    fun addNutritionLog(
        mealName: String,
        calories: Int,
        proteinGrams: Int = 0,
        carbsGrams: Int = 0,
        fatGrams: Int = 0,
        photoPath: String? = null
    ) {
        viewModelScope.launch {
            repository.insertNutritionLog(
                com.example.data.NutritionLog(
                    date = _selectedNutritionDate.value,
                    mealName = mealName,
                    calories = calories,
                    proteinGrams = proteinGrams,
                    carbsGrams = carbsGrams,
                    fatGrams = fatGrams,
                    photoPath = photoPath
                )
            )
            com.example.audio.DuoSoundPlayer.playCorrect()
        }
    }

    fun deleteNutritionLog(id: Int) {
        viewModelScope.launch {
            repository.deleteNutritionLog(id)
        }
    }

    private val prefs = application.getSharedPreferences("formfit_settings", android.content.Context.MODE_PRIVATE)

    private val _aiApiKey = MutableStateFlow(prefs.getString("ai_vision_api_key", "") ?: "")
    val aiApiKey = _aiApiKey.asStateFlow()

    fun saveAiApiKey(key: String) {
        val trimmed = key.trim()
        _aiApiKey.value = trimmed
        prefs.edit().putString("ai_vision_api_key", trimmed).apply()
    }

    suspend fun analyzeFoodImage(bitmap: android.graphics.Bitmap): com.example.api.FoodAnalysisResult {
        val userKey = _aiApiKey.value
        return com.example.api.GeminiFoodAnalyzer.analyzeMealImage(bitmap, userKey)
    }

    val allSessions: StateFlow<List<WorkoutSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userStats: StateFlow<UserStats> = repository.userStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStats()
        )

    // Current Active Workout State
    private val _isActiveSession = MutableStateFlow(false)
    val isActiveSession = _isActiveSession.asStateFlow()

    private val _currentExercise = MutableStateFlow("Squat")
    val currentExercise = _currentExercise.asStateFlow()

    private val _repCount = MutableStateFlow(0)
    val repCount = _repCount.asStateFlow()

    private val _sessionSeconds = MutableStateFlow(0)
    val sessionSeconds = _sessionSeconds.asStateFlow()

    private val _currentFeedback = MutableStateFlow("Position yourself in front of the camera")
    val currentFeedback = _currentFeedback.asStateFlow()

    private val _currentScore = MutableStateFlow(100)
    val currentScore = _currentScore.asStateFlow()

    private val _repScores = MutableStateFlow<List<Int>>(emptyList())
    val repScores = _repScores.asStateFlow()

    private val _mistakesList = MutableStateFlow<List<String>>(emptyList())
    val mistakesList = _mistakesList.asStateFlow()

    private val _isVirtualCoachMode = MutableStateFlow(true)
    val isVirtualCoachMode = _isVirtualCoachMode.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled = _isSoundEnabled.asStateFlow()

    // Last completed session summary state (to display on summary screen)
    private val _lastCompletedSession = MutableStateFlow<WorkoutSession?>(null)
    val lastCompletedSession = _lastCompletedSession.asStateFlow()

    // Available achievements/badges
    val badgesList = listOf(
        Badge("first_workout", "First Steps", "Completed your very first FormFit workout!", "🔥", "Complete 1 workout"),
        Badge("streak_7", "Unstoppable", "Maintained a 7-day workout streak!", "⚡", "Reach a 7-day streak"),
        Badge("perfect_squats", "Squat Deity", "Completed 100 perfect squats!", "👑", "100 squats with >90% form"),
        Badge("form_master", "Form Master", "Achieved an average form score of over 90%!", "🎓", "Avg form score >90%")
    )

    private val firebaseRepo = com.example.data.FirebaseLeaderboardRepository(application)

    val needsDisplayNamePrompt: StateFlow<Boolean> = firebaseRepo.needsDisplayNamePrompt
    val currentUserProfile: StateFlow<com.example.data.FirestoreUser?> = firebaseRepo.currentUserProfile

    // Live Online Leaderboard
    val leaderboard: StateFlow<List<LeaderboardEntry>> = firebaseRepo.leaderboardEntries

    // Supabase Configuration
    val supabaseUrl: StateFlow<String> = firebaseRepo.supabaseUrl
    val supabaseAnonKey: StateFlow<String> = firebaseRepo.supabaseAnonKey
    val isSupabaseConnected: StateFlow<Boolean> = firebaseRepo.isSupabaseConnected

    fun saveSupabaseConfig(url: String, anonKey: String) {
        firebaseRepo.saveSupabaseConfig(url, anonKey)
    }

    private var timerJob: Job? = null
    private var simulationJob: Job? = null

    init {
        // Observe all sessions to unlock progressive badges
        viewModelScope.launch {
            allSessions.collect { sessions ->
                evaluateComplexBadges(sessions)
            }
        }
    }

    fun saveDisplayName(name: String) {
        viewModelScope.launch {
            val stats = repository.getUserStatsDirect()
            val sessions = allSessions.value
            val totalReps = sessions.sumOf { it.totalReps }
            val avgScore = if (sessions.isNotEmpty()) sessions.map { it.averageScore }.average().toInt() else 0

            firebaseRepo.saveDisplayName(
                displayName = name,
                initialStats = stats,
                totalWorkoutSessions = sessions.size,
                totalRepsCount = totalReps,
                averageForm = avgScore
            )
        }
    }

    private fun evaluateComplexBadges(sessions: List<WorkoutSession>) {
        viewModelScope.launch {
            val stats = repository.getUserStatsDirect()
            val unlocked = stats.unlockedBadgesCsv.split(",").filter { it.isNotEmpty() }.toMutableSet()
            
            // 1. Perfect squats counter
            var totalPerfectSquats = 0
            for (s in sessions) {
                if (s.exerciseType == "Squat" && s.averageScore >= 90) {
                    totalPerfectSquats += s.totalReps
                }
            }
            if (totalPerfectSquats >= 10) { // Keep MVP easy to achieve: 10 perfect squats for demo
                if (unlocked.add("perfect_squats")) {
                    repository.unlockBadge("perfect_squats")
                }
            }

            // 2. Form Master check
            val hasFormMaster = sessions.any { it.averageScore >= 90 && it.totalReps >= 5 }
            if (hasFormMaster) {
                if (unlocked.add("form_master")) {
                    repository.unlockBadge("form_master")
                }
            }
        }
    }

    fun updateLeaderboard(userXpContribution: Int) {
        viewModelScope.launch {
            val stats = repository.getUserStatsDirect()
            val sessions = allSessions.value
            val totalReps = sessions.sumOf { it.totalReps }
            val avgScore = if (sessions.isNotEmpty()) sessions.map { it.averageScore }.average().toInt() else 0
            val badges = stats.unlockedBadgesCsv.split(",").filter { it.isNotBlank() }

            firebaseRepo.syncUserWorkoutCompleted(
                xpEarnedInWorkout = userXpContribution,
                totalWorkoutCount = sessions.size,
                totalRepsCount = totalReps,
                overallAvgFormScore = avgScore,
                streakCount = stats.streakDays,
                todayDateStr = todayDateString,
                unlockedBadgesList = badges
            )
        }
    }

    fun setVirtualCoachMode(enabled: Boolean) {
        _isVirtualCoachMode.value = enabled
        if (_isActiveSession.value) {
            // Restart practice jobs if exercise is running
            startWorkout(_currentExercise.value)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        com.example.audio.DuoSoundPlayer.isSoundEnabled = enabled
        if (enabled) {
            com.example.audio.DuoSoundPlayer.playClick()
        }
    }

    fun startWorkout(exerciseType: String) {
        _currentExercise.value = exerciseType
        _isActiveSession.value = true
        _repCount.value = 0
        _sessionSeconds.value = 0
        _repScores.value = emptyList()
        _mistakesList.value = emptyList()
        _currentScore.value = 100
        _currentFeedback.value = when (exerciseType) {
            "Pull-up" -> "Grip the bar with overhand grip. Hang fully extended."
            "Squat" -> "Get ready to squat! Stand straight."
            "Push-up" -> "Get into plank position. Prepare to lower."
            "Lunge" -> "Prepare to lunge. Keep hips square."
            "Plank" -> "Hold a solid plank. Keep body straight!"
            else -> "Ready!"
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isActiveSession.value) {
                delay(1000)
                _sessionSeconds.value += 1
                if (_currentExercise.value == "Plank" && _isActiveSession.value) {
                    // Plank counts "reps" as seconds of perfect plank holding
                    if (_currentScore.value >= 85) {
                        _repCount.value += 1
                    }
                }
            }
        }

        simulationJob?.cancel()
        if (_isVirtualCoachMode.value) {
            startVirtualCoachSimulation(exerciseType)
        }
    }

    private fun startVirtualCoachSimulation(exerciseType: String) {
        simulationJob = viewModelScope.launch {
            var cycleProgress = 0f // 0f to 1f representation of rep completion
            var goingDown = true
            
            while (_isActiveSession.value) {
                delay(80) // ~12 FPS simulation update loop
                
                if (goingDown) {
                    cycleProgress += 0.05f
                    if (cycleProgress >= 1f) {
                        cycleProgress = 1f
                        goingDown = false
                    }
                } else {
                    cycleProgress -= 0.05f
                    if (cycleProgress <= 0f) {
                        cycleProgress = 0f
                        goingDown = true
                        
                        // Completed a full repetition!
                        // Calculate score of the completed rep
                        val formScore = if (Math.random() > 0.15) {
                            (85..100).random()
                        } else {
                            (50..80).random() // Trigger random mistake rep sometimes
                        }
                        
                        _currentScore.value = formScore
                        _repScores.value = _repScores.value + formScore

                        if (exerciseType != "Plank") {
                            _repCount.value += 1
                        }

                        // Form Feedback & Mistake Categorization
                        if (formScore >= 85) {
                            com.example.audio.DuoSoundPlayer.playCorrect()
                            _currentFeedback.value = when (exerciseType) {
                                "Pull-up" -> "Chin cleared bar! Full lockout."
                                "Squat" -> "Excellent squat depth! Perfect posture."
                                "Push-up" -> "Perfect push-up! Keep it up."
                                "Lunge" -> "Great alignment! Excellent balance."
                                else -> "Keep holding! Body is straight."
                            }
                        } else {
                            com.example.audio.DuoSoundPlayer.playMistake()
                            val mistake = when (exerciseType) {
                                "Pull-up" -> if (Math.random() > 0.5) {
                                    "Chin not over bar"
                                } else {
                                    "Excessive body swing / kip"
                                }
                                "Squat" -> if (Math.random() > 0.5) {
                                    "Squat not deep enough"
                                } else {
                                    "Knees collapsing inward"
                                }
                                "Push-up" -> if (Math.random() > 0.5) {
                                    "Push-up not deep enough"
                                } else {
                                    "Hip sagging"
                                }
                                "Lunge" -> "Incorrect lunge knee alignment"
                                "Plank" -> "Hip sagging during plank"
                                else -> "Incorrect posture"
                            }
                            
                            _mistakesList.value = _mistakesList.value + mistake
                            _currentFeedback.value = when (mistake) {
                                "Chin not over bar" -> "Pull higher! Get chin completely over the bar."
                                "Excessive body swing / kip" -> "Keep body quiet! Avoid leg kick or swing."
                                "Squat not deep enough" -> "Go lower! Get thighs parallel to the ground."
                                "Knees collapsing inward" -> "Keep your knees aligned with your toes."
                                "Push-up not deep enough" -> "Chest closer to the ground!"
                                "Hip sagging" -> "Engage your core to keep hips level."
                                "Incorrect lunge knee alignment" -> "Don't let front knee pass your toes."
                                "Hip sagging during plank" -> "Lift your hips up. Keep core engaged."
                                else -> "Correct your form!"
                            }
                        }
                    }
                }
                
                // Real-time angle computation simulations
                if (exerciseType == "Plank") {
                    // Random minor fluctuations
                    val randomScore = if (Math.random() > 0.10) (85..100).random() else (60..80).random()
                    _currentScore.value = randomScore
                    if (randomScore < 85) {
                        _currentFeedback.value = "Engage your core! Hips are sagging."
                        if (Math.random() > 0.5) {
                            val previousSize = _mistakesList.value.size
                            _mistakesList.value = _mistakesList.value + "Hip sagging during plank"
                            if (_mistakesList.value.size > previousSize) {
                                com.example.audio.DuoSoundPlayer.playMistake()
                            }
                        }
                    } else {
                        _currentFeedback.value = "Great plank form! Hold it steady."
                    }
                }
            }
        }
    }

    // This method is called from real Camera Analyzer frames when Camera Mode is active
    fun processCameraFrameAnalysis(
        score: Int,
        mistake: String?,
        feedback: String,
        isRepCompleted: Boolean
    ) {
        _currentScore.value = score
        _currentFeedback.value = feedback
        
        if (mistake != null && Math.random() > 0.7) { // limit spam
            val previousSize = _mistakesList.value.size
            _mistakesList.value = _mistakesList.value + mistake
            if (_mistakesList.value.size > previousSize) {
                com.example.audio.DuoSoundPlayer.playMistake()
            }
        }

        if (isRepCompleted) {
            _repCount.value += 1
            _repScores.value = _repScores.value + score
            if (score >= 85) {
                com.example.audio.DuoSoundPlayer.playCorrect()
            } else {
                com.example.audio.DuoSoundPlayer.playMistake()
            }
        }
    }

    fun stopAndSaveWorkout() {
        _isActiveSession.value = false
        timerJob?.cancel()
        simulationJob?.cancel()
        
        com.example.audio.DuoSoundPlayer.playFanfare()

        val exercise = _currentExercise.value
        val reps = _repCount.value
        val duration = _sessionSeconds.value
        val mistakes = _mistakesList.value.size
        
        // Form metrics math
        val avgScore = if (_repScores.value.isNotEmpty()) {
            _repScores.value.average().toInt()
        } else if (exercise == "Plank") {
            // For plank, calculate based on overall score state
            if (mistakes > 0) (80..92).random() else (92..100).random()
        } else {
            0
        }
        
        val bestScore = if (_repScores.value.isNotEmpty()) {
            _repScores.value.maxOrNull() ?: 0
        } else if (exercise == "Plank") {
            if (mistakes == 0) 100 else 90
        } else {
            0
        }

        // Gamification XP Formula:
        // Base XP: 20 XP for session completion
        // Rep XP: 5 XP per correct rep (>80 form score), 2 XP per lower-quality rep
        // Plank XP: 1 XP per second held with good form
        var xpEarned = 20
        if (exercise == "Plank") {
            xpEarned += reps // seconds held with good form
        } else {
            _repScores.value.forEach { score ->
                xpEarned += if (score >= 80) 5 else 2
            }
        }

        // Reward extra XP for High Form Quality (Form Master bonus)
        if (avgScore >= 90 && reps >= 5) {
            xpEarned += 30 // 30 XP bonus
        }

        val session = WorkoutSession(
            exerciseType = exercise,
            totalReps = reps,
            durationSeconds = duration,
            averageScore = avgScore,
            bestScore = bestScore,
            mistakeCount = mistakes,
            xpEarned = xpEarned
        )

        _lastCompletedSession.value = session

        viewModelScope.launch {
            repository.insertSession(session)
            updateLeaderboard(xpEarned)
        }
    }

    fun abandonWorkout() {
        _isActiveSession.value = false
        timerJob?.cancel()
        simulationJob?.cancel()
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            // Reset local leaderboard as well so it doesn't show outdated user stats
            updateLeaderboard(0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        simulationJob?.cancel()
        database.close()
    }
}
