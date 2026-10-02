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
    val chinVerdict: String = "GOOD REP" // Kept for backwards compatibility
)

data class PullUpMetrics(
    val phase: String = "HANG",
    val repCount: Int = 0,
    val chinAtBarCount: Int = 0, // Deprecated, kept for backward compat
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
    val barY: Float? = null
)

class PullUpBiomechanics {
    private var lastTime = 0L
    private val massKg = 79.0
    private val liftedMassKg = massKg * 0.956
    private val G = 9.81
    
    private var phase = "HANG"
    private var repCount = 0
    private var peakPowerSessionW = 0.0
    private var cumulativeWorkJ = 0.0
    private var cumulativeKcal = 0.0
    
    private val shoulderYHistory = mutableListOf<Float>()
    private val timestamps = mutableListOf<Long>()
    
    private var currentSpeed = 0.0
    private var currentAccel = 0.0
    private var currentPower = 0.0
    
    private var pxPerM = 800.0
    
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
    private var repMinElbow = 180.0
    private var repBaseShoulderY = 0f
    private var repMinShoulderY = 1f
    private val repHipXHistory = mutableListOf<Float>()
    
    private val reps = mutableListOf<PullUpRepRecord>()
    private var vRefRep1 = 0.0
    
    private var cachedMetrics = PullUpMetrics()

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
        currentAccel = 0.0
        currentPower = 0.0
        reps.clear()
        vRefRep1 = 0.0
        repStartTime = 0.0
        repTopTime = 0.0
        repEccStartTime = 0.0
        repPeakConcVel = 0.0
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
        val dt = ((now - lastTime) / 1000.0).coerceIn(0.001, 0.2)
        lastTime = now
        
        // Landmark resolution with robust side fallback
        val lWrist = skeleton.wristLeft
        val rWrist = skeleton.wristRight
        val lShoulder = skeleton.shoulderLeft
        val rShoulder = skeleton.shoulderRight
        val lElbow = skeleton.elbowLeft
        val rElbow = skeleton.elbowRight
        val lHip = skeleton.hipLeft
        val rHip = skeleton.hipRight
        
        // Ensure at least one arm and shoulder are detectable
        val hasLeftArm = lWrist != null && lElbow != null && lShoulder != null
        val hasRightArm = rWrist != null && rElbow != null && rShoulder != null
        
        if (!hasLeftArm && !hasRightArm) {
            // Return cached metrics without resetting rep counter or phase
            return cachedMetrics
        }
        
        // Midpoint coordinates
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
        
        // Dynamic arm-length calibration
        val measuredArmY = abs(shMidY - wrMidY)
        if (measuredArmY > 0.08f) {
            val expectedArmM = 0.60
            val computedPxPerM = (measuredArmY / expectedArmM)
            pxPerM = (0.95 * pxPerM + 0.05 * computedPxPerM).coerceIn(300.0, 2000.0)
        }
        
        // Calculate elbow angles
        val elLeft = if (hasLeftArm) calculateAngle(lShoulder!!.x, lShoulder.y, lElbow!!.x, lElbow.y, lWrist!!.x, lWrist.y) else null
        val elRight = if (hasRightArm) calculateAngle(rShoulder!!.x, rShoulder.y, rElbow!!.x, rElbow.y, rWrist!!.x, rWrist.y) else null
        val currentElbowAngle = when {
            elLeft != null && elRight != null -> (elLeft + elRight) / 2.0
            elLeft != null -> elLeft
            else -> elRight!!
        }
        
        // Kinematics filtering
        shoulderYHistory.add(shMidY)
        timestamps.add(now)
        if (shoulderYHistory.size > 15) {
            shoulderYHistory.removeAt(0)
            timestamps.removeAt(0)
        }
        
        if (shoulderYHistory.size >= 4) {
            val yCurr = shoulderYHistory.takeLast(2).average().toFloat()
            val yPrev = shoulderYHistory.take(2).average().toFloat()
            val dtStep = ((now - timestamps.first()) / 1000.0).coerceAtLeast(0.05)
            
            val deltaM = (yPrev - yCurr) / pxPerM
            // Deadband filter: ignore sub-threshold camera sensor jitter
            val rawSpeed = if (abs(deltaM) > 0.005) deltaM / dtStep else 0.0
            
            // EMA smoothing
            currentSpeed = (0.75 * currentSpeed + 0.25 * rawSpeed)
            if (abs(currentSpeed) < 0.03) currentSpeed = 0.0
            currentAccel = (currentSpeed - (deltaM / dtStep)) / dtStep
        }
        
        // Power calculation (only when actively pulling upward)
        val forceN = liftedMassKg * (G + max(0.0, currentAccel))
        currentPower = if (phase == "PULL" && currentSpeed > 0.05) {
            forceN * currentSpeed
        } else {
            0.0
        }
        if (currentPower > peakPowerSessionW) {
            peakPowerSessionW = currentPower
        }
        
        // Hip tracking for sway
        val hipMidX = when {
            lHip != null && rHip != null -> (lHip.x + rHip.x) / 2f
            lHip != null -> lHip.x
            rHip != null -> rHip.x
            else -> null
        }
        
        val timeS = now / 1000.0
        
