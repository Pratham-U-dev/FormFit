package com.example.cv

import android.os.SystemClock
import kotlin.math.*

data class PushUpRepRecord(
    val repNum: Int,
    val durationLowering: Double,      // Eccentric (lowering) phase duration in seconds
    val durationHold: Double,           // Pause at bottom in seconds
    val durationPress: Double,          // Concentric (pressing up) phase duration in seconds
    val peakPressVelocity: Double,      // Peak upward velocity in m/s during press
    val peakPower: Double,              // Peak mechanical power in Watts
    val energyKcal: Double,             // Metabolic energy in kcal for this rep
    val speedLossPct: Int,              // VBT: % speed loss vs Rep 1 (0 = fresh, 40+ = very fatigued)
    val minElbowAngle: Double,          // Minimum elbow angle reached (lower = deeper rep)
    val hipSagDetected: Boolean,        // True if body alignment < 165° was detected during rep
    val fullLockout: Boolean,           // True if elbow returned to >= 155° at top
    val formVerdict: String             // Human-readable verdict string
)

data class PushUpMetrics(
    val phase: String = "PLANK",         // Current phase: PLANK, LOWER, BOTTOM, PRESS
    val repCount: Int = 0,
    val speedLossPct: Int = 0,
    val peakPowerW: Int = 0,
    val currentSpeedMps: Double = 0.0,   // Current upward velocity (positive = pressing up)
    val elbowAngle: Int = 170,
    val bodyAlignmentAngle: Int = 180,   // Shoulder-Hip-Ankle angle
    val totalKcal: Double = 0.0,
    val totalHeatKj: Double = 0.0,
    val pecsTempRise: Double = 0.0,      // Pectoralis temperature rise in °C (modelled)
    val pecsFatiguedPct: Int = 0,
    val tricepsFatiguedPct: Int = 0,
    val lastRep: PushUpRepRecord? = null,
    val pecsEffort: Double = 0.0,
    val tricepsEffort: Double = 0.0,
    val frontDeltsEffort: Double = 0.0,
    val coreEffort: Double = 0.0,
    val shoulderYHistory: List<Float> = emptyList(),
    val vRefRep1: Double = 0.0,
    val currentPowerW: Int = 0
)

