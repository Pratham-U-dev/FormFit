package com.example.cv

import kotlin.math.abs
import kotlin.math.atan2

data class LandmarkPoint(val x: Float, val y: Float, val confidence: Float)

data class PoseSkeleton(
    val shoulderLeft: LandmarkPoint?,
    val shoulderRight: LandmarkPoint?,
    val elbowLeft: LandmarkPoint?,
    val elbowRight: LandmarkPoint?,
    val wristLeft: LandmarkPoint?,
    val wristRight: LandmarkPoint?,
    val hipLeft: LandmarkPoint?,
    val hipRight: LandmarkPoint?,
    val kneeLeft: LandmarkPoint?,
    val kneeRight: LandmarkPoint?,
    val ankleLeft: LandmarkPoint?,
    val ankleRight: LandmarkPoint?
)

object PoseGeometry {

    fun calculateAngle(
        p1: LandmarkPoint,
        p2: LandmarkPoint, // Mid point (vertex)
        p3: LandmarkPoint
    ): Double {
        val angleRad = atan2(p3.y - p2.y, p3.x - p2.x) - atan2(p1.y - p2.y, p1.x - p2.x)
        var angleDeg = abs(Math.toDegrees(angleRad.toDouble()))
        if (angleDeg > 180.0) {
            angleDeg = 360.0 - angleDeg
        }
        return angleDeg
    }
}

class ExerciseFormEvaluator {
    
    // State machine trackers
    private var squatState = "UP" // UP, DOWN
    private var minSquatAngle = 180.0
    private var squatDeepEnough = false

    private var pushupState = "UP" // UP, DOWN
    private var minPushupAngle = 180.0
    private var pushupDeepEnough = false

    private var lungeState = "UP" // UP, DOWN
    private var minLungeAngle = 180.0
    private var lungeDeepEnough = false

    // Pull-up state machine trackers
    private var pullupState = "HANG" // HANG, PULL, TOP, LOWER
    private var minPullupElbowAngle = 180.0
    private var pullupChinCleared = false
    private var pullupLockoutAtBottom = true

    fun reset() {
        squatState = "UP"
        minSquatAngle = 180.0
        squatDeepEnough = false

        pushupState = "UP"
        minPushupAngle = 180.0
        pushupDeepEnough = false

        lungeState = "UP"
        minLungeAngle = 180.0
        lungeDeepEnough = false

        pullupState = "HANG"
        minPullupElbowAngle = 180.0
        pullupChinCleared = false
        pullupLockoutAtBottom = true
    }

    fun evaluateSquat(skeleton: PoseSkeleton): EvaluationResult {
        val hip = skeleton.hipLeft ?: skeleton.hipRight ?: return EvaluationResult.idle("Stand fully in frame")
        val knee = skeleton.kneeLeft ?: skeleton.kneeRight ?: return EvaluationResult.idle("Position hips and knees in frame")
        val ankle = skeleton.ankleLeft ?: skeleton.ankleRight ?: return EvaluationResult.idle("Ensure ankles are visible")

        val kneeAngle = PoseGeometry.calculateAngle(hip, knee, ankle)
        var feedback = "Stand straight, then squat down"
        var mistake: String? = null
        var isRepCompleted = false
        var score = 100

        if (kneeAngle < minSquatAngle) {
            minSquatAngle = kneeAngle
        }

        // Deep squat check
        if (kneeAngle <= 100.0) {
            squatDeepEnough = true
        }

        // Detect knees collapsing inward if both knees are visible
        if (skeleton.kneeLeft != null && skeleton.kneeRight != null &&
            skeleton.ankleLeft != null && skeleton.ankleRight != null
        ) {
            val kneeDist = abs(skeleton.kneeLeft.x - skeleton.kneeRight.x)
            val ankleDist = abs(skeleton.ankleLeft.x - skeleton.ankleRight.x)
            // If knees collapse closer than ankles by a critical ratio
            if (kneeDist < ankleDist * 0.75) {
                mistake = "Knees collapsing inward"
                feedback = "Keep knees aligned with your toes"
                score = 65
            }
        }

        if (squatState == "UP") {
            if (kneeAngle < 130.0) {
                squatState = "DOWN"
                minSquatAngle = kneeAngle
                squatDeepEnough = false
                feedback = "Going down... keep chest high!"
            }
        } else if (squatState == "DOWN") {
            if (kneeAngle > 150.0) {
                // User stood back up! Rep complete
                squatState = "UP"
                isRepCompleted = true
                
                if (!squatDeepEnough && minSquatAngle > 110.0) {
                    mistake = "Squat not deep enough"
                    feedback = "Squat deeper! Thighs parallel to floor."
                    score = 70
                } else {
                    feedback = "Excellent squat depth!"
                    score = if (mistake != null) 75 else 95
                }
                minSquatAngle = 180.0
            } else {
                feedback = if (squatDeepEnough) "Good depth! Now push up." else "Go lower... keep hips back."
            }
        }

        return EvaluationResult(
            score = score,
            mistake = mistake,
            feedback = feedback,
            isRepCompleted = isRepCompleted,
            angleValue = kneeAngle
        )
    }

