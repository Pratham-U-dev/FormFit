package com.example.cv

import android.os.SystemClock
import kotlin.math.*

data class MuscleState(
    var mR: Double = 1.0,
    var mA: Double = 0.0,
    var mF: Double = 0.0,
    var activation: Double = 0.0,
    var deltaTempC: Double = 0.0,
    var effortIndex: Double = 0.0,
    var heatPowerW: Double = 0.0
)

data class PullUpRepRecord(
    val repNum: Int,
    val durationConcentric: Double,
    val durationHold: Double,
    val durationEccentric: Double,
    val peakVelocity: Double,
    val peakPower: Double,
    val energyKcal: Double,
    val speedLossPct: Int,
    val swayCm: Double,
    val fullLockout: Boolean,
    val chinVerdict: String = "GOOD REP"
)

data class PullUpMetrics(
    val phase: String = "HANG",
    val repCount: Int = 0,
    val chinAtBarCount: Int = 0,
    val speedLossPct: Int = 0,
    val peakPowerW: Int = 0,
    val currentSpeedMps: Double = 0.0,
    val elbowAngle: Int = 180,
    val totalKcal: Double = 0.0,
    val totalHeatKj: Double = 0.0,
    val latsTempRise: Double = 0.0,
    val latsFatiguedPct: Int = 0,
    val bicepsFatiguedPct: Int = 0,
    val lastRep: PullUpRepRecord? = null,
    val latsEffort: Double = 0.0,
    val bicepsEffort: Double = 0.0,
    val forearmsEffort: Double = 0.0,
    val legsEffort: Double = 0.0,
    val shoulderYHistory: List<Float> = emptyList(),
    val barY: Float? = null,
    val vRefRep1: Double = 0.0,
    val currentPowerW: Int = 0
)