class PushUpBiomechanics(
    private var userWeightKg: Double = 75.0,
    private var userHeightCm: Double = 175.0
) {
    private var lastTime = 0L
    private val G = 9.80665

    // Lifted mass in push-up: ~64% of total bodyweight
    private var liftedMassKg = userWeightKg * 0.64

    private var phase = "PLANK"
    private var repCount = 0
    private var peakPowerSessionW = 0.0
    private var cumulativeWorkJ = 0.0
    private var cumulativeKcal = 0.0
    private var cumulativeHeatKj = 0.0

    private val shoulderYHistory = mutableListOf<Float>()
    private val timestamps = mutableListOf<Long>()

    private var currentSpeed = 0.0
    private var prevSpeed = 0.0
    private var currentAccel = 0.0
    private var currentPower = 0.0

    // Dynamic scale: Normalized units per meter
    private var normUnitsPerM = 0.45

    // Muscle states
    private val pecs = MuscleState()
    private val triceps = MuscleState()
    private val frontDelts = MuscleState()
    private val core = MuscleState()

    // Rep tracking state
    private var repStartTime = 0.0
    private var repBottomTime = 0.0
    private var repPressStartTime = 0.0
    private var repPeakPressVel = 0.0
    private var repPeakPower = 0.0
    private var repMinElbow = 180.0
    private var repBaseShoulderY = 0f
    private var repMaxShoulderY = 0f
    private var hipSagThisRep = false

    private val reps = mutableListOf<PushUpRepRecord>()
    private var vRefRep1 = 0.0
    private var cachedMetrics = PushUpMetrics()

    fun updateBodyParams(weightKg: Double, heightCm: Double) {
        userWeightKg = weightKg.coerceIn(30.0, 250.0)
        userHeightCm = heightCm.coerceIn(100.0, 250.0)
        liftedMassKg = userWeightKg * 0.64
    }

    fun reset() {
        lastTime = 0L
        phase = "PLANK"
        repCount = 0
        peakPowerSessionW = 0.0
        cumulativeWorkJ = 0.0
        cumulativeKcal = 0.0
        cumulativeHeatKj = 0.0
        shoulderYHistory.clear()
        timestamps.clear()
        currentSpeed = 0.0
        prevSpeed = 0.0
        currentAccel = 0.0
        currentPower = 0.0
        normUnitsPerM = 0.45

        pecs.mR = 1.0; pecs.mA = 0.0; pecs.mF = 0.0; pecs.activation = 0.0; pecs.deltaTempC = 0.0; pecs.effortIndex = 0.0; pecs.heatPowerW = 0.0
        triceps.mR = 1.0; triceps.mA = 0.0; triceps.mF = 0.0; triceps.activation = 0.0; triceps.deltaTempC = 0.0; triceps.effortIndex = 0.0; triceps.heatPowerW = 0.0
        frontDelts.mR = 1.0; frontDelts.mA = 0.0; frontDelts.mF = 0.0; frontDelts.activation = 0.0; frontDelts.deltaTempC = 0.0; frontDelts.effortIndex = 0.0; frontDelts.heatPowerW = 0.0
        core.mR = 1.0; core.mA = 0.0; core.mF = 0.0; core.activation = 0.0; core.deltaTempC = 0.0; core.effortIndex = 0.0; core.heatPowerW = 0.0

        repStartTime = 0.0
        repBottomTime = 0.0
        repPressStartTime = 0.0
        repPeakPressVel = 0.0
        repPeakPower = 0.0
        repMinElbow = 180.0
        repBaseShoulderY = 0f
        repMaxShoulderY = 0f
        hipSagThisRep = false

        reps.clear()
        vRefRep1 = 0.0
        cachedMetrics = PushUpMetrics()
    }

    fun processFrame(skeleton: PoseSkeleton): PushUpMetrics {
        val now = SystemClock.elapsedRealtime()
        if (lastTime == 0L) {
            lastTime = now
            return cachedMetrics
        }
        val dt = ((now - lastTime) / 1000.0).coerceIn(0.005, 0.200)
        lastTime = now
        val timeS = now / 1000.0

        // Step 1: Extract landmarks
        val ls = skeleton.shoulderLeft
        val rs = skeleton.shoulderRight
        val le = skeleton.elbowLeft
        val re = skeleton.elbowRight
        val lw = skeleton.wristLeft
        val rw = skeleton.wristRight
        val lh = skeleton.hipLeft
        val rh = skeleton.hipRight
        val la = skeleton.ankleLeft
        val ra = skeleton.ankleRight

        val leftArmValid = ls != null && le != null && lw != null
        val rightArmValid = rs != null && re != null && rw != null

        if (!leftArmValid && !rightArmValid) {
            return cachedMetrics
        }

        // Step 2: Midpoints
        val shMidY = when {
            ls != null && rs != null -> (ls.y + rs.y) / 2f
            ls != null -> ls.y
            else -> rs!!.y
        }
        val shMidX = when {
            ls != null && rs != null -> (ls.x + rs.x) / 2f
            ls != null -> ls.x
            else -> rs!!.x
        }
        val hipMidY = when {
            lh != null && rh != null -> (lh.y + rh.y) / 2f
            lh != null -> lh.y
            rh != null -> rh.y
            else -> null
        }
        val hipMidX = when {
            lh != null && rh != null -> (lh.x + rh.x) / 2f
            lh != null -> lh.x
            rh != null -> rh.x
            else -> null
        }
        val ankleMidY = when {
            la != null && ra != null -> (la.y + ra.y) / 2f
            la != null -> la.y
            ra != null -> ra.y
            else -> null
        }
        val ankleMidX = when {
            la != null && ra != null -> (la.x + ra.x) / 2f
            la != null -> la.x
            ra != null -> ra.x
            else -> null
        }

        // Step 3: Scale calibration
        if (hipMidY != null) {
            val measuredTorso = abs(hipMidY - shMidY)
            val expectedTorsoM = userHeightCm / 100.0 * 0.288
            if (measuredTorso in 0.08f..0.45f && expectedTorsoM > 0.1) {
                val instant = (measuredTorso / expectedTorsoM).toDouble()
                normUnitsPerM = (0.92 * normUnitsPerM + 0.08 * instant).coerceIn(0.20, 0.90)
            }
        }

        // Step 4: Elbow angle
        var currentElbowAngle = 170.0
        val angles = mutableListOf<Double>()
        if (leftArmValid) {
            angles.add(calculateAngle(ls!!.x, ls.y, le!!.x, le.y, lw!!.x, lw.y))
        }
        if (rightArmValid) {
            angles.add(calculateAngle(rs!!.x, rs.y, re!!.x, re.y, rw!!.x, rw.y))
        }
        if (angles.isNotEmpty()) {
            currentElbowAngle = angles.average()
        }

        // Step 5: Body alignment angle (Shoulder -> Hip -> Ankle)
        var bodyAlignmentAngle = 180.0
        if (ls != null && lh != null && la != null) {
            bodyAlignmentAngle = calculateAngle(ls.x, ls.y, lh.x, lh.y, la.x, la.y)
        } else if (rs != null && rh != null && ra != null) {
            bodyAlignmentAngle = calculateAngle(rs.x, rs.y, rh.x, rh.y, ra.x, ra.y)
        } else if (shMidX != 0f && hipMidX != null && hipMidY != null && ankleMidX != null && ankleMidY != null) {
            bodyAlignmentAngle = calculateAngle(shMidX, shMidY, hipMidX, hipMidY, ankleMidX, ankleMidY)
        }

        if (bodyAlignmentAngle < 165.0 && phase != "PLANK") {
            hipSagThisRep = true
        }

        // Step 6: Kinematics (Rolling window on shoulder Y)
        shoulderYHistory.add(shMidY)
        timestamps.add(now)
        if (shoulderYHistory.size > 10) {
            shoulderYHistory.removeAt(0)
            timestamps.removeAt(0)
        }

        if (shoulderYHistory.size >= 4) {
            val half = shoulderYHistory.size / 2
            val yPrevAvg = shoulderYHistory.take(half).average()
            val yCurrAvg = shoulderYHistory.takeLast(half).average()
            val dtStep = ((timestamps.last() - timestamps.first()) / 1000.0).coerceIn(0.02, 0.5)

            // In push-up: pressing UP means shMidY DECREASES in screen coordinates
            val deltaMeters = (yPrevAvg - yCurrAvg) / normUnitsPerM
            val rawVel = deltaMeters / dtStep
            prevSpeed = currentSpeed
            currentSpeed = 0.60 * currentSpeed + 0.40 * rawVel

            if (abs(currentSpeed) < 0.025) {
                currentSpeed = 0.0
            }
            currentAccel = (currentSpeed - prevSpeed) / dt
        }

        // Step 7: Force & Power
        val concentricVelocity = max(0.0, currentSpeed) // Positive = pressing up
        val forceN = liftedMassKg * (G + max(0.0, currentAccel))

        if (phase == "PRESS" && concentricVelocity > 0.03) {
            currentPower = forceN * concentricVelocity
            if (currentPower > repPeakPower) repPeakPower = currentPower
            if (currentPower > peakPowerSessionW) peakPowerSessionW = currentPower
            if (concentricVelocity > repPeakPressVel) repPeakPressVel = concentricVelocity
        } else {
            currentPower = 0.0
        }

        // Step 8: State Machine
        when (phase) {
            "PLANK" -> {
                if (currentElbowAngle < 150.0) {
                    phase = "LOWER"
                    repStartTime = timeS
                    repMinElbow = currentElbowAngle
                    repBaseShoulderY = shMidY
                    repMaxShoulderY = shMidY
                    repPeakPressVel = 0.0
                    repPeakPower = 0.0
                    hipSagThisRep = false
                }
            }
            "LOWER" -> {
                if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                if (shMidY > repMaxShoulderY) repMaxShoulderY = shMidY

                if (currentElbowAngle <= 95.0 || (currentElbowAngle <= 110.0 && currentSpeed >= -0.03)) {
                    phase = "BOTTOM"
                    repBottomTime = timeS
                } else if (currentSpeed > 0.12 && currentElbowAngle < 130.0) {
                    phase = "PRESS"
                    repBottomTime = timeS
                    repPressStartTime = timeS
                }
            }
            "BOTTOM" -> {
                if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                if (shMidY > repMaxShoulderY) repMaxShoulderY = shMidY

                if (currentSpeed > 0.06 || currentElbowAngle > 100.0) {
                    phase = "PRESS"
                    repPressStartTime = timeS
                }
            }
            "PRESS" -> {
                if (currentSpeed > repPeakPressVel) repPeakPressVel = currentSpeed

                if (currentElbowAngle >= 155.0) {
                    recordRep(timeS, currentElbowAngle)
                    phase = "PLANK"
                }
            }
        }

        // Step 9: Muscle model updates
        val pecsTarget = when (phase) {
            "PRESS" -> 1.0
            "BOTTOM" -> 0.7
            "LOWER" -> 0.5
            else -> 0.02
        }
        val tricepsTarget = when (phase) {
            "PRESS" -> 1.0
            "BOTTOM" -> 0.6
            "LOWER" -> 0.4
            else -> 0.02
        }
        val deltsTarget = when (phase) {
            "PRESS" -> 0.85
            "LOWER" -> 0.5
            else -> 0.02
        }
        val coreTarget = if (phase != "PLANK") 0.4 else 0.1

        updateMuscle(pecs, dt, pecsTarget, mass = 0.85, mvic = 100.0)
        updateMuscle(triceps, dt, tricepsTarget, mass = 0.55, mvic = 90.0)
        updateMuscle(frontDelts, dt, deltsTarget, mass = 0.45, mvic = 60.0)
        updateMuscle(core, dt, coreTarget, mass = 2.5, mvic = 30.0)

        // Step 10: Cumulative energy
        cumulativeWorkJ += max(0.0, currentPower) * dt
        cumulativeKcal = (cumulativeWorkJ / 0.22 + cumulativeWorkJ * 0.35 / 0.22) / 4184.0
        val frameHeatJ = (pecs.heatPowerW + triceps.heatPowerW + frontDelts.heatPowerW + core.heatPowerW) * dt
        cumulativeHeatKj += frameHeatJ / 1000.0

        // Step 11: Live VBT speed loss and telemetry
        val liveSpeedLoss = if (vRefRep1 > 0.05 && phase == "PRESS" && repPeakPressVel > 0.05) {
            max(0, ((1.0 - (repPeakPressVel / vRefRep1)) * 100.0).toInt()).coerceIn(0, 95)
        } else if (reps.isNotEmpty()) {
            reps.last().speedLossPct
        } else 0

        val pecsFatigue = min(99, repCount * 9 + (pecs.mF * 55).toInt())
        val tricepsFatigue = min(99, repCount * 8 + (triceps.mF * 50).toInt())

        cachedMetrics = PushUpMetrics(
            phase = phase,
            repCount = repCount,
            speedLossPct = liveSpeedLoss,
            peakPowerW = peakPowerSessionW.toInt(),
            currentSpeedMps = currentSpeed,
            elbowAngle = currentElbowAngle.toInt(),
            bodyAlignmentAngle = bodyAlignmentAngle.toInt(),
            totalKcal = cumulativeKcal,
            totalHeatKj = cumulativeHeatKj,
            pecsTempRise = pecs.deltaTempC,
            pecsFatiguedPct = pecsFatigue,
            tricepsFatiguedPct = tricepsFatigue,
            lastRep = reps.lastOrNull(),
            pecsEffort = pecs.activation,
            tricepsEffort = triceps.activation,
            frontDeltsEffort = frontDelts.activation,
            coreEffort = core.activation,
            shoulderYHistory = shoulderYHistory.toList(),
            vRefRep1 = vRefRep1,
            currentPowerW = currentPower.toInt()
        )

        return cachedMetrics
    }

    private fun recordRep(timeS: Double, currentElbowAngle: Double) {
        repCount++
        val durLowering = max(0.3, (if (repBottomTime > 0) repBottomTime else timeS) - repStartTime)
        val durHold = max(0.0, repPressStartTime - repBottomTime)
        val durPress = max(0.3, timeS - (if (repPressStartTime > 0) repPressStartTime else repBottomTime))

        val actualPeakVel = max(0.12, repPeakPressVel)

        if (vRefRep1 == 0.0 || repCount == 1) {
            vRefRep1 = actualPeakVel
        }

        val vl = if (vRefRep1 > 0.05) {
            max(0, ((1.0 - (actualPeakVel / vRefRep1)) * 100.0).toInt()).coerceIn(0, 95)
        } else 0

        // Range of motion in metres (shoulder travel distance)
        val romNorm = abs(repMaxShoulderY - repBaseShoulderY)
        val romM = (romNorm / normUnitsPerM).coerceIn(0.10, 0.45)
        val workJ = liftedMassKg * G * romM
        cumulativeWorkJ += workJ
        val kcal = (workJ / 0.22 + workJ * 0.35 / 0.22) / 4184.0
        cumulativeKcal += kcal

        val lockout = currentElbowAngle >= 155.0
        val isDeep = repMinElbow <= 95.0

        val verdict = when {
            isDeep && lockout && !hipSagThisRep -> "PERFECT FORM"
            isDeep && lockout -> "FULL REP"
            isDeep && hipSagThisRep -> "CORE ISSUE"
            hipSagThisRep -> "HIP SAG"
            lockout && !isDeep -> "TOO SHALLOW"
            else -> "CONTROLLED REP"
        }

        val finalPeakPower = if (repPeakPower > 0.0) repPeakPower else (liftedMassKg * G * actualPeakVel)

        // Pecs temperature rise per completed rep
        pecs.deltaTempC = min(2.5, pecs.deltaTempC + 0.15)

        reps.add(
            PushUpRepRecord(
                repNum = repCount,
                durationLowering = durLowering,
                durationHold = durHold,
                durationPress = durPress,
                peakPressVelocity = actualPeakVel,
                peakPower = finalPeakPower,
                energyKcal = kcal,
                speedLossPct = vl,
                minElbowAngle = repMinElbow,
                hipSagDetected = hipSagThisRep,
                fullLockout = lockout,
                formVerdict = verdict
            )
        )

        // Reset rep state
        repBottomTime = 0.0
        repPressStartTime = 0.0
        repPeakPressVel = 0.0
        repPeakPower = 0.0
        hipSagThisRep = false
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
