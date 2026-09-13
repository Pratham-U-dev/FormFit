package com.example.cv

import android.os.SystemClock
import kotlin.math.*

data class MuscleState(
    var mR: Double = 1.0,
    var mA: Double = 0.0,
    var mF: Double = 0.0,
    var activation: Double = 0.0,
    var deltaTempC: Double = 0.0,
    var effortIndex: Double = 0.0, // 0.0 to 1.0
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
    val chinVerdict: String
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
    val barY: Float? = null
)

class PullUpBiomechanics {
    private var lastTime = 0L
    private val massKg = 79.0
    private val liftedMassKg = massKg * 0.956
    private val G = 9.81
    
    private var phase = "HANG"
    private var repCount = 0
    private var chinAtBarCount = 0
    private var peakPowerSessionW = 0.0
    private var cumulativeWorkJ = 0.0
    private var cumulativeHeatJ = 0.0
    private var cumulativeKcal = 0.0
    
    private val shoulderYHistory = mutableListOf<Float>()
    private val timestamps = mutableListOf<Long>()
    
    private var currentSpeed = 0.0
    private var currentAccel = 0.0
    private var currentPower = 0.0
    
    private var barY: Float? = null
    private var pxPerM = 1000.0
    
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
    private var repHipXHistory = mutableListOf<Float>()
    private var currentChinClearanceCm = 0.0
    
    private val reps = mutableListOf<PullUpRepRecord>()
    private var vRefRep1 = 0.0
    
