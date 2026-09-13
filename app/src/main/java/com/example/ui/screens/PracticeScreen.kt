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

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R

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
    val pullUpMetrics by viewModel.pullUpMetrics.collectAsState()
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
                // RENDER EXERCISE INFO SCREEN
                ExerciseInfoView(exerciseType = exerciseType, repCount = repCount)
            } else {
                // RENDER CAMERAX PREVIEW OR PERMISSION REQUEST
                if (cameraPermissionState.status.isGranted) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraWithPoseOverlay(
                            exerciseType = exerciseType,
                            onFrameAnalysis = { score, mistake, fb, rep, skeleton ->
                                viewModel.processCameraFrameAnalysis(score, mistake, fb, rep, skeleton)
                            }
                        )
                        if (exerciseType == "Pull-up") {
                            PullUpDashboardHUD(pullUpMetrics)
                        } else {
                            // Live In-Camera Overlay for Reps and Feedback
                            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                            Box(modifier = Modifier.background(DuoSurface1, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(
                                    text = if (exerciseType == "Plank") "HOLD: ${repCount}s" else "REPS: $repCount",
                                    color = DuoYellow,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.background(if (formScore < 80) Color(0xFFFFEBEE) else Color(0xFFE8F5E9), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(
                                    text = feedback,
                                    color = if (formScore < 80) DuoRed else DuoGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
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
fun ExerciseInfoView(exerciseType: String, repCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exerciseType.uppercase(),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DuoInk
            )
            Text(
                text = "Replace",
                color = DuoBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F9FF), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.pullup_illustration_1789326142612), // Placeholder generated image
                contentDescription = "Exercise Illustration",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Toggles
        Row(
            modifier = Modifier
                .background(Color(0xFFF1F3F5), RoundedCornerShape(20.dp))
                .padding(4.dp)
        ) {
            Box(modifier = Modifier.background(DuoBlue, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Animation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Muscle", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("How to do", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Repeats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REPEATS",
                color = DuoBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color(0xFFF1F3F5), RoundedCornerShape(8.dp)).size(32.dp), contentAlignment = Alignment.Center) { Text("-", fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(16.dp))
                Text("$repCount", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DuoInk)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.background(Color(0xFFF1F3F5), RoundedCornerShape(8.dp)).size(32.dp), contentAlignment = Alignment.Center) { Text("+", fontWeight = FontWeight.Bold) }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(
                text = "INSTRUCTIONS",
                color = DuoBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start in a proper position. Lower your body under control, then push or pull back to the starting position and repeat the exercise. Please remember to keep proper form during this exercise.",
                color = DuoInk,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraWithPoseOverlay(
    exerciseType: String,
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
