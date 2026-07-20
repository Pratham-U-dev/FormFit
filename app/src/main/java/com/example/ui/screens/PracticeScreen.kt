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
                VirtualCoachCanvas(exerciseType = exerciseType, timerSeconds = timerSeconds)
            } else {
                // RENDER CAMERAX PREVIEW OR PERMISSION REQUEST
                if (cameraPermissionState.status.isGranted) {
                    CameraWithPoseOverlay(
                        exerciseType = exerciseType,
                        onFrameAnalysis = { score, mistake, fb, rep ->
                            viewModel.processCameraFrameAnalysis(score, mistake, fb, rep)
                        }
                    )
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
                            modifier = Modifier.height(44.dp)
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
                modifier = Modifier.weight(0.4f).height(50.dp)
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
                testTag = "finish_workout_button"
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
fun VirtualCoachCanvas(exerciseType: String, timerSeconds: Int) {
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

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraWithPoseOverlay(
    exerciseType: String,
    onFrameAnalysis: (Int, String?, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val previewView = remember { PreviewView(context) }

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
                return Offset(this.x * size.width, this.y * size.height)
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

            // Body outlines
            drawBone(skeleton.shoulderLeft, skeleton.shoulderRight)
            drawBone(skeleton.shoulderLeft, skeleton.hipLeft)
            drawBone(skeleton.shoulderRight, skeleton.hipRight)
            drawBone(skeleton.hipLeft, skeleton.hipRight)

            // Arms
            drawBone(skeleton.shoulderLeft, skeleton.elbowLeft)
            drawBone(skeleton.elbowLeft, skeleton.wristLeft)
            drawBone(skeleton.shoulderRight, skeleton.elbowRight)
            drawBone(skeleton.elbowRight, skeleton.wristRight)

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

        // Bind CameraX preview and ML Kit Analyzer
        LaunchedEffect(exerciseType) {
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
                                    "Squat" -> evaluator.evaluateSquat(skeleton)
                                    "Push-up" -> evaluator.evaluatePushup(skeleton)
                                    "Lunge" -> evaluator.evaluateLunge(skeleton)
                                    "Plank" -> evaluator.evaluatePlank(skeleton)
                                    else -> EvaluationResult.idle("Active")
                                }
                                currentResult = result
                                onFrameAnalysis(result.score, result.mistake, result.feedback, result.isRepCompleted)
                            }
                        }
                    )
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("CameraPoseOverlay", "Binding camera failed", e)
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