    fun evaluatePushup(skeleton: PoseSkeleton): EvaluationResult {
        val shoulder = skeleton.shoulderLeft ?: skeleton.shoulderRight ?: return EvaluationResult.idle("Ensure shoulders are in frame")
        val elbow = skeleton.elbowLeft ?: skeleton.elbowRight ?: return EvaluationResult.idle("Position elbows in frame")
        val wrist = skeleton.wristLeft ?: skeleton.wristRight ?: return EvaluationResult.idle("Ensure wrists are visible")

        val elbowAngle = PoseGeometry.calculateAngle(shoulder, elbow, wrist)
        var feedback = "Hold plank, then lower chest"
        var mistake: String? = null
        var isRepCompleted = false
        var score = 100

        if (elbowAngle < minPushupAngle) {
            minPushupAngle = elbowAngle
        }

        if (elbowAngle <= 95.0) {
            pushupDeepEnough = true
        }

        // Core / Hip Sagging Check
        if (skeleton.shoulderLeft != null && skeleton.hipLeft != null && skeleton.ankleLeft != null) {
            val bodyAlignment = PoseGeometry.calculateAngle(skeleton.shoulderLeft, skeleton.hipLeft, skeleton.ankleLeft)
            if (bodyAlignment < 165.0) {
                mistake = "Hip sagging"
                feedback = "Engage your core! Lift your hips."
                score = 60
            }
        }

        if (pushupState == "UP") {
            if (elbowAngle < 130.0) {
                pushupState = "DOWN"
                minPushupAngle = elbowAngle
                pushupDeepEnough = false
                feedback = "Lowering... keep body straight!"
            }
        } else if (pushupState == "DOWN") {
            if (elbowAngle > 150.0) {
                pushupState = "UP"
                isRepCompleted = true
                
                if (!pushupDeepEnough && minPushupAngle > 110.0) {
                    mistake = "Push-up not deep enough"
                    feedback = "Lower more! Aim for 90-degree elbows."
                    score = 70
                } else {
                    feedback = "Great push-up!"
                    score = if (mistake != null) 70 else 98
                }
                minPushupAngle = 180.0
            } else {
                feedback = if (pushupDeepEnough) "Good depth! Push up." else "Go lower... chest closer to floor."
            }
        }

        return EvaluationResult(
            score = score,
            mistake = mistake,
            feedback = feedback,
            isRepCompleted = isRepCompleted,
            angleValue = elbowAngle
        )
    }