class PullUpBiomechanics(
    private var userWeightKg: Double = 75.0,
    private var userHeightCm: Double = 175.0,
    private var userArmLengthCm: Double = 65.0
) {
    private var lastTime = 0L
    private val G = 9.80665
    
    // Effective mass lifted in pull-ups (excluding forearms & hands hanging: ~94-96%)
    private var liftedMassKg = userWeightKg * 0.95
    private var armLengthM = userArmLengthCm / 100.0
    
    private var phase = "HANG"
    private var repCount = 0
    private var peakPowerSessionW = 0.0
    private var cumulativeWorkJ = 0.0
    private var cumulativeKcal = 0.0
    
    private val shoulderYHistory = mutableListOf<Float>()
    private val timestamps = mutableListOf<Long>()
    
    private var currentSpeed = 0.0
    private var prevSpeed = 0.0
    private var currentAccel = 0.0
    private var currentPower = 0.0
    
    // Normalized screen units per physical meter (auto-calibrated from arm/torso landmarks)
    private var normUnitsPerM = 0.42
    
    // Muscle states
    private val lats = MuscleState()
    private val biceps = MuscleState()
    private val forearms = MuscleState()
    private val legs = MuscleState()
    
    // Rep state
    private var repStartTime = 0.0
    private var repTopTime = 0.0
    private var repEccStartTime = 0.0
    private var repPeakConcVel = 0.0
    private var repPeakPower = 0.0
    private var repMinElbow = 180.0
    private var repBaseShoulderY = 0f
    private var repMinShoulderY = 1f
    private val repHipXHistory = mutableListOf<Float>()
    
    private val reps = mutableListOf<PullUpRepRecord>()
    private var vRefRep1 = 0.0
    
    private var cachedMetrics = PullUpMetrics()

    fun updateBodyParams(weightKg: Double, heightCm: Double, armLengthCm: Double) {
        userWeightKg = weightKg.coerceIn(30.0, 250.0)
        userHeightCm = heightCm.coerceIn(100.0, 250.0)
        userArmLengthCm = armLengthCm.coerceIn(30.0, 120.0)
        liftedMassKg = userWeightKg * 0.95
        armLengthM = userArmLengthCm / 100.0
    }

    fun reset() {
        lastTime = 0L
        phase = "HANG"
        repCount = 0
        peakPowerSessionW = 0.0
        cumulativeWorkJ = 0.0
        cumulativeKcal = 0.0
        shoulderYHistory.clear()
        timestamps.clear()
        currentSpeed = 0.0
        prevSpeed = 0.0
        currentAccel = 0.0
        currentPower = 0.0
        reps.clear()
        vRefRep1 = 0.0
        repStartTime = 0.0
        repTopTime = 0.0
        repEccStartTime = 0.0
        repPeakConcVel = 0.0
        repPeakPower = 0.0
        repMinElbow = 180.0
        repHipXHistory.clear()
        lats.deltaTempC = 0.0
        lats.mF = 0.0
        biceps.mF = 0.0
        cachedMetrics = PullUpMetrics()
    }
    
    fun processFrame(skeleton: PoseSkeleton): PullUpMetrics {
        val now = SystemClock.elapsedRealtime()
        if (lastTime == 0L) {
            lastTime = now
            return cachedMetrics
        }
        val dt = ((now - lastTime) / 1000.0).coerceIn(0.01, 0.25)
        lastTime = now
        
        // Landmark resolution with dual-side fallback
        val lWrist = skeleton.wristLeft
        val rWrist = skeleton.wristRight
        val lShoulder = skeleton.shoulderLeft
        val rShoulder = skeleton.shoulderRight
        val lElbow = skeleton.elbowLeft
        val rElbow = skeleton.elbowRight
        val lHip = skeleton.hipLeft
        val rHip = skeleton.hipRight
        
        val hasLeftArm = lWrist != null && lElbow != null && lShoulder != null
        val hasRightArm = rWrist != null && rElbow != null && rShoulder != null
        
        if (!hasLeftArm && !hasRightArm) {
            return cachedMetrics
        }
        
        // Midpoint coordinates (normalized 0.0 to 1.0)
        val shMidY = when {
            lShoulder != null && rShoulder != null -> (lShoulder.y + rShoulder.y) / 2f
            lShoulder != null -> lShoulder.y
            else -> rShoulder!!.y
        }
        
        val wrMidY = when {
            lWrist != null && rWrist != null -> (lWrist.y + rWrist.y) / 2f
            lWrist != null -> lWrist.y
            else -> rWrist!!.y
        }
        
        // Dynamic anthropometric scale calibration
        val measuredArmY = abs(shMidY - wrMidY)
        if (measuredArmY in 0.12f..0.60f) {
            val instantUnitsPerM = (measuredArmY / armLengthM)
            normUnitsPerM = (0.92 * normUnitsPerM + 0.08 * instantUnitsPerM).coerceIn(0.20, 0.90)
        }
        
        // Elbow angle calculation
        val elLeft = if (hasLeftArm) calculateAngle(lShoulder!!.x, lShoulder.y, lElbow!!.x, lElbow.y, lWrist!!.x, lWrist.y) else null
        val elRight = if (hasRightArm) calculateAngle(rShoulder!!.x, rShoulder.y, rElbow!!.x, rElbow.y, rWrist!!.x, rWrist.y) else null
        val currentElbowAngle = when {
            elLeft != null && elRight != null -> (elLeft + elRight) / 2.0
            elLeft != null -> elLeft
            else -> elRight!!
        }
        
        // Kinematics velocity & acceleration filtering
        shoulderYHistory.add(shMidY)
        timestamps.add(now)
        if (shoulderYHistory.size > 10) {
            shoulderYHistory.removeAt(0)
            timestamps.removeAt(0)
        }
        
        if (shoulderYHistory.size >= 3) {
            val yCurr = shoulderYHistory.takeLast(2).average().toFloat()
            val yPrev = shoulderYHistory.take(2).average().toFloat()
            val dtStep = ((now - timestamps.first()) / 1000.0).coerceIn(0.03, 0.40)
            
            // In screen coordinates, moving UP means y is decreasing (yPrev > yCurr is positive velocity)
            val deltaNorm = (yPrev - yCurr)
            val deltaMeters = deltaNorm / normUnitsPerM
            
            // Raw velocity in m/s
            val rawVel = deltaMeters / dtStep
            
            // Low-pass exponential filter
            prevSpeed = currentSpeed
            currentSpeed = (0.60 * currentSpeed + 0.40 * rawVel)
            if (abs(currentSpeed) < 0.03) {
                currentSpeed = 0.0
            }
            
            currentAccel = (currentSpeed - prevSpeed) / dt
        }
        
        val timeS = now / 1000.0
        val concentricVelocity = max(0.0, currentSpeed)
        
        // Instantaneous Mechanical Force & Power
        val dynamicAccel = max(0.0, currentAccel)
        val forceN = liftedMassKg * (G + dynamicAccel)
        
        if (phase == "PULL" && concentricVelocity > 0.04) {
            currentPower = forceN * concentricVelocity
            if (currentPower > repPeakPower) {
                repPeakPower = currentPower
            }
            if (currentPower > peakPowerSessionW) {
                peakPowerSessionW = currentPower
            }
        } else {
            currentPower = 0.0
        }
        
        // Hip tracking for sway
        val hipMidX = when {
            lHip != null && rHip != null -> (lHip.x + rHip.x) / 2f
            lHip != null -> lHip.x
            rHip != null -> rHip.x
            else -> null
        }
        
        // Pull-Up Finite State Machine
        when (phase) {
            "HANG" -> {
                // User hanging from bar, initiates pull when elbow flexes (<135°) or upward speed is positive
                if (currentElbowAngle < 132.0 || (currentSpeed > 0.08 && currentElbowAngle < 145.0)) {
                    phase = "PULL"
                    repStartTime = timeS
                    repMinElbow = currentElbowAngle
                    repPeakConcVel = max(0.12, concentricVelocity)
                    repPeakPower = currentPower
                    repBaseShoulderY = shMidY
                    repMinShoulderY = shMidY
                    repHipXHistory.clear()
                    if (hipMidX != null) repHipXHistory.add(hipMidX)
                }
            }
            "PULL" -> {
                if (hipMidX != null) repHipXHistory.add(hipMidX)
                if (concentricVelocity > repPeakConcVel) repPeakConcVel = concentricVelocity
                if (currentPower > repPeakPower) repPeakPower = currentPower
                if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                if (shMidY < repMinShoulderY) repMinShoulderY = shMidY
                
                // Reached top: elbows flexed deep (<= 95°) OR upward velocity stops near top
                if (currentElbowAngle <= 95.0 || (currentElbowAngle <= 105.0 && currentSpeed <= 0.02)) {
                    phase = "TOP"
                    repTopTime = timeS
                } else if (currentSpeed < -0.15 && currentElbowAngle > 115.0) {
                    // Aborted pull without reaching top
                    phase = "LOWER"
                    repTopTime = timeS
                    repEccStartTime = timeS
                }
            }
            "TOP" -> {
                if (hipMidX != null) repHipXHistory.add(hipMidX)
                if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                if (shMidY < repMinShoulderY) repMinShoulderY = shMidY
                
                // Initiates descent
                if (currentElbowAngle > 105.0 || currentSpeed < -0.05) {
                    phase = "LOWER"
                    repEccStartTime = timeS
                }
            }
            "LOWER" -> {
                // Completed return to full extension at bottom (lockout >= 138°)
                if (currentElbowAngle >= 138.0) {
                    recordRep(timeS, currentElbowAngle)
                    phase = "HANG"
                }
            }
        }
        
        // Neuromuscular and Fatigue Modeling
        val isActivePhase = phase == "PULL" || phase == "TOP" || phase == "LOWER"
        val targetAct = when (phase) {
            "PULL" -> 1.0
            "TOP" -> 0.8
            "LOWER" -> 0.5
            else -> 0.0
        }
        
        if (isActivePhase) {
            updateMuscle(lats, dt, targetAct, 0.734, 124.0)
            updateMuscle(biceps, dt, targetAct * 0.9, 0.402, 78.0)
        } else {
            lats.activation = (lats.activation - dt * 0.5).coerceAtLeast(0.0)
            biceps.activation = (biceps.activation - dt * 0.5).coerceAtLeast(0.0)
            lats.heatPowerW = 0.0
            biceps.heatPowerW = 0.0
        }
        
        // Real-time Velocity Based Training (VBT) Speed Loss % vs Rep 1 calculation
        val currentVbtSpeedLoss = if (vRefRep1 > 0.05) {
            if (phase == "PULL") {
                max(0, ((1.0 - (repPeakConcVel / vRefRep1)) * 100.0).toInt()).coerceIn(0, 95)
            } else {
                reps.lastOrNull()?.speedLossPct ?: 0
            }
        } else {
            0
        }
        
        cachedMetrics = PullUpMetrics(
            phase = phase,
            repCount = repCount,
            chinAtBarCount = repCount,
            speedLossPct = currentVbtSpeedLoss,
            peakPowerW = peakPowerSessionW.toInt(),
            currentSpeedMps = max(0.0, currentSpeed),
            elbowAngle = currentElbowAngle.toInt(),
            totalKcal = cumulativeKcal,
            totalHeatKj = (cumulativeWorkJ * 0.35) / 1000.0,
            latsTempRise = lats.deltaTempC,
            latsFatiguedPct = min(99, (repCount * 8 + (lats.mF * 50).toInt())),
            bicepsFatiguedPct = min(99, (repCount * 7 + (biceps.mF * 45).toInt())),
            lastRep = reps.lastOrNull(),
            latsEffort = lats.activation,
            bicepsEffort = biceps.activation,
            forearmsEffort = if (isActivePhase) 0.7 else 0.0,
            legsEffort = 0.05,
            shoulderYHistory = shoulderYHistory.toList(),
            barY = wrMidY,
            vRefRep1 = vRefRep1,
            currentPowerW = currentPower.toInt()
        )
        
        return cachedMetrics
    }
    
    private fun recordRep(timeS: Double, currentElbowAngle: Double) {
        repCount++
        val durConc = max(0.3, (if (repTopTime > 0) repTopTime else timeS) - repStartTime)
        val durHold = max(0.0, repEccStartTime - repTopTime)
        val durEcc = max(0.3, timeS - (if (repEccStartTime > 0) repEccStartTime else repTopTime))
        
        val actualPeakVel = max(0.15, repPeakConcVel)
        
        // Rep 1 establishes baseline velocity reference (vRefRep1)
        if (vRefRep1 == 0.0 || repCount == 1) {
            vRefRep1 = actualPeakVel
        }
        
        // VBT Speed loss % vs Rep 1
        val vl = if (vRefRep1 > 0.05) {
            max(0, ((1.0 - (actualPeakVel / vRefRep1)) * 100.0).toInt()).coerceIn(0, 95)
        } else 0
        
        // Biomechanical work: Lifted mass * G * ROM
        val romNorm = abs(repBaseShoulderY - repMinShoulderY)
        val romM = (romNorm / normUnitsPerM).coerceIn(0.20, 0.85)
        val workJ = liftedMassKg * G * romM
        cumulativeWorkJ += workJ
        
        // Biological metabolic energy consumption:
        // Mechanical work / human muscular efficiency (22%) + eccentric work (35%)
        val kcal = (workJ / 0.22 + workJ * 0.35 / 0.22) / 4184.0
        cumulativeKcal += kcal
        
        val swayCm = if (repHipXHistory.isNotEmpty()) {
            ((repHipXHistory.maxOrNull()!! - repHipXHistory.minOrNull()!!) / normUnitsPerM * 100.0).coerceIn(0.0, 45.0)
        } else 3.0
        
        val lockout = currentElbowAngle >= 140.0
        val isDeep = repMinElbow <= 95.0
        
        val verdict = when {
            isDeep && lockout && swayCm < 10.0 -> "PERFECT FORM"
            isDeep && lockout -> "FULL REP"
            isDeep -> "GOOD DEPTH"
            lockout -> "FULL LOCKOUT"
            else -> "CONTROLLED REP"
        }
        
        val finalRepPeakPower = if (repPeakPower > 0.0) repPeakPower else (liftedMassKg * G * actualPeakVel)
        
        // Lats temperature rise per completed rep
        lats.deltaTempC = min(2.8, lats.deltaTempC + 0.18)
        
        reps.add(PullUpRepRecord(
            repNum = repCount,
            durationConcentric = durConc,
            durationHold = durHold,
            durationEccentric = durEcc,
            peakVelocity = actualPeakVel,
            peakPower = finalRepPeakPower,
            energyKcal = kcal,
            speedLossPct = vl,
            swayCm = swayCm,
            fullLockout = lockout,
            chinVerdict = verdict
        ))
        
        repTopTime = 0.0
        repEccStartTime = 0.0
        repPeakConcVel = 0.0
        repPeakPower = 0.0
    }
    
    private fun updateMuscle(state: MuscleState, dt: Double, targetAct: Double, mass: Double, mvic: Double) {
        val tau = if (targetAct > state.activation) 0.04 else 0.08
        state.activation += (targetAct - state.activation) * (dt / tau)
        state.activation = state.activation.coerceIn(0.0, 1.2)
        
        val fRate = 0.025
        val rRate = 0.005
        
        var cCtrl = 5.0 * (min(1.0, state.activation) - state.mA)
        if (cCtrl > 0) cCtrl = min(cCtrl, state.mR / dt)
        else cCtrl = max(cCtrl, -state.mA / dt)
        
        val dMa = (cCtrl - fRate * state.mA) * dt
        val dMf = (fRate * state.mA - rRate * state.mF) * dt
        
        state.mA = (state.mA + dMa).coerceIn(0.0, 1.0)
        state.mF = (state.mF + dMf).coerceIn(0.0, 1.0)
        state.mR = (1.0 - state.mA - state.mF).coerceIn(0.0, 1.0)
        
        state.heatPowerW = state.activation * mass * 45.0
        state.effortIndex = min(1.0, 0.90 * (state.mA + state.mF) + 0.10 * (state.deltaTempC / 2.0))
    }
    
    private fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val rad = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        var deg = abs(Math.toDegrees(rad.toDouble()))
        if (deg > 180.0) deg = 360.0 - deg
        return deg
    }
}