    fun processFrame(skeleton: PoseSkeleton): PullUpMetrics {
        val now = SystemClock.elapsedRealtime()
        if (lastTime == 0L) {
            lastTime = now
            return emptyMetrics()
        }
        val dt = (now - lastTime) / 1000.0
        lastTime = now
        
        val lWrist = skeleton.wristLeft
        val rWrist = skeleton.wristRight
        val lShoulder = skeleton.shoulderLeft
        val rShoulder = skeleton.shoulderRight
        val lElbow = skeleton.elbowLeft
        val rElbow = skeleton.elbowRight
        val lHip = skeleton.hipLeft
        val rHip = skeleton.hipRight
        
        if (lWrist == null || rWrist == null || lShoulder == null || rShoulder == null || lElbow == null || rElbow == null || lHip == null || rHip == null) {
            return emptyMetrics()
        }
        
        // Calibration
        val shMidY = (lShoulder.y + rShoulder.y) / 2f
        val wrMidY = (lWrist.y + rWrist.y) / 2f
        if (wrMidY < shMidY && barY == null) {
            barY = wrMidY
            val expectedArmM = 1.88f * 0.332f
            val measuredArmY = abs(shMidY - wrMidY)
            if (measuredArmY > 0.1f) {
                pxPerM = (measuredArmY / expectedArmM).toDouble()
            }
        }
        
        // Angles
        val elLeft = calculateAngle(lShoulder.x, lShoulder.y, lElbow.x, lElbow.y, lWrist.x, lWrist.y)
        val elRight = calculateAngle(rShoulder.x, rShoulder.y, rElbow.x, rElbow.y, rWrist.x, rWrist.y)
        val currentElbowAngle = (elLeft + elRight) / 2.0
        
        // Kinematics
        val currShY = (lShoulder.y + rShoulder.y) / 2f
        shoulderYHistory.add(currShY)
        timestamps.add(now)
        if (shoulderYHistory.size > 30) {
            shoulderYHistory.removeAt(0)
            timestamps.removeAt(0)
        }
        
        if (shoulderYHistory.size >= 3) {
            val yCurr = shoulderYHistory.takeLast(3).average().toFloat()
            val yPrev = shoulderYHistory.dropLast(3).takeLast(3).average().toFloat().takeIf { !it.isNaN() } ?: shoulderYHistory.first()
            val dtStep = (now - timestamps[max(0, timestamps.size - 6)]) / 1000.0
            if (dtStep > 0) {
                val deltaM = (yPrev - yCurr) / pxPerM
                val newSpeed = deltaM / dtStep
                currentAccel = (newSpeed - currentSpeed) / dt
                currentSpeed = newSpeed
            }
        }
        
        val forceN = liftedMassKg * (G + max(0.0, currentAccel))
        currentPower = forceN * max(0.0, currentSpeed)
        if (currentPower > peakPowerSessionW) peakPowerSessionW = currentPower
        
        // Chin clearance
        if (barY != null) {
            val chinY = shMidY - (0.15 * pxPerM).toFloat() 
            val deltaY = barY!! - chinY
            currentChinClearanceCm = (deltaY / pxPerM) * 100.0 + (8.0 * 0.35)
        }
        
        // State Machine
        val handsOverhead = lWrist.y < lShoulder.y && rWrist.y < rShoulder.y
        val hipMidX = (lHip.x + rHip.x) / 2f
        val timeS = now / 1000.0
        
        if (handsOverhead) {
            if (phase == "SETUP" || phase == "DONE") phase = "HANG"
            
            when (phase) {
                "HANG" -> {
                    if (currentSpeed > 0.08 && currentElbowAngle < 165) {
                        phase = "PULL"
                        repStartTime = timeS
                        repMinElbow = currentElbowAngle
                        repPeakConcVel = currentSpeed
                        repBaseShoulderY = currShY
                        repMinShoulderY = currShY
                        repHipXHistory.clear()
                    }
                }
                "PULL" -> {
                    repHipXHistory.add(hipMidX)
                    if (currentSpeed > repPeakConcVel) repPeakConcVel = currentSpeed
                    if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                    if (currShY < repMinShoulderY) repMinShoulderY = currShY
                    
                    if (currentSpeed <= 0.05 && currentElbowAngle < 100) {
                        phase = "HOLD"
                        repTopTime = timeS
                    } else if (currentSpeed < -0.10) {
                        phase = "LOWER"
                        repTopTime = timeS
                        repEccStartTime = timeS
                    }
                }
                "HOLD" -> {
                    repHipXHistory.add(hipMidX)
                    if (currentElbowAngle < repMinElbow) repMinElbow = currentElbowAngle
                    if (currShY < repMinShoulderY) repMinShoulderY = currShY
                    
                    if (currentSpeed < -0.06 || currentElbowAngle > 110) {
                        phase = "LOWER"
                        repEccStartTime = timeS
                    }
                }
                "LOWER" -> {
                    if (currentElbowAngle >= 150 || (currentSpeed >= -0.04 && currentElbowAngle >= 140)) {
                        recordRep(timeS, currentElbowAngle)
                        phase = "HANG"
                    }
                }
            }
        } else {
            if (phase != "SETUP") phase = "DONE"
        }
        
        // Muscles integration
        updateMuscle(lats, dt, if (phase == "PULL") 1.2 else if (phase == "HOLD") 1.0 else if (phase == "LOWER") 0.6 else 0.05, 0.734, 124.0)
        updateMuscle(biceps, dt, if (phase == "PULL") 1.0 else if (phase == "LOWER") 0.5 else 0.05, 0.402, 78.0)
        updateMuscle(forearms, dt, if (handsOverhead) 0.6 else 0.05, 0.666, 60.0)
        updateMuscle(legs, dt, 0.08, 5.5, 10.0)
        
        cumulativeWorkJ += max(0.0, currentPower) * dt
        cumulativeKcal = (cumulativeWorkJ + cumulativeHeatJ) * (1.0 / 4184.0)
        
        val vl = if (vRefRep1 > 0) max(0.0, (1.0 - (currentSpeed / vRefRep1)) * 100.0).toInt() else 0
        
        return PullUpMetrics(
            phase = phase,
            repCount = repCount,
            chinAtBarCount = chinAtBarCount,
            speedLossPct = reps.lastOrNull()?.speedLossPct ?: vl,
            peakPowerW = peakPowerSessionW.toInt(),
            currentSpeedMps = currentSpeed,
            elbowAngle = currentElbowAngle.toInt(),
            totalKcal = cumulativeKcal,
            totalHeatKj = cumulativeHeatJ / 1000.0,
            latsTempRise = lats.deltaTempC,
            latsFatiguedPct = (lats.mF * 100).toInt(),
            bicepsFatiguedPct = (biceps.mF * 100).toInt(),
            lastRep = reps.lastOrNull(),
            latsEffort = lats.effortIndex,
            bicepsEffort = biceps.effortIndex,
            forearmsEffort = forearms.effortIndex,
            legsEffort = legs.effortIndex,
            shoulderYHistory = shoulderYHistory.toList(),
            barY = barY
        )
    }
    