    fun evaluateLunge(skeleton: PoseSkeleton): EvaluationResult {
        val hip = skeleton.hipLeft ?: skeleton.hipRight ?: return EvaluationResult.idle("Hips must be visible")
        val knee = skeleton.kneeLeft ?: skeleton.kneeRight ?: return EvaluationResult.idle("Knees must be visible")
        val ankle = skeleton.ankleLeft ?: skeleton.ankleRight ?: return EvaluationResult.idle("Ankles must be visible")

        val kneeAngle = PoseGeometry.calculateAngle(hip, knee, ankle)
        var feedback = "Step forward and lower hips"
        var mistake: String? = null
        var isRepCompleted = false
        var score = 100

        if (kneeAngle < minLungeAngle) {
            minLungeAngle = kneeAngle
        }

        if (kneeAngle <= 95.0) {
            lungeDeepEnough = true
        }

        // Check front knee over toes alignment (using simple X coordinate comparison)
        if (skeleton.kneeLeft != null && skeleton.ankleLeft != null) {
            // Front knee should ideally not extend too far horizontally past ankle
            if (abs(skeleton.kneeLeft.x - skeleton.ankleLeft.x) > 0.15) {
                mistake = "Incorrect lunge knee alignment"
                feedback = "Don't push front knee past your toes!"
                score = 65
            }
        }

        if (lungeState == "UP") {
            if (kneeAngle < 135.0) {
                lungeState = "DOWN"
                minLungeAngle = kneeAngle
                lungeDeepEnough = false
                feedback = "Lunge down... keep balance!"
            }
        } else if (lungeState == "DOWN") {
            if (kneeAngle > 155.0) {
                lungeState = "UP"
                isRepCompleted = true
                
                if (!lungeDeepEnough) {
                    mistake = "Incorrect lunge knee alignment"
                    feedback = "Step wider and lower hips further."
                    score = 70
                } else {
                    feedback = "Perfect lunge rep!"
                    score = if (mistake != null) 70 else 96
                }
                minLungeAngle = 180.0
            } else {
                feedback = "Step up to complete rep."
            }
        }

        return EvaluationResult(
            score = score,
            mistake = mistake,
            feedback = feedback,
            isRepCompleted = isRepCompleted,
            angleValue = kneeAngle
        )
    }

    fun evaluatePlank(skeleton: PoseSkeleton): EvaluationResult {
        val shoulder = skeleton.shoulderLeft ?: skeleton.shoulderRight ?: return EvaluationResult.idle("Align shoulders in frame")
        val hip = skeleton.hipLeft ?: skeleton.hipRight ?: return EvaluationResult.idle("Align hips in frame")
        val ankle = skeleton.ankleLeft ?: skeleton.ankleRight ?: return EvaluationResult.idle("Align ankles in frame")

        val hipAngle = PoseGeometry.calculateAngle(shoulder, hip, ankle)
        var feedback = "Hold body in a straight line!"
        var mistake: String? = null
        var score = 100

        // In a perfect plank, hip angle should be very close to straight (170° to 190°)
        if (hipAngle < 165.0) {
            mistake = "Hip sagging during plank"
            feedback = "Squeeze your core! Lift hips."
            score = 65
        } else if (hipAngle > 195.0) {
            mistake = "Hip sagging during plank" // Keep simple mistake categories for PRD
            feedback = "Lower your hips to straight level."
            score = 65
        } else {
            feedback = "Plank posture is perfectly straight!"
        }

        return EvaluationResult(
            score = score,
            mistake = mistake,
            feedback = feedback,
            isRepCompleted = false,
            angleValue = hipAngle
        )
    }

