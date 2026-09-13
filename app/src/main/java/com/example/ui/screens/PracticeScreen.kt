package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.cv.EvaluationResult
import com.example.cv.ExerciseFormEvaluator
import com.example.cv.LandmarkPoint
import com.example.cv.PoseSkeleton
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCard
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PracticeScreen(
    viewModel: WorkoutViewModel,
    onWorkoutFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exerciseType by viewModel.currentExercise.collectAsState()
    val repCount by viewModel.repCount.collectAsState()
    val timerSeconds by viewModel.sessionSeconds.collectAsState()
    val feedback by viewModel.currentFeedback.collectAsState()
    val formScore by viewModel.currentScore.collectAsState()
    val isVirtual by viewModel.isVirtualCoachMode.collectAsState()
    val mistakes by viewModel.mistakesList.collectAsState()
    val pullUpMetrics by viewModel.pullUpMetrics.collectAsState()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP CONTROL HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRAINING: ${exerciseType.uppercase()}",
                    color = DuoInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (isVirtual) "Virtual practice sandbox" else "Real-time pose analysis",
                    color = DuoInkMuted,
                    fontSize = 12.sp
                )
            }

            // Mode Selector Toggle
            Row(
                modifier = Modifier
                    .background(DuoSurface1, shape = RoundedCornerShape(12.dp))
                    .border(2.dp, DuoBorder, shape = RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setVirtualCoachMode(true) },
                    modifier = Modifier
                        .background(
                            if (isVirtual) DuoBlue else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = "Virtual Mode",
                        tint = if (isVirtual) Color.White else DuoInkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        viewModel.setVirtualCoachMode(false)
                        if (!cameraPermissionState.status.isGranted) {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (!isVirtual) DuoBlue else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Mode",
                        tint = if (!isVirtual) Color.White else DuoInkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // WORKOUT STATS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Reps Badges
            DuoCard(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                backgroundColor = DuoSurface1
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (exerciseType == "Plank") "HOLD TIME" else "REPETITIONS",
                        color = DuoInkMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (exerciseType == "Plank") "${repCount}s" else "$repCount",
                        color = DuoYellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Timer Badge
            DuoCard(
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                backgroundColor = DuoSurface1
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DURATION",
                        color = DuoInkMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = DuoBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Live Score Gauge
            DuoCard(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                backgroundColor = if (formScore < 80) Color(0xFFFFEBEE) else DuoSurface1,
                borderColor = if (formScore < 80) DuoRed else DuoBorder
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FORM SCORE",
                        color = if (formScore < 80) DuoRed else DuoInkMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$formScore%",
                        color = if (formScore < 80) DuoRed else DuoGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // CENTRAL FEEDBACK SPEECH BUBBLE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (formScore < 80) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    2.dp,
                    if (formScore < 80) DuoRed else DuoGreen,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
                .padding(bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = feedback,
                color = if (formScore < 80) DuoRed else DuoInk,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // VIEWPORT (CAMERA OR SIMULATOR)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DuoSurface1, shape = RoundedCornerShape(20.dp))
                .border(3.dp, DuoBorder, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isVirtual) {
                // RENDER ANIMATED COACH CANVAS
                VirtualCoachCanvas(exerciseType = exerciseType, timerSeconds = timerSeconds, pullUpMetrics = pullUpMetrics)
            } else {
                // RENDER CAMERAX PREVIEW OR PERMISSION REQUEST
                if (cameraPermissionState.status.isGranted) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraWithPoseOverlay(
                            exerciseType = exerciseType,
                            pullUpMetrics = pullUpMetrics,
                            onFrameAnalysis = { score, mistake, fb, rep, skeleton ->
                                viewModel.processCameraFrameAnalysis(score, mistake, fb, rep, skeleton)
                            }
                        )
                        if (exerciseType == "Pull-up") {
                            PullUpDashboardHUD(pullUpMetrics)
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Camera Permission Required",
                            fontWeight = FontWeight.Bold,
                            color = DuoInk,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "FormFit needs camera access to perform live human pose estimation and analyze your workouts.",
                            color = DuoInkMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DuoButton(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            backgroundColor = DuoBlue,
                            shadowColor = DuoBlueDark,
                            modifier = Modifier.height(44.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("GRANT CAMERA ACCESS", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FINISH WORKOUT BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DuoButton(
                onClick = { viewModel.abandonWorkout(); onWorkoutFinished() },
                backgroundColor = Color.White,
                shadowColor = DuoBorder,
                textColor = DuoInkMuted,
                modifier = Modifier.weight(0.4f).height(50.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    "ABANDON",
                    color = DuoInkMuted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            DuoButton(
                onClick = { viewModel.stopAndSaveWorkout(); onWorkoutFinished() },
                backgroundColor = DuoGreen,
                shadowColor = DuoGreenDark,
                modifier = Modifier.weight(0.6f).height(50.dp),
                testTag = "finish_workout_button",
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    "FINISH WORKOUT",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VirtualCoachCanvas(
    exerciseType: String,
    timerSeconds: Int,
    pullUpMetrics: com.example.cv.PullUpMetrics? = null
) {
    // We animate a value between 0f and 1f representing mechanical squat extension/flexion
    var pulse by remember { mutableStateOf(0f) }
    var ascending by remember { mutableStateOf(false) }

    LaunchedEffect(timerSeconds) {
        // Continuous updates mimicking exercise pacing
        while (true) {
            delay(50)
            if (ascending) {
                pulse -= 0.04f
                if (pulse <= 0f) {
                    pulse = 0f
                    ascending = false
                }
            } else {
                pulse += 0.04f
                if (pulse >= 1f) {
                    pulse = 1f
                    ascending = true
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Draw ambient floor grid
        drawLine(
            color = DuoBorder,
            start = Offset(centerX - 120f, centerY + 220f),
            end = Offset(centerX + 120f, centerY + 220f),
            strokeWidth = 6f
        )

        val bonePaintColor = DuoBlue
        val boneThickness = 14f

        when (exerciseType) {
            "Squat" -> {
                // Standing: pulse = 0f. Deep squat: pulse = 1f
                val headY = centerY - 140f + (pulse * 80f)
                val hipY = centerY + 10f + (pulse * 95f)
                val kneeY = centerY + 110f + (pulse * 40f)
                val ankleY = centerY + 200f
                val kneeXOffset = pulse * 35f

                // Head
                drawCircle(color = DuoInk, radius = 24f, center = Offset(centerX, headY - 40f))

                // Spine (Shoulder to Hip)
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, headY),
                    end = Offset(centerX, hipY),
                    strokeWidth = boneThickness
                )

                // Left Leg (Hip -> Knee -> Ankle)
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, hipY),
                    end = Offset(centerX - 35f - kneeXOffset, kneeY),
                    strokeWidth = boneThickness
                )
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX - 35f - kneeXOffset, kneeY),
                    end = Offset(centerX - 40f, ankleY),
                    strokeWidth = boneThickness
                )

                // Right Leg
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, hipY),
                    end = Offset(centerX + 35f + kneeXOffset, kneeY),
                    strokeWidth = boneThickness
                )
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX + 35f + kneeXOffset, kneeY),
                    end = Offset(centerX + 40f, ankleY),
                    strokeWidth = boneThickness
                )

                // Arms (extended forward in squats)
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, headY + 10f),
                    end = Offset(centerX - 60f, headY + 20f),
                    strokeWidth = boneThickness
                )

                // Overlay joints as dots
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(centerX, headY))
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(centerX, hipY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(centerX - 35f - kneeXOffset, kneeY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(centerX + 35f + kneeXOffset, kneeY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(centerX - 40f, ankleY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(centerX + 40f, ankleY))
            }
            "Push-up" -> {
                // Rotated figure representing Plank/Pushup mechanics
                // High plank: pulse = 0f. Chest down: pulse = 1f
                val dipOffset = pulse * 50f
                val shoulderX = centerX - 100f
                val shoulderY = centerY + 40f - dipOffset
                val hipX = centerX + 10f
                val hipY = centerY + 40f - (dipOffset * 0.5f)
                val ankleX = centerX + 120f
                val ankleY = centerY + 60f

                val elbowX = centerX - 80f - (pulse * 30f)
                val elbowY = centerY + 90f
                val wristX = centerX - 100f
                val wristY = centerY + 130f

                // Head
                drawCircle(color = DuoInk, radius = 22f, center = Offset(shoulderX - 35f, shoulderY - 15f))

                // Spine (Shoulder to Hip)
                drawLine(
                    color = bonePaintColor,
                    start = Offset(shoulderX, shoulderY),
                    end = Offset(hipX, hipY),
                    strokeWidth = boneThickness
                )

                // Hips to Ankles
                drawLine(
                    color = bonePaintColor,
                    start = Offset(hipX, hipY),
                    end = Offset(ankleX, ankleY),
                    strokeWidth = boneThickness
                )

                // Arm (Shoulder -> Elbow -> Wrist/Floor)
                drawLine(
                    color = bonePaintColor,
                    start = Offset(shoulderX, shoulderY),
                    end = Offset(elbowX, elbowY),
                    strokeWidth = boneThickness
                )
                drawLine(
                    color = bonePaintColor,
                    start = Offset(elbowX, elbowY),
                    end = Offset(wristX, wristY),
                    strokeWidth = boneThickness
                )

                // Joints
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(shoulderX, shoulderY))
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(hipX, hipY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(elbowX, elbowY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(wristX, wristY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(ankleX, ankleY))
            }
            "Pull-up" -> {
                // Pull-up Bar at the top
                val barY = centerY - 140f
                drawLine(
                    color = DuoInk,
                    start = Offset(centerX - 120f, barY),
                    end = Offset(centerX + 120f, barY),
                    strokeWidth = 10f
                )
                // Bar mounts
                drawLine(color = DuoBorder, start = Offset(centerX - 100f, barY), end = Offset(centerX - 100f, barY - 40f), strokeWidth = 4f)
                drawLine(color = DuoBorder, start = Offset(centerX + 100f, barY), end = Offset(centerX + 100f, barY - 40f), strokeWidth = 4f)

                // Hands fixed on bar
                val leftHandX = centerX - 55f
                val rightHandX = centerX + 55f

                // Dead hang: pulse = 0f. Chin over bar: pulse = 1f
                val liftY = pulse * 105f
                val headY = centerY - 35f - liftY
                val shoulderY = centerY + 5f - liftY
                val hipY = centerY + 85f - liftY

                // Elbow flaring outward as person pulls up
                val elbowFlopX = pulse * 28f
                val elbowY = centerY - 55f - (liftY * 0.45f)

                // Legs tucked back slightly during pull-up
                val kneeY = centerY + 140f - liftY
                val ankleY = centerY + 185f - liftY

                // Thermal lat activation color mapping based on live model (or fallback for virtual)
                val latEffort = pullUpMetrics?.latsEffort ?: (0.3 + pulse * 0.5)
                val muscleThermalColor = if (latEffort > 0.8) DuoRed else if (latEffort > 0.5) DuoOrange else if (latEffort > 0.2) DuoYellow else DuoBlue

                // Head (rises above bar when pulse > 0.85)
                drawCircle(color = DuoInk, radius = 22f, center = Offset(centerX, headY))

                // Latissimus Dorsi Polygon Mesh
                val latPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centerX - 35f, shoulderY)
                    lineTo(centerX + 35f, shoulderY)
                    lineTo(centerX + 25f, hipY)
                    lineTo(centerX - 25f, hipY)
                    close()
                }
                drawPath(path = latPath, color = muscleThermalColor.copy(alpha = 0.85f))

                // Spine
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, shoulderY),
                    end = Offset(centerX, hipY),
                    strokeWidth = boneThickness
                )

                // Arms: Wrists -> Elbows -> Shoulders
                // Left Arm
                drawLine(color = bonePaintColor, start = Offset(leftHandX, barY), end = Offset(leftHandX - elbowFlopX, elbowY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(leftHandX - elbowFlopX, elbowY), end = Offset(centerX - 30f, shoulderY), strokeWidth = boneThickness)

                // Right Arm
                drawLine(color = bonePaintColor, start = Offset(rightHandX, barY), end = Offset(rightHandX + elbowFlopX, elbowY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(rightHandX + elbowFlopX, elbowY), end = Offset(centerX + 30f, shoulderY), strokeWidth = boneThickness)

                // Legs (tucked knees)
                drawLine(color = bonePaintColor, start = Offset(centerX - 12f, hipY), end = Offset(centerX - 15f, kneeY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(centerX - 15f, kneeY), end = Offset(centerX - 10f, ankleY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(centerX + 12f, hipY), end = Offset(centerX + 15f, kneeY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(centerX + 15f, kneeY), end = Offset(centerX + 10f, ankleY), strokeWidth = boneThickness)

                // Joint Dots
                drawCircle(color = DuoInk, radius = 8f, center = Offset(leftHandX, barY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(rightHandX, barY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(leftHandX - elbowFlopX, elbowY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(rightHandX + elbowFlopX, elbowY))
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(centerX - 30f, shoulderY))
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(centerX + 30f, shoulderY))
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(centerX, hipY))
            }
            "Lunge" -> {
                // Standing: pulse = 0f. Flexed lunge depth: pulse = 1f
                val headY = centerY - 140f + (pulse * 70f)
                val hipY = centerY + 10f + (pulse * 80f)
                
                // Front Leg (Steps forward - Left)
                val frontKneeX = centerX - 60f
                val frontKneeY = centerY + 100f + (pulse * 10f)
                val frontAnkleX = centerX - 60f
                val frontAnkleY = centerY + 200f

                // Back Leg (Stays backward - Right)
                val backKneeX = centerX + 40f + (pulse * 10f)
                val backKneeY = centerY + 100f + (pulse * 70f)
                val backAnkleX = centerX + 80f
                val backAnkleY = centerY + 200f

                // Head
                drawCircle(color = DuoInk, radius = 24f, center = Offset(centerX, headY - 40f))

                // Spine
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, headY),
                    end = Offset(centerX, hipY),
                    strokeWidth = boneThickness
                )

                // Front Leg
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, hipY),
                    end = Offset(frontKneeX, frontKneeY),
                    strokeWidth = boneThickness
                )
                drawLine(
                    color = bonePaintColor,
                    start = Offset(frontKneeX, frontKneeY),
                    end = Offset(frontAnkleX, frontAnkleY),
                    strokeWidth = boneThickness
                )

                // Back Leg
                drawLine(
                    color = bonePaintColor,
                    start = Offset(centerX, hipY),
                    end = Offset(backKneeX, backKneeY),
                    strokeWidth = boneThickness
                )
                drawLine(
                    color = bonePaintColor,
                    start = Offset(backKneeX, backKneeY),
                    end = Offset(backAnkleX, backAnkleY),
                    strokeWidth = boneThickness
                )

                // Draw Joint Highlights
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(centerX, hipY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(frontKneeX, frontKneeY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(backKneeX, backKneeY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(frontAnkleX, frontAnkleY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(backAnkleX, backAnkleY))
            }
            "Plank" -> {
                // Static body plank alignment with minor core vibration
                val wobble = sin(timerSeconds * 2.5f) * 4f
                val shoulderX = centerX - 100f
                val shoulderY = centerY + 50f
                val hipX = centerX + 10f
                val hipY = centerY + 50f + wobble
                val ankleX = centerX + 120f
                val ankleY = centerY + 65f

                val wristX = centerX - 100f
                val wristY = centerY + 130f

                // Head
                drawCircle(color = DuoInk, radius = 22f, center = Offset(shoulderX - 35f, shoulderY - 15f))

                // Bone lines
                drawLine(color = bonePaintColor, start = Offset(shoulderX, shoulderY), end = Offset(hipX, hipY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(hipX, hipY), end = Offset(ankleX, ankleY), strokeWidth = boneThickness)
                drawLine(color = bonePaintColor, start = Offset(shoulderX, shoulderY), end = Offset(wristX, wristY), strokeWidth = boneThickness)

                // Highlight Spine Line
                drawCircle(color = DuoYellow, radius = 8f, center = Offset(shoulderX, shoulderY))
                drawCircle(color = DuoOrange, radius = 8f, center = Offset(hipX, hipY))
                drawCircle(color = DuoInk, radius = 8f, center = Offset(ankleX, ankleY))
            }
        }
    }
}

@Composable
fun PullUpDashboardHUD(metrics: com.example.cv.PullUpMetrics) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${metrics.repCount}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(" REPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            }
            
            Box(modifier = Modifier
                .background(if (metrics.phase == "HOLD") DuoOrange else if (metrics.phase == "PULL") DuoGreen else DuoInk, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(metrics.phase, color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("${metrics.chinAtBarCount}/${metrics.repCount} CHIN AT THE BAR", color = Color.White, fontSize = 12.sp)
            Text("${metrics.speedLossPct} % SPEED VS REP 1", color = Color.White, fontSize = 12.sp)
            Text("${metrics.peakPowerW} W PEAK POWER", color = Color.White, fontSize = 12.sp)
            Text(String.format("+%.2f °C LATS · MODELLED", metrics.latsTempRise), color = Color.White, fontSize = 12.sp)
            Text("${metrics.latsFatiguedPct} % · ${metrics.bicepsFatiguedPct} % LATS · BICEPS FATIGUED, MODEL", color = Color.White, fontSize = 12.sp)
            Text(String.format("≈ %.1f kcal / %.1f kJ OF HEAT", metrics.totalKcal, metrics.totalHeatKj), color = Color.White, fontSize = 12.sp)
        }
        
        // BOTTOM HUD
        if (metrics.lastRep != null) {
            val rep = metrics.lastRep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x99000000), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("REP ${rep.repNum}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (rep.fullLockout) Box(modifier = Modifier.background(DuoGreen, RoundedCornerShape(4.dp)).padding(4.dp)) { Text("FULL LOCK-OUT", color = Color.White, fontSize = 10.sp) }
                            if (rep.swayCm < 10) Box(modifier = Modifier.background(DuoGreen, RoundedCornerShape(4.dp)).padding(4.dp)) { Text("NO SWING", color = Color.White, fontSize = 10.sp) }
                        }
                        Text(String.format("up %.1f s hold %.1f s down %.1f s", rep.durationConcentric, rep.durationHold, rep.durationEccentric), color = Color.White, fontSize = 12.sp)
                        Text(String.format("peak %.2f m/s %d W ≈ %.1f kcal", rep.peakVelocity, rep.peakPower.toInt(), rep.energyKcal), color = Color.White, fontSize = 12.sp)
                        Text(String.format("speed loss %d %% sway %.0f cm", rep.speedLossPct, rep.swayCm), color = Color.White, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(if (rep.chinVerdict.contains("ABOVE")) DuoGreen else if (rep.chinVerdict.contains("AT")) DuoOrange else DuoRed, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(rep.chinVerdict, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraWithPoseOverlay(
    exerciseType: String,
    pullUpMetrics: com.example.cv.PullUpMetrics? = null,
    onFrameAnalysis: (Int, String?, String, Boolean, PoseSkeleton?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val previewView = remember(context) { PreviewView(context) }

    // Real-time local evaluation states
    val evaluator = remember { ExerciseFormEvaluator() }
    var currentSkeleton by remember { mutableStateOf<PoseSkeleton?>(null) }
    var currentResult by remember { mutableStateOf<EvaluationResult?>(null) }

    // Draw the ML Kit camera skeleton overlays
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Draw overlay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val skeleton = currentSkeleton ?: return@Canvas
            
            // Scaler helper to translate 0..1 ML Kit coordinates to local canvas size
            fun LandmarkPoint.toOffset(): Offset {
                val xPos = if (lensFacing == CameraSelector.LENS_FACING_FRONT) (1f - this.x) else this.x
                return Offset(xPos * size.width, this.y * size.height)
            }

            // Draw bone vectors
            fun drawBone(p1: LandmarkPoint?, p2: LandmarkPoint?) {
                if (p1 != null && p2 != null && p1.confidence > 0.5f && p2.confidence > 0.5f) {
                    drawLine(
                        color = DuoBlue,
                        start = p1.toOffset(),
                        end = p2.toOffset(),
                        strokeWidth = 10f
                    )
                }
            }

            // Draw Bone Vectors depending on exercise
            if (exerciseType == "Pull-up") {
                // Pull-up specific heatmap and geometry drawing
                val effortColor = fun(effort: Double): Color {
                    return if (effort > 0.8) DuoRed else if (effort > 0.6) DuoOrange else if (effort > 0.3) DuoYellow else if (effort > 0.1) DuoGreen else DuoBlue.copy(alpha = 0.5f)
                }

                // Draw Lats Polygon
                if (skeleton.shoulderLeft != null && skeleton.shoulderRight != null && skeleton.hipLeft != null && skeleton.hipRight != null) {
                    val latColor = effortColor(pullUpMetrics?.latsEffort ?: 0.0)
                    val latPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(skeleton.shoulderLeft.toOffset().x, skeleton.shoulderLeft.toOffset().y)
                        lineTo(skeleton.shoulderRight.toOffset().x, skeleton.shoulderRight.toOffset().y)
                        lineTo(skeleton.hipRight.toOffset().x, skeleton.hipRight.toOffset().y)
                        lineTo(skeleton.hipLeft.toOffset().x, skeleton.hipLeft.toOffset().y)
                        close()
                    }
                    drawPath(path = latPath, color = latColor.copy(alpha = 0.8f))
                }
                
                // Draw normal skeleton on top
                drawBone(skeleton.shoulderLeft, skeleton.shoulderRight)
                drawBone(skeleton.shoulderLeft, skeleton.hipLeft)
                drawBone(skeleton.shoulderRight, skeleton.hipRight)
                drawBone(skeleton.hipLeft, skeleton.hipRight)
                
                // Biceps Heatmap
                val bicepColor = effortColor(pullUpMetrics?.bicepsEffort ?: 0.0)
                if (skeleton.shoulderLeft != null && skeleton.elbowLeft != null) {
                    drawLine(color = bicepColor.copy(alpha = 0.8f), start = skeleton.shoulderLeft.toOffset(), end = skeleton.elbowLeft.toOffset(), strokeWidth = 35f)
                }
                if (skeleton.shoulderRight != null && skeleton.elbowRight != null) {
                    drawLine(color = bicepColor.copy(alpha = 0.8f), start = skeleton.shoulderRight.toOffset(), end = skeleton.elbowRight.toOffset(), strokeWidth = 35f)
                }
                
                // Forearms Heatmap
                val forearmColor = effortColor(pullUpMetrics?.forearmsEffort ?: 0.0)
                if (skeleton.elbowLeft != null && skeleton.wristLeft != null) {
                    drawLine(color = forearmColor.copy(alpha = 0.8f), start = skeleton.elbowLeft.toOffset(), end = skeleton.wristLeft.toOffset(), strokeWidth = 25f)
                }
                if (skeleton.elbowRight != null && skeleton.wristRight != null) {
                    drawLine(color = forearmColor.copy(alpha = 0.8f), start = skeleton.elbowRight.toOffset(), end = skeleton.wristRight.toOffset(), strokeWidth = 25f)
                }
                
            } else {
                // Default Body outlines
                drawBone(skeleton.shoulderLeft, skeleton.shoulderRight)
                drawBone(skeleton.shoulderLeft, skeleton.hipLeft)
                drawBone(skeleton.shoulderRight, skeleton.hipRight)
                drawBone(skeleton.hipLeft, skeleton.hipRight)
    
                // Arms
                drawBone(skeleton.shoulderLeft, skeleton.elbowLeft)
                drawBone(skeleton.elbowLeft, skeleton.wristLeft)
                drawBone(skeleton.shoulderRight, skeleton.elbowRight)
                drawBone(skeleton.elbowRight, skeleton.wristRight)
            }

            // Legs
            drawBone(skeleton.hipLeft, skeleton.kneeLeft)
            drawBone(skeleton.kneeLeft, skeleton.ankleLeft)
            drawBone(skeleton.hipRight, skeleton.kneeRight)
            drawBone(skeleton.kneeRight, skeleton.ankleRight)

            // Draw circles over joints
            val joints = listOf(
                skeleton.shoulderLeft, skeleton.shoulderRight,
                skeleton.elbowLeft, skeleton.elbowRight,
                skeleton.wristLeft, skeleton.wristRight,
                skeleton.hipLeft, skeleton.hipRight,
                skeleton.kneeLeft, skeleton.kneeRight,
                skeleton.ankleLeft, skeleton.ankleRight
            )

            joints.forEach { point ->
                if (point != null && point.confidence > 0.5f) {
                    drawCircle(
                        color = DuoYellow,
                        radius = 12f,
                        center = point.toOffset()
                    )
                }
            }
        }

        // Camera Switch Button Overlay
        IconButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                com.example.audio.DuoSoundPlayer.playClick()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                .border(2.dp, DuoBorder, shape = CircleShape)
                .testTag("switch_camera_button")
        ) {
            Icon(
                imageVector = Icons.Default.FlipCameraAndroid,
                contentDescription = "Switch Camera",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Camera Mode Indicator Tag
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                .border(1.5.dp, DuoBorder, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (lensFacing == CameraSelector.LENS_FACING_FRONT) "FRONT CAM" else "BACK CAM",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bind CameraX preview and ML Kit Analyzer
        LaunchedEffect(exerciseType, lensFacing) {
            evaluator.reset()
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = androidx.camera.core.Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                // Pose Detection Configuration
                val options = PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build()
                val detector = PoseDetection.getClient(options)

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(
                        imageProxy = imageProxy,
                        detector = detector,
                        onResult = { skeleton ->
                            currentSkeleton = skeleton
                            if (skeleton != null) {
                                val result = when (exerciseType) {
                                    "Pull-up" -> evaluator.evaluatePullup(skeleton)
                                    "Squat" -> evaluator.evaluateSquat(skeleton)
                                    "Push-up" -> evaluator.evaluatePushup(skeleton)
                                    "Lunge" -> evaluator.evaluateLunge(skeleton)
                                    "Plank" -> evaluator.evaluatePlank(skeleton)
                                    else -> EvaluationResult.idle("Active")
                                }
                                currentResult = result
                                onFrameAnalysis(result.score, result.mistake, result.feedback, result.isRepCompleted, skeleton)
                            }
                        }
                    )
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPoseOverlay", "Binding camera failed for lens $lensFacing", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun processImageProxy(
    imageProxy: ImageProxy,
    detector: com.google.mlkit.vision.pose.PoseDetector,
    onResult: (PoseSkeleton?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { pose ->
                val skeleton = parsePoseLandmarks(pose)
                onResult(skeleton)
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun parsePoseLandmarks(pose: com.google.mlkit.vision.pose.Pose): PoseSkeleton {
    fun getLandmark(type: Int): LandmarkPoint? {
        val landmark = pose.getPoseLandmark(type) ?: return null
        // Normalize coordinates relative to image bounds for overlay drawing
        return LandmarkPoint(landmark.position.x / 480f, landmark.position.y / 640f, landmark.inFrameLikelihood)
    }

    return PoseSkeleton(
        shoulderLeft = getLandmark(PoseLandmark.LEFT_SHOULDER),
        shoulderRight = getLandmark(PoseLandmark.RIGHT_SHOULDER),
        elbowLeft = getLandmark(PoseLandmark.LEFT_ELBOW),
        elbowRight = getLandmark(PoseLandmark.RIGHT_ELBOW),
        wristLeft = getLandmark(PoseLandmark.LEFT_WRIST),
        wristRight = getLandmark(PoseLandmark.RIGHT_WRIST),
        hipLeft = getLandmark(PoseLandmark.LEFT_HIP),
        hipRight = getLandmark(PoseLandmark.RIGHT_HIP),
        kneeLeft = getLandmark(PoseLandmark.LEFT_KNEE),
        kneeRight = getLandmark(PoseLandmark.RIGHT_KNEE),
        ankleLeft = getLandmark(PoseLandmark.LEFT_ANKLE),
        ankleRight = getLandmark(PoseLandmark.RIGHT_ANKLE)
    )
}