    private fun recordRep(timeS: Double, currentElbowAngle: Double) {
        repCount++
        val durConc = max(0.2, (if (repTopTime > 0) repTopTime else timeS) - repStartTime)
        val durHold = max(0.0, repEccStartTime - repTopTime)
        val durEcc = max(0.2, timeS - (if (repEccStartTime > 0) repEccStartTime else repTopTime))
        
        if (vRefRep1 == 0.0) vRefRep1 = repPeakConcVel
        val vl = if (vRefRep1 > 0) max(0.0, (1.0 - (repPeakConcVel / vRefRep1)) * 100.0).toInt() else 0
        
        val romCm = abs(repBaseShoulderY - repMinShoulderY) / pxPerM * 100.0
        val workJ = liftedMassKg * G * (romCm / 100.0)
        val kcal = (workJ / 0.22 + workJ * 0.35 / 0.22) / 4184.0
        
        val swayCm = if (repHipXHistory.isNotEmpty()) (repHipXHistory.maxOrNull()!! - repHipXHistory.minOrNull()!!) / pxPerM * 100.0 else 4.0
        val lockout = currentElbowAngle >= 148.0
        
        val verdict = if (currentChinClearanceCm > 3.0) "CHIN ABOVE BAR" else if (currentChinClearanceCm >= -3.0) "~ CHIN AT BAR" else "CHIN SHORT"
        if (currentChinClearanceCm >= -3.0) chinAtBarCount++
        
        reps.add(PullUpRepRecord(
            repNum = repCount,
            durationConcentric = durConc,
            durationHold = durHold,
            durationEccentric = durEcc,
            peakVelocity = repPeakConcVel,
            peakPower = liftedMassKg * (G + max(0.0, currentAccel)) * repPeakConcVel,
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
        val tau = if (targetAct > state.activation) 0.015 else 0.050
        state.activation += (targetAct - state.activation) * (dt / tau)
        state.activation = state.activation.coerceIn(0.0, 1.5)
        
        val fRate = 0.0182
        val rRate = 0.00168
        val rEff = if (state.activation < 0.05) 15.0 else 1.0
        
        var cCtrl = 10.0 * (min(1.0, state.activation) - state.mA)
        if (cCtrl > 0) cCtrl = min(cCtrl, state.mR / dt)
        else cCtrl = max(cCtrl, -state.mA / dt)
        
        val dMa = (cCtrl - fRate * state.mA) * dt
        val dMf = (fRate * state.mA - rRate * rEff * state.mF) * dt
        
        state.mA = (state.mA + dMa).coerceIn(0.0, 1.0)
        state.mF = (state.mF + dMf).coerceIn(0.0, 1.0)
        state.mR = (1.0 - state.mA - state.mF).coerceIn(0.0, 1.0)
        
        var heatW = state.activation * mass * 45.0
        if (phase == "PULL") heatW += currentPower * ((1.0/0.22) - 1.0) * (mvic/400.0)
        else if (phase == "LOWER") heatW += abs(currentPower) * (0.35/0.22) * (mvic/400.0)
        
        state.heatPowerW = max(0.0, heatW)
        cumulativeHeatJ += state.heatPowerW * dt
        
        val kRemoval = 42.0 * mass
        val dTemp = (state.heatPowerW - kRemoval * state.deltaTempC) / (3600.0 * mass) * dt
        state.deltaTempC = max(0.0, state.deltaTempC + dTemp)
        
        state.effortIndex = min(1.0, 0.90 * (state.mA + state.mF) + 0.20 * (state.deltaTempC / 1.5))
    }
    
    private fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val rad = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        var deg = abs(Math.toDegrees(rad.toDouble()))
        if (deg > 180.0) deg = 360.0 - deg
        return deg
    }
    
    private fun emptyMetrics() = PullUpMetrics()
}
