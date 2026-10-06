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
import com.example.cv.PoseSkeleton
import com.example.cv.PullUpBiomechanics
import com.example.cv.PullUpMetrics
import com.example.cv.PushUpBiomechanics
import com.example.cv.PushUpMetrics

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
        database.userProfileDao(),
        database.nutritionDao()
    )

    val userBodyProfile: StateFlow<com.example.data.UserBodyProfile> = repository.userBodyProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.data.UserBodyProfile()
        )

    fun updateUserBodyProfile(profile: com.example.data.UserBodyProfile) {
        viewModelScope.launch {
            repository.updateUserBodyProfile(profile)
            pullUpBiomechanics.updateBodyParams(profile.weightKg, profile.heightCm, profile.armLengthCm)
            pushUpBiomechanics.updateBodyParams(profile.weightKg, profile.heightCm)
            com.example.audio.DuoSoundPlayer.playCorrect()
        }
    }

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

    private val _currentExercise = MutableStateFlow("Pull-up")
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

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled = _isTtsEnabled.asStateFlow()

    // Last completed session summary state (to display on summary screen)
    private val pullUpBiomechanics = PullUpBiomechanics()
    private val _pullUpMetrics = MutableStateFlow(PullUpMetrics())
    val pullUpMetrics = _pullUpMetrics.asStateFlow()

    private val pushUpBiomechanics = PushUpBiomechanics()
    private val _pushUpMetrics = MutableStateFlow(PushUpMetrics())
    val pushUpMetrics = _pushUpMetrics.asStateFlow()

    private val _liveSpeedLossPct = MutableStateFlow(0)
    val liveSpeedLossPct = _liveSpeedLossPct.asStateFlow()

    private val _livePeakPowerW = MutableStateFlow(0)
    val livePeakPowerW = _livePeakPowerW.asStateFlow()

    private val _liveVelocityMps = MutableStateFlow(0.0)
    val liveVelocityMps = _liveVelocityMps.asStateFlow()

    private val _exerciseState = MutableStateFlow("READY")
    val exerciseState = _exerciseState.asStateFlow()

    private val _lastSessionRepScores = MutableStateFlow<List<Int>>(emptyList())
    val lastSessionRepScores = _lastSessionRepScores.asStateFlow()

    private val _lastSessionMistakes = MutableStateFlow<List<String>>(emptyList())
    val lastSessionMistakes = _lastSessionMistakes.asStateFlow()

    private val _isPersonalBest = MutableStateFlow(false)
    val isPersonalBest = _isPersonalBest.asStateFlow()

    private val _lastSessionCalories = MutableStateFlow(0.0)
    val lastSessionCalories = _lastSessionCalories.asStateFlow()

    private val _lastSessionPeakPower = MutableStateFlow(0)
    val lastSessionPeakPower = _lastSessionPeakPower.asStateFlow()

    private val _lastCompletedSession = MutableStateFlow<WorkoutSession?>(null)
    val lastCompletedSession = _lastCompletedSession.asStateFlow()

    private val _targetReps = MutableStateFlow(10)
    val targetReps = _targetReps.asStateFlow()

    fun setTargetReps(count: Int) {
        _targetReps.value = count.coerceIn(1, 100)
    }

    fun incrementTargetReps() {
        _targetReps.value = (_targetReps.value + 1).coerceAtMost(100)
        com.example.audio.DuoSoundPlayer.playClick()
    }

    fun decrementTargetReps() {
        _targetReps.value = (_targetReps.value - 1).coerceAtLeast(1)
        com.example.audio.DuoSoundPlayer.playClick()
    }

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

    init {
        // Observe all sessions to unlock progressive badges
        viewModelScope.launch {
            allSessions.collect { sessions ->
                evaluateComplexBadges(sessions)
            }
        }
        // Observe body profile to update biomechanics physics models
        viewModelScope.launch {
            userBodyProfile.collect { profile ->
                pullUpBiomechanics.updateBodyParams(profile.weightKg, profile.heightCm, profile.armLengthCm)
                pushUpBiomechanics.updateBodyParams(profile.weightKg, profile.heightCm)
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

    fun buyGemsPack(amount: Int, priceLabel: String) {
        viewModelScope.launch {
            repository.addGems(amount)
            com.example.audio.DuoSoundPlayer.playFanfare()
            com.example.audio.TtsCoach.speak("Awesome! $amount gems added to your account.", isUrgent = true)
        }
    }

    fun buyStreakFreeze(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val stats = repository.getUserStatsDirect()
            if (stats.streakFreezesEquipped >= 2) {
                onResult(false)
                return@launch
            }
            val success = repository.spendGems(200)
            if (success) {
                repository.setStreakFreezes(stats.streakFreezesEquipped + 1)
                com.example.audio.DuoSoundPlayer.playCorrect()
                com.example.audio.TtsCoach.speak("Streak Freeze equipped!", isUrgent = true)
                onResult(true)
            } else {
                com.example.audio.DuoSoundPlayer.playMistake()
                com.example.audio.TtsCoach.speak("Not enough gems. Purchase more in the shop.", isUrgent = true)
                onResult(false)
            }
        }
    }

    fun refillHeartsWithGems(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.spendGems(350)
            if (success) {
                repository.refillHearts()
                com.example.audio.DuoSoundPlayer.playCorrect()
                com.example.audio.TtsCoach.speak("Hearts fully restored to five!", isUrgent = true)
                onResult(true)
            } else {
                com.example.audio.DuoSoundPlayer.playMistake()
                com.example.audio.TtsCoach.speak("Not enough gems to refill hearts.", isUrgent = true)
                onResult(false)
            }
        }
    }

    fun practiceToEarnHeart() {
        viewModelScope.launch {
            val stats = repository.getUserStatsDirect()
            if (stats.hearts < stats.maxHearts) {
                repository.updateHearts(stats.hearts + 1)
                com.example.audio.DuoSoundPlayer.playCorrect()
                com.example.audio.TtsCoach.speak("Practice complete! +1 heart earned.", isUrgent = true)
            }
        }
    }

    fun buyPowerUp(powerUpName: String, gemCost: Int, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.spendGems(gemCost)
            if (success) {
                if (powerUpName.contains("Double XP", ignoreCase = true)) {
                    repository.activateDoubleXp(30)
                }
                com.example.audio.DuoSoundPlayer.playCorrect()
                com.example.audio.TtsCoach.speak("$powerUpName activated!", isUrgent = true)
                onResult(true)
            } else {
                com.example.audio.DuoSoundPlayer.playMistake()
                com.example.audio.TtsCoach.speak("Need more gems for this power up.", isUrgent = true)
                onResult(false)
            }
        }
    }

    fun toggleSuperSubscription(activate: Boolean) {
        viewModelScope.launch {
            repository.setSuperSubscriber(activate)
            if (activate) {
                com.example.audio.DuoSoundPlayer.playFanfare()
                com.example.audio.TtsCoach.speak("Welcome to Super FormFit! Unlimited hearts unlocked.", isUrgent = true)
            } else {
                com.example.audio.DuoSoundPlayer.playClick()
            }
        }
    }

    fun cheerFriend(friendName: String) {
        viewModelScope.launch {
            com.example.audio.DuoSoundPlayer.playCorrect()
            com.example.audio.TtsCoach.speak("Cheered $friendName's streak!", isUrgent = true)
        }
    }

    fun setVirtualCoachMode(enabled: Boolean) {
        _isVirtualCoachMode.value = enabled
        if (!enabled) {
            // Switching to actual camera workout: reset counters to start clean
            _repCount.value = 0
            _sessionSeconds.value = 0
            _repScores.value = emptyList()
            _mistakesList.value = emptyList()
            _currentScore.value = 100
            pullUpBiomechanics.reset()
            _pullUpMetrics.value = PullUpMetrics()
            pushUpBiomechanics.reset()
            _pushUpMetrics.value = PushUpMetrics()
            _exerciseState.value = if (_currentExercise.value == "Pull-up") "HANG" else if (_currentExercise.value == "Plank") "HOLDING" else "STAND"

            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_isActiveSession.value && !_isVirtualCoachMode.value) {
                    delay(1000)
                    _sessionSeconds.value += 1
                    if (_currentExercise.value == "Plank" && _isActiveSession.value) {
                        if (_currentScore.value >= 85) {
                            _repCount.value += 1
                        }
                    }
                }
            }
        } else {
            // In virtual sandbox mode, ensure rep counter is 0 and timer stopped
            timerJob?.cancel()
            _repCount.value = 0
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        com.example.audio.DuoSoundPlayer.isSoundEnabled = enabled
        if (enabled) {
            com.example.audio.DuoSoundPlayer.playClick()
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        com.example.audio.TtsCoach.isTtsEnabled = enabled
        if (enabled) {
            com.example.audio.TtsCoach.speak("Voice coach enabled", isUrgent = true)
        } else {
            com.example.audio.TtsCoach.stop()
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
        pullUpBiomechanics.reset()
        _pullUpMetrics.value = PullUpMetrics()
        pushUpBiomechanics.reset()
        _pushUpMetrics.value = PushUpMetrics()
        _exerciseState.value = if (exerciseType == "Pull-up") "HANG" else if (exerciseType == "Plank") "HOLDING" else "STAND"
        _currentFeedback.value = when (exerciseType) {
            "Pull-up" -> "Grip the bar with overhand grip. Hang fully extended."
            "Squat" -> "Get ready to squat! Stand straight."
            "Push-up" -> "Get into plank position. Prepare to lower."
            "Lunge" -> "Prepare to lunge. Keep hips square."
            "Plank" -> "Hold a solid plank. Keep body straight!"
            else -> "Ready!"
        }

        if (!_isVirtualCoachMode.value) {
            com.example.audio.TtsCoach.speak(_currentFeedback.value, isUrgent = true)
        }

        timerJob?.cancel()
        if (!_isVirtualCoachMode.value) {
            timerJob = viewModelScope.launch {
                while (_isActiveSession.value && !_isVirtualCoachMode.value) {
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
        }
    }

    // This method is called from real Camera Analyzer frames when Camera Mode is active
    fun processCameraFrameAnalysis(
        score: Int,
        mistake: String?,
        feedback: String,
        isRepCompleted: Boolean,
        state: String = "READY",
        skeleton: PoseSkeleton? = null,
        velocityMps: Double = 0.0,
        peakPowerW: Int = 0,
        speedLossPct: Int = 0
    ) {
        // Virtual sandbox is purely for informational guide - never process camera or count reps
        if (_isVirtualCoachMode.value) return

        _currentScore.value = score
        _currentFeedback.value = feedback

        if (skeleton != null && _currentExercise.value == "Pull-up") {
            val metrics = pullUpBiomechanics.processFrame(skeleton)
            _pullUpMetrics.value = metrics
            _exerciseState.value = metrics.phase
            _liveSpeedLossPct.value = metrics.speedLossPct
            _livePeakPowerW.value = metrics.peakPowerW
            _liveVelocityMps.value = metrics.currentSpeedMps
            
            // Sync with rep counter
            if (metrics.repCount > _repCount.value) {
                val added = metrics.repCount - _repCount.value
                _repCount.value = metrics.repCount
                val repScore = if (metrics.lastRep?.fullLockout == true) 98 else 85
                for (i in 0 until added) {
                    _repScores.value = _repScores.value + repScore
                }
                if (repScore >= 85) {
                    com.example.audio.DuoSoundPlayer.playCorrect()
                } else {
                    com.example.audio.DuoSoundPlayer.playMistake()
                }
                metrics.lastRep?.chinVerdict?.let { verdict ->
                    com.example.audio.TtsCoach.speak("${metrics.repCount}. $verdict", isUrgent = true)
                }
            }
        } else if (skeleton != null && _currentExercise.value == "Push-up") {
            val metrics = pushUpBiomechanics.processFrame(skeleton)
            _pushUpMetrics.value = metrics
            _exerciseState.value = metrics.phase
            _liveSpeedLossPct.value = metrics.speedLossPct
            _livePeakPowerW.value = metrics.peakPowerW
            _liveVelocityMps.value = metrics.currentSpeedMps
            
            // Sync rep counter with biomechanics engine
            if (metrics.repCount > _repCount.value) {
                val added = metrics.repCount - _repCount.value
                _repCount.value = metrics.repCount
                val repScore = when (metrics.lastRep?.formVerdict) {
                    "PERFECT FORM" -> 100
                    "FULL REP" -> 95
                    "CORE ISSUE", "TOO SHALLOW" -> 70
                    "HIP SAG" -> 60
                    else -> 85
                }
                for (i in 0 until added) {
                    _repScores.value = _repScores.value + repScore
                }
                if (repScore >= 85) {
                    com.example.audio.DuoSoundPlayer.playCorrect()
                } else {
                    com.example.audio.DuoSoundPlayer.playMistake()
                }
                metrics.lastRep?.formVerdict?.let { verdict ->
                    com.example.audio.TtsCoach.speak("${metrics.repCount}. $verdict", isUrgent = true)
                }
            }
        } else {
            _exerciseState.value = state
            _liveSpeedLossPct.value = speedLossPct
            _livePeakPowerW.value = peakPowerW
            _liveVelocityMps.value = velocityMps

            if (isRepCompleted) {
                _repCount.value += 1
                _repScores.value = _repScores.value + score
                if (score >= 85) {
                    com.example.audio.DuoSoundPlayer.playCorrect()
                } else {
                    com.example.audio.DuoSoundPlayer.playMistake()
                }
                com.example.audio.TtsCoach.speak("Rep ${_repCount.value}. $feedback", isUrgent = true)
            }
        }

        if (mistake != null) {
            val previousSize = _mistakesList.value.size
            if (Math.random() > 0.7) {
                _mistakesList.value = _mistakesList.value + mistake
                if (_mistakesList.value.size > previousSize) {
                    com.example.audio.DuoSoundPlayer.playMistake()
                }
            }
            val spokenCorrection = feedback.ifBlank { mistake }
            com.example.audio.TtsCoach.speak(spokenCorrection, isUrgent = false, minIntervalMs = 3000L)
        }
    }

    fun stopAndSaveWorkout() {
        if (_isVirtualCoachMode.value) {
            // Virtual sandbox is purely for informational guide - do not log or count reps!
            _isActiveSession.value = false
            timerJob?.cancel()
            _lastCompletedSession.value = null
            return
        }

        _isActiveSession.value = false
        timerJob?.cancel()
        
        com.example.audio.DuoSoundPlayer.playFanfare()
        com.example.audio.TtsCoach.speak("Workout finished! Awesome effort.", isUrgent = true)

        val exercise = _currentExercise.value
        val reps = _repCount.value
        val duration = _sessionSeconds.value
        val mistakes = _mistakesList.value.size
        val bodyWeight = userBodyProfile.value.weightKg
        
        // Form metrics math
        val avgScore = if (_repScores.value.isNotEmpty()) {
            _repScores.value.average().toInt()
        } else if (exercise == "Plank") {
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
        var xpEarned = 20
        if (exercise == "Plank") {
            xpEarned += reps
        } else {
            _repScores.value.forEach { score ->
                xpEarned += if (score >= 80) 5 else 2
            }
        }

        if (avgScore >= 90 && reps >= 5) {
            xpEarned += 30
        }

        // Check if Personal Best
        val previousBest = allSessions.value
            .filter { it.exerciseType == exercise }
            .maxOfOrNull { it.totalReps } ?: 0
        _isPersonalBest.value = reps > 0 && reps > previousBest

        _lastSessionRepScores.value = _repScores.value
        _lastSessionMistakes.value = _mistakesList.value

        val kcal = if (exercise == "Pull-up") {
            if (_pullUpMetrics.value.totalKcal > 0.05) _pullUpMetrics.value.totalKcal else (reps * (bodyWeight * 0.95 * 9.81 * 0.45) / 920.0)
        } else if (exercise == "Squat") {
            reps * (bodyWeight * 0.88 * 9.81 * 0.50) / 920.0
        } else if (exercise == "Push-up") {
            if (_pushUpMetrics.value.totalKcal > 0.05) _pushUpMetrics.value.totalKcal
            else reps * (bodyWeight * 0.64 * 9.81 * 0.35) / 920.0
        } else {
            reps * 0.45
        }
        _lastSessionCalories.value = kcal
        val peakPower = when (exercise) {
            "Pull-up" -> _pullUpMetrics.value.peakPowerW
            "Push-up" -> if (_pushUpMetrics.value.peakPowerW > 0) _pushUpMetrics.value.peakPowerW
                         else if (_livePeakPowerW.value > 0) _livePeakPowerW.value
                         else (_repScores.value.size * 35 + (bodyWeight * 3.5).toInt())
            else -> if (_livePeakPowerW.value > 0) _livePeakPowerW.value else (_repScores.value.size * 45 + (bodyWeight * 4.5).toInt())
        }
        _lastSessionPeakPower.value = peakPower

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
        _lastCompletedSession.value = null
        _repCount.value = 0
        timerJob?.cancel()
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
        database.close()
    }
}
