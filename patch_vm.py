import re

with open('app/src/main/java/com/example/viewmodel/WorkoutViewModel.kt', 'r') as f:
    content = f.read()

# Add imports
content = re.sub(
    r'import com.example.cv.PoseSkeleton\n',
    'import com.example.cv.PoseSkeleton\nimport com.example.cv.PullUpBiomechanics\nimport com.example.cv.PullUpMetrics\n',
    content
)

# Add properties
content = re.sub(
    r'    private val _lastCompletedSession = MutableStateFlow<WorkoutSession\?>\(null\)',
    '    private val pullUpBiomechanics = PullUpBiomechanics()\n    private val _pullUpMetrics = MutableStateFlow(PullUpMetrics())\n    val pullUpMetrics = _pullUpMetrics.asStateFlow()\n\n    private val _lastCompletedSession = MutableStateFlow<WorkoutSession?>(null)',
    content
)

# Update processCameraFrameAnalysis
target = """    fun processCameraFrameAnalysis(
        score: Int,
        mistake: String?,
        feedback: String,
        isRepCompleted: Boolean,
        skeleton: PoseSkeleton? = null
    ) {
        _currentScore.value = score
        _currentFeedback.value = feedback

        if (isRepCompleted) {
            _repCount.value += 1
            _repScores.value = _repScores.value + score
            if (score >= 85) {
                com.example.audio.DuoSoundPlayer.playCorrect()
            } else {
                com.example.audio.DuoSoundPlayer.playMistake()
            }
        }"""

replacement = """    fun processCameraFrameAnalysis(
        score: Int,
        mistake: String?,
        feedback: String,
        isRepCompleted: Boolean,
        skeleton: PoseSkeleton? = null
    ) {
        _currentScore.value = score
        _currentFeedback.value = feedback

        if (skeleton != null && _currentExercise.value == "Pull-up") {
            val metrics = pullUpBiomechanics.processFrame(skeleton)
            _pullUpMetrics.value = metrics
            
            // Sync with old rep counter to avoid breaking gamification logic
            if (metrics.repCount > _repCount.value) {
                val repScore = if (metrics.lastRep?.chinVerdict == "CHIN ABOVE BAR") 100 
                               else if (metrics.lastRep?.chinVerdict == "~ CHIN AT BAR") 85 
                               else 60
                
                _repScores.value = _repScores.value + repScore
                if (repScore >= 85) com.example.audio.DuoSoundPlayer.playCorrect()
                else com.example.audio.DuoSoundPlayer.playMistake()
            }
            _repCount.value = metrics.repCount
        } else {
            if (isRepCompleted) {
                _repCount.value += 1
                _repScores.value = _repScores.value + score
                if (score >= 85) {
                    com.example.audio.DuoSoundPlayer.playCorrect()
                } else {
                    com.example.audio.DuoSoundPlayer.playMistake()
                }
            }
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/viewmodel/WorkoutViewModel.kt', 'w') as f:
    f.write(content)