    fun evaluatePullup(skeleton: PoseSkeleton): EvaluationResult {
        val shoulder = skeleton.shoulderLeft ?: skeleton.shoulderRight ?: return EvaluationResult.idle("Position shoulders in frame")
        val elbow = skeleton.elbowLeft ?: skeleton.elbowRight ?: return EvaluationResult.idle("Position elbows in frame")
        val wrist = skeleton.wristLeft ?: skeleton.wristRight ?: return EvaluationResult.idle("Hands must grip the bar")

        val leftElbowAngle = if (skeleton.shoulderLeft != null && skeleton.elbowLeft != null && skeleton.wristLeft != null) {
            PoseGeometry.calculateAngle(skeleton.shoulderLeft, skeleton.elbowLeft, skeleton.wristLeft)
        } else null

        val rightElbowAngle = if (skeleton.shoulderRight != null && skeleton.elbowRight != null && skeleton.wristRight != null) {
            PoseGeometry.calculateAngle(skeleton.shoulderRight, skeleton.elbowRight, skeleton.wristRight)
        } else null

        val avgElbowAngle = when {
            leftElbowAngle != null && rightElbowAngle != null -> (leftElbowAngle + rightElbowAngle) / 2.0
            leftElbowAngle != null -> leftElbowAngle
            rightElbowAngle != null -> rightElbowAngle
            else -> PoseGeometry.calculateAngle(shoulder, elbow, wrist)
        }

        var feedback = "Dead hang... pull chin over bar!"
        var mistake: String? = null
        var isRepCompleted = false
        var score = 100

        if (avgElbowAngle < minPullupElbowAngle) {
            minPullupElbowAngle = avgElbowAngle
        }

        // Check if chin reached bar level (elbow angle < 85° or shoulders near wrist height)
        val avgWristY = if (skeleton.wristLeft != null && skeleton.wristRight != null) {
            (skeleton.wristLeft.y + skeleton.wristRight.y) / 2f
        } else wrist.y
        val avgShoulderY = if (skeleton.shoulderLeft != null && skeleton.shoulderRight != null) {
            (skeleton.shoulderLeft.y + skeleton.shoulderRight.y) / 2f
        } else shoulder.y

        // When shoulders rise within close proximity to wrists, chin is over bar
        val chinClearedThreshold = (avgShoulderY - avgWristY) < 0.18f || avgElbowAngle <= 80.0
        if (chinClearedThreshold) {
            pullupChinCleared = true
        }

        // Arm asymmetry check
        if (leftElbowAngle != null && rightElbowAngle != null) {
            val asym = abs(leftElbowAngle - rightElbowAngle)
            if (asym > 25.0) {
                mistake = "Uneven pull (arm asymmetry)"
                feedback = "Pull evenly with both arms!"
                score = 65
            }
        }

        // Excessive swing / kipping check using horizontal hip displacement vs shoulder
        val hip = skeleton.hipLeft ?: skeleton.hipRight
        if (hip != null) {
            val horizontalSway = abs(hip.x - shoulder.x)
            if (horizontalSway > 0.14f) {
                mistake = "Excessive body swing / kip"
                feedback = "Strict pull-up: avoid swinging legs!"
                score = 60
            }
        }

        when (pullupState) {
            "HANG" -> {
                // User starts pulling up when elbows flex below 145°
                if (avgElbowAngle < 145.0) {
                    pullupState = "PULL"
                    minPullupElbowAngle = avgElbowAngle
                    pullupChinCleared = false
                    pullupLockoutAtBottom = true
                    feedback = "Pulling up! Drive elbows down."
                } else {
                    feedback = "Hanging from bar. Ready to pull!"
                }
            }
            "PULL" -> {
                if (avgElbowAngle < 85.0 || pullupChinCleared) {
                    pullupState = "TOP"
                    feedback = "Chin over bar! Hold briefly."
                } else {
                    feedback = "Drive higher! Get chin over the bar."
                }
            }
            "TOP" -> {
                // Lowering down
                if (avgElbowAngle > 100.0) {
                    pullupState = "LOWER"
                    feedback = "Controlled descent... full extension."
                } else {
                    feedback = "Chin cleared! Now lower smoothly."
                }
            }
            "LOWER" -> {
                // Bottom lockout check: elbows must reach >= 150°
                if (avgElbowAngle >= 150.0) {
                    pullupState = "HANG"
                    isRepCompleted = true

                    if (!pullupChinCleared && minPullupElbowAngle > 88.0) {
                        mistake = "Chin not over bar"
                        feedback = "Pull higher! Chin must clear the bar."
                        score = 70
                    } else {
                        feedback = "Full lockout & chin cleared! Excellent rep."
                        score = if (mistake != null) 72 else 98
                    }
                    minPullupElbowAngle = 180.0
                } else {
                    feedback = "Lower all the way to full dead hang lockout."
                }
            }
        }

        return EvaluationResult(
            score = score,
            mistake = mistake,
            feedback = feedback,
            isRepCompleted = isRepCompleted,
            angleValue = avgElbowAngle
        )
    }
}

data class EvaluationResult(
    val score: Int,
    val mistake: String?,
    val feedback: String,
    val isRepCompleted: Boolean,
    val angleValue: Double
) {
    companion object {
        fun idle(message: String) = EvaluationResult(
            score = 100,
            mistake = null,
            feedback = message,
            isRepCompleted = false,
            angleValue = 180.0
        )
    }
}