        // Pull-Up Finite State Machine
        when (phase) {
            "HANG" -> {
                // User hanging from bar, initiates pull when elbow flexes or speed is positive
                if (currentElbowAngle < 130.0 || (currentSpeed > 0.08 && currentElbowAngle < 145.0)) {
                    phase = "PULL"
                    repStartTime = timeS
                    repMinElbow = currentElbowAngle
                    repPeakConcVel = max(0.1, currentSpeed)
                    repBaseShoulderY = shMidY
                    repMinShoulderY = shMidY
                    repHipXHistory.clear()
                    if (hipMidX != null) repHipXHistory.add(hipMidX)
                }
            }
            "PULL" -> {
                if (hipMidX != null) repHipXHistory.add(hipMidX)
                if (currentSpeed > repPeakConcVel) repPeakConcVel = currentSpeed
                if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                if (shMidY < repMinShoulderY) repMinShoulderY = shMidY
                
                // Reached top: elbows flexed deep (<= 95°) OR upward velocity stops near top
                if (currentElbowAngle <= 95.0 || (currentElbowAngle <= 105.0 && currentSpeed <= 0.02)) {
                    phase = "TOP"
                    repTopTime = timeS
                } else if (currentSpeed < -0.12 && currentElbowAngle > 115.0) {
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
        
        // Neuromuscular and Fatigue Modeling (Only active during movement phases)
        val isActivePhase = phase == "PULL" || phase == "TOP" || phase == "LOWER"
        val targetAct = when (phase) {
            "PULL" -> 1.0
            "TOP" -> 0.8
            "LOWER" -> 0.5
            else -> 0.0 // HANG or stationary: 0 baseline activation
        }
        
        if (isActivePhase) {
            updateMuscle(lats, dt, targetAct, 0.734, 124.0)
            updateMuscle(biceps, dt, targetAct * 0.9, 0.402, 78.0)
        } else {
            // Resting state: slowly cool down and recover
            lats.activation = (lats.activation - dt * 0.5).coerceAtLeast(0.0)
            biceps.activation = (biceps.activation - dt * 0.5).coerceAtLeast(0.0)
            lats.heatPowerW = 0.0
            biceps.heatPowerW = 0.0
        }
        
        val vl = if (vRefRep1 > 0) max(0.0, (1.0 - (currentSpeed / vRefRep1)) * 100.0).toInt() else 0
        
        cachedMetrics = PullUpMetrics(
            phase = phase,
            repCount = repCount,
            chinAtBarCount = repCount, // 1:1 with reps
            speedLossPct = reps.lastOrNull()?.speedLossPct ?: vl,
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
            barY = wrMidY
        )
        
        return cachedMetrics
    }
    
    private fun recordRep(timeS: Double, currentElbowAngle: Double) {
        repCount++
        val durConc = max(0.3, (if (repTopTime > 0) repTopTime else timeS) - repStartTime)
        val durHold = max(0.0, repEccStartTime - repTopTime)
        val durEcc = max(0.3, timeS - (if (repEccStartTime > 0) repEccStartTime else repTopTime))
        
        if (vRefRep1 == 0.0 && repPeakConcVel > 0.1) {
            vRefRep1 = repPeakConcVel
        }
        val vl = if (vRefRep1 > 0) max(0.0, (1.0 - (repPeakConcVel / vRefRep1)) * 100.0).toInt() else 0
        
        // Biomechanical work: lifted mass * g * ROM
        val romM = abs(repBaseShoulderY - repMinShoulderY) / pxPerM
        val romCm = (romM * 100.0).coerceIn(20.0, 75.0)
        val workJ = liftedMassKg * G * (romCm / 100.0)
        cumulativeWorkJ += workJ
        
        // Biological metabolic energy: work / efficiency (0.22) + eccentric work (0.35)
        val kcal = (workJ / 0.22 + workJ * 0.35 / 0.22) / 4184.0
        cumulativeKcal += kcal
        
        val swayCm = if (repHipXHistory.isNotEmpty()) {
            ((repHipXHistory.maxOrNull()!! - repHipXHistory.minOrNull()!!) / pxPerM * 100.0).coerceIn(0.0, 40.0)
        } else 3.0
        
        val lockout = currentElbowAngle >= 142.0
        val isDeep = repMinElbow <= 95.0
        
        val verdict = when {
            isDeep && lockout && swayCm < 10.0 -> "PERFECT FORM"
            isDeep && lockout -> "FULL REP"
            isDeep -> "GOOD DEPTH"
            lockout -> "FULL LOCKOUT"
            else -> "CONTROLLED REP"
        }
        
        // Muscle temp rise per completed rep
        lats.deltaTempC = min(2.5, lats.deltaTempC + 0.18)
        
        reps.add(PullUpRepRecord(
            repNum = repCount,
            durationConcentric = durConc,
            durationHold = durHold,
            durationEccentric = durEcc,
            peakVelocity = max(0.2, repPeakConcVel),
            peakPower = liftedMassKg * G * max(0.2, repPeakConcVel),
            energyKcal = kcal,
            speedLossPct = vl,
            swayCm = swayCm,
            fullLockout = lockout,
            chinVerdict = verdict
        ))
        
        repTopTime = 0.0
        repEccStartTime = 0.0
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
