package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.VideoView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.style.TextOverflow
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
    val exerciseState by viewModel.exerciseState.collectAsState()
    val targetReps by viewModel.targetReps.collectAsState()
    val timerSeconds by viewModel.sessionSeconds.collectAsState()
    val feedback by viewModel.currentFeedback.collectAsState()
    val formScore by viewModel.currentScore.collectAsState()
    val isVirtual by viewModel.isVirtualCoachMode.collectAsState()
    val mistakes by viewModel.mistakesList.collectAsState()
    val liveSpeedLossPct by viewModel.liveSpeedLossPct.collectAsState()
    val livePeakPowerW by viewModel.livePeakPowerW.collectAsState()
    val liveVelocityMps by viewModel.liveVelocityMps.collectAsState()
    val bodyProfile by viewModel.userBodyProfile.collectAsState()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // Auto-switch to workout complete when user hits target repeats during live camera workout
    LaunchedEffect(repCount, pullUpMetrics.repCount, targetReps, isVirtual) {
        val currentReps = if (exerciseType == "Pull-up") pullUpMetrics.repCount else repCount
        if (!isVirtual && targetReps > 0 && currentReps >= targetReps) {
            viewModel.stopAndSaveWorkout()
            onWorkoutFinished()
        }
    }

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
                    text = if (isVirtual) "INFO & GUIDE" else "TRAINING: ${exerciseType.uppercase()}",
                    color = DuoInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (isVirtual) "Exercise Guide & Form" else "Real-time pose analysis",
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
                        contentDescription = "Info & Guide",
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
                ExerciseInfoView(
                    exerciseType = exerciseType,
                    targetReps = targetReps,
                    onIncrementTarget = { viewModel.incrementTargetReps() },
                    onDecrementTarget = { viewModel.decrementTargetReps() },
                    onSelectPreset = { count -> viewModel.setTargetReps(count) },
                    onStartCameraWorkout = {
                        viewModel.setVirtualCoachMode(false)
                        if (!cameraPermissionState.status.isGranted) {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    exerciseState = exerciseState
                )
            } else {
                // RENDER CAMERAX PREVIEW OR PERMISSION REQUEST
                if (cameraPermissionState.status.isGranted) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CameraWithPoseOverlay(
                            exerciseType = exerciseType,
                            userWeightKg = bodyProfile.weightKg,
                            userHeightCm = bodyProfile.heightCm,
                            onFrameAnalysis = { score, mistake, fb, rep, state, skeleton, vel, power, vbtLoss ->
                                viewModel.processCameraFrameAnalysis(score, mistake, fb, rep, state, skeleton, vel, power, vbtLoss)
                            }
                        )
                        if (exerciseType == "Pull-up") {
                            PullUpDashboardHUD(pullUpMetrics, targetReps)
                        } else {
                            // Live In-Camera Overlay for Reps, State, VBT Telemetry and Feedback
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 16.dp, top = 56.dp, end = 16.dp, bottom = 16.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(DuoSurface1, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text(
                                            text = if (exerciseType == "Plank") "HOLD: ${repCount}s / ${targetReps}s" else "REPS: $repCount / $targetReps",
                                            color = DuoYellow,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Box(modifier = Modifier.background(DuoBlue, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = "STATE: $exerciseState",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (exerciseType != "Plank") {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text("⚡ $liveSpeedLossPct % SPEED LOSS VS REP 1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("🔥 $livePeakPowerW W PEAK POWER · ${String.format("%.2f", liveVelocityMps)} m/s", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
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

        // BOTTOM ACTION BUTTONS
        if (isVirtual) {
            // Info & Guide Actions: purely for info, do not save fake session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DuoButton(
                    onClick = { viewModel.abandonWorkout(); onWorkoutFinished() },
                    backgroundColor = Color.White,
                    shadowColor = DuoBorder,
                    textColor = DuoInkMuted,
                    modifier = Modifier.weight(0.35f).height(50.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(
                        "EXIT GUIDE",
                        color = DuoInkMuted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                DuoButton(
                    onClick = {
                        viewModel.setVirtualCoachMode(false)
                        if (!cameraPermissionState.status.isGranted) {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    backgroundColor = DuoBlue,
                    shadowColor = DuoBlueDark,
                    modifier = Modifier.weight(0.65f).height(50.dp),
                    testTag = "start_camera_workout_button",
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Start Camera",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "START WORKOUT ($targetReps)",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        } else {
            // Live Camera Workout Actions: finish early or abandon
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
}



@Composable
fun ExerciseInfoView(
    exerciseType: String,
    targetReps: Int,
    onIncrementTarget: () -> Unit,
    onDecrementTarget: () -> Unit,
    onSelectPreset: (Int) -> Unit = {},
    onStartCameraWorkout: () -> Unit = {},
    exerciseState: String = "READY"
) {
    var selectedTab by remember { mutableStateOf("Animation") }
    var selectedMuscleName by remember { mutableStateOf<String?>("Latissimus Dorsi (Lats)") }
    var activeStepNumber by remember { mutableIntStateOf(1) }
    var checklistState by remember { mutableStateOf(setOf(0, 1)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title Header: Info & Guide
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "INFO & GUIDE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DuoInk
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(DuoBlue, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(exerciseType.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // THREE FUNCTIONAL TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F3F5), RoundedCornerShape(20.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Animation", "Muscle", "How to do").forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) DuoBlue else Color.Transparent)
                        .clickable { selectedTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) Color.White else DuoInkMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // TAB 1: ANIMATION (pure animation playback without speed controls or labels)
        when (selectedTab) {
            "Animation" -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.5.dp, DuoBorder, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (exerciseType == "Pull-up") {
                            // User's attached animated MP4
                            ChinupVideoPlayer(
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val infiniteTransition = rememberInfiniteTransition(label = "ExerciseAnimation")
                            val animatedOffset by infiniteTransition.animateFloat(
                                initialValue = 18f,
                                targetValue = -25f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "ExerciseOffset"
                            )
                            ExerciseSkeletonDiagram(exerciseType = exerciseType, offset = animatedOffset)
                        }
                    }
                }
            }

            // TAB 2: MUSCLE ANATOMY
            "Muscle" -> {
                val muscles = when (exerciseType) {
                    "Pull-up" -> listOf(
                        MuscleData("Latissimus Dorsi (Lats)", "Prime Mover · Shoulder Adduction & Downward Pull", 95, DuoGreen, "Concentric & Eccentric", "Drive your elbows down into back pockets rather than pulling with forearms."),
                        MuscleData("Biceps Brachii", "Synergist · Elbow Flexion & Pull Strength", 88, DuoBlue, "Concentric Flexion", "Supinated or neutral grips increase bicep load; maintain forearm alignment."),
                        MuscleData("Forearms & Grip", "Bar Suspension · Isometric Lock", 82, DuoOrange, "Isometric Hold", "Wrap thumbs completely around the bar to maximize grip endurance."),
                        MuscleData("Rhomboids & Traps", "Scapular Retraction & Depression", 78, DuoYellow, "Isometric Retraction", "Pack your shoulder blades down and back before beginning each pull."),
                        MuscleData("Core & Abdominals", "Anti-Extension & Strict Form", 65, DuoInkMuted, "Isometric Stabilization", "Brace your core and cross ankles to eliminate momentum and kipping.")
                    )
                    "Squat" -> listOf(
                        MuscleData("Quadriceps", "Prime Mover · Knee Extension", 95, DuoGreen, "Concentric & Eccentric", "Push the floor away through mid-foot and maintain knee alignment over toes."),
                        MuscleData("Gluteus Maximus", "Hip Extension & Lockout Drive", 90, DuoBlue, "Concentric Extension", "Squeeze glutes at top lockout and maintain hip depth below parallel."),
                        MuscleData("Hamstrings", "Hip Hinge Stability & Knee Protection", 75, DuoYellow, "Isometric Control", "Engage hamstrings during descent to decelerate hips smoothly."),
                        MuscleData("Core & Erectors", "Spinal Neutrality & Torso Uprightness", 70, DuoInkMuted, "Isometric Bracing", "Inhale and brace intra-abdominal pressure to protect lumbar spine.")
                    )
                    "Push-up" -> listOf(
                        MuscleData("Pectoralis Major", "Prime Mover · Horizontal Adduction", 95, DuoGreen, "Concentric Press", "Flare elbows no more than 45° to protect rotator cuff and isolate chest."),
                        MuscleData("Triceps Brachii", "Elbow Lockout & Pressing Power", 88, DuoBlue, "Concentric Extension", "Lock elbows cleanly at top without hyper-extending joints."),
                        MuscleData("Anterior Deltoid", "Shoulder Flexion & Descent Control", 80, DuoOrange, "Concentric Drive", "Keep shoulders depressed away from neck throughout the motion."),
                        MuscleData("Core & Abdominals", "Anti-Extension Plank Rigidity", 75, DuoInkMuted, "Isometric Hold", "Do not allow hips to sag or hike; maintain straight head-to-heel line.")
                    )
                    else -> listOf(
                        MuscleData("Transverse Abdominis", "Deep Core Compression", 96, DuoGreen, "Isometric Bracing", "Draw navel toward spine while maintaining steady nasal breathing."),
                        MuscleData("Rectus Abdominis", "Anti-Extension & Pelvic Stability", 90, DuoBlue, "Isometric Hold", "Maintain slight posterior pelvic tilt to engage rectus abdominis fully."),
                        MuscleData("Internal & External Obliques", "Lateral & Rotational Control", 85, DuoYellow, "Isometric Hold", "Resist torso twisting or hip swaying."),
                        MuscleData("Glutes & Quads", "Lower Body Tension", 72, DuoInkMuted, "Isometric Tension", "Squeeze quads and glutes to lock pelvis in neutral posture.")
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .border(1.5.dp, DuoBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TARGETED MUSCLE GROUPS (TAP TO INSPECT)",
                        color = DuoInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )

                    muscles.forEach { muscle ->
                        val isSelected = selectedMuscleName == muscle.name
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFF1F5F9) else Color.White)
                                .border(1.dp, if (isSelected) DuoBlue else DuoBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedMuscleName = if (isSelected) null else muscle.name
                                }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = muscle.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DuoInk)
                                Text(text = "${muscle.percentage}% EMG", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = muscle.color)
                            }
                            Text(text = muscle.role, fontSize = 11.sp, color = DuoInkMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { muscle.percentage / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = muscle.color,
                                trackColor = Color(0xFFE2E8F0)
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(text = "Style: ${muscle.contraction}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DuoBlue)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "Pro Tip: ${muscle.tip}", fontSize = 11.sp, color = DuoInk, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: HOW TO DO & CHECKLIST
            "How to do" -> {
                val steps = when (exerciseType) {
                    "Pull-up" -> listOf(
                        "1" to ("Grip & Setup" to "Grip the bar slightly wider than shoulder width with an overhand (pronated) grip. Wrap thumbs securely around the bar."),
                        "2" to ("Active Dead Hang" to "Engage back by depressing shoulders away from ears. Pack scapulae, cross ankles, and brace core."),
                        "3" to ("Concentric Pull" to "Drive elbows down and back toward your ribs. Lead with chest, avoiding craning or throwing neck forward."),
                        "4" to ("Apex Chin Clearance" to "Pull until chin clears horizontally above bar level. Squeeze lats hard for a brief 0.5s apex hold."),
                        "5" to ("Controlled 2-3s Descent" to "Lower smoothly under muscular control until arms achieve complete elbow lockout at full dead hang.")
                    )
                    "Squat" -> listOf(
                        "1" to ("Foot Stance" to "Feet shoulder-width apart, toes pointed 15–30 degrees outward. Chest proud, core engaged."),
                        "2" to ("Hip Hinge" to "Break at hips and knees simultaneously. Push knees outward in line with your toes."),
                        "3" to ("Depth" to "Descend until hip crease is below the top of knees (parallel or deeper). Keep torso upright."),
                        "4" to ("Drive Up" to "Drive through full foot to stand up. Squeeze glutes and extend hips fully at the top.")
                    )
                    "Push-up" -> listOf(
                        "1" to ("Hand Placement" to "Hands slightly wider than shoulders, fingers spread. Body forms a rigid straight plank line."),
                        "2" to ("Descent" to "Lower chest toward the ground by bending elbows at a 45-degree angle to your torso."),
                        "3" to ("Bottom Depth" to "Descend until chest is about 1–2 inches off the ground without allowing hips to sag."),
                        "4" to ("Concentric Press" to "Press floor away vigorously to return to full elbow lockout at top of plank.")
                    )
                    else -> listOf(
                        "1" to ("Elbow Alignment" to "Place elbows directly below shoulders. Forearms parallel or hands clasped."),
                        "2" to ("Spine Neutrality" to "Form a straight line from heels to ears. Tuck pelvis slightly to engage lower abs."),
                        "3" to ("Bracing" to "Squeeze quads, glutes, and abdominals simultaneously. Breathe steadily through nose.")
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                        .border(1.5.dp, DuoBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "STEP-BY-STEP TECHNIQUE GUIDE",
                        color = DuoInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )

                    steps.forEachIndexed { index, (stepNum, pair) ->
                        val (title, desc) = pair
                        val isStepActive = activeStepNumber == index + 1
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isStepActive) Color(0xFFEFF6FF) else Color.White)
                                .border(1.dp, if (isStepActive) DuoBlue else DuoBorder, RoundedCornerShape(8.dp))
                                .clickable { activeStepNumber = index + 1 }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(if (isStepActive) DuoBlue else DuoSurface1, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stepNum,
                                        color = if (isStepActive) Color.White else DuoInk,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DuoInk)
                            }
                            if (isStepActive) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = desc, fontSize = 11.sp, color = DuoInkMuted, lineHeight = 15.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "FORM READINESS CHECKLIST",
                        color = DuoInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )

                    val checklist = listOf(
                        "Overhand grip set outside shoulder-width",
                        "Shoulders packed down (anti-shrug)",
                        "Core braced & legs still (zero kipping swing)",
                        "Full ROM: Chin over bar to complete dead hang"
                    )

                    checklist.forEachIndexed { idx, item ->
                        val isChecked = checklistState.contains(idx)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, DuoBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    checklistState = if (isChecked) checklistState - idx else checklistState + idx
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(if (isChecked) DuoGreen else Color(0xFFE2E8F0), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Checked",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                color = if (isChecked) DuoInk else DuoInkMuted,
                                fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // TARGET REPEATS CARD (SET BY USER)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                .border(1.5.dp, DuoBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "TARGET REPEATS",
                        color = DuoBlue,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "Auto-completes workout when reached",
                        color = DuoInkMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .clickable { onDecrementTarget() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DuoInk)
                    }
                    Text(
                        text = "$targetReps",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoInk,
                        modifier = Modifier.padding(horizontal = 10.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                    Box(
                        modifier = Modifier
                            .background(DuoBlue, RoundedCornerShape(8.dp))
                            .size(34.dp)
                            .clickable { onIncrementTarget() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Preset Selection Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Presets:", fontSize = 11.sp, color = DuoInkMuted, fontWeight = FontWeight.Bold)
                listOf(5, 8, 10, 12, 15, 20).forEach { preset ->
                    val isSelectedPreset = targetReps == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelectedPreset) DuoBlue else Color.White)
                            .border(1.dp, if (isSelectedPreset) DuoBlue else DuoBorder, RoundedCornerShape(8.dp))
                            .clickable { onSelectPreset(preset) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$preset",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectedPreset) Color.White else DuoInk,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Guidance Callout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEBF5FF), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "💡 Tap 'START WORKOUT' below to begin your camera workout with real-time pose analysis!",
                color = DuoBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

data class MuscleData(
    val name: String,
    val role: String,
    val percentage: Int,
    val color: Color,
    val contraction: String,
    val tip: String
)

@Composable
fun ExerciseSkeletonDiagram(exerciseType: String, offset: Float) {
    Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f + offset * 1.5f
        // Head
        drawCircle(color = Color(0xFF334155), radius = 18.dp.toPx(), center = Offset(centerX, centerY - 60f))
        // Spine
        drawLine(color = Color(0xFF334155), start = Offset(centerX, centerY - 42f), end = Offset(centerX, centerY + 50f), strokeWidth = 8f)
        // Arms
        drawLine(color = Color(0xFF3B82F6), start = Offset(centerX, centerY - 30f), end = Offset(centerX - 40f, centerY - 10f), strokeWidth = 8f)
        drawLine(color = Color(0xFF3B82F6), start = Offset(centerX, centerY - 30f), end = Offset(centerX + 40f, centerY - 10f), strokeWidth = 8f)
        // Legs
        drawLine(color = Color(0xFF10B981), start = Offset(centerX, centerY + 50f), end = Offset(centerX - 30f, centerY + 120f), strokeWidth = 8f)
        drawLine(color = Color(0xFF10B981), start = Offset(centerX, centerY + 50f), end = Offset(centerX + 30f, centerY + 120f), strokeWidth = 8f)
    }
}

@Composable
fun ChinupVideoPlayer(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoUri = remember {
        Uri.parse("android.resource://${context.packageName}/${R.raw.chinup_animation}")
    }
    var activeVideoView by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                activeVideoView?.stopPlayback()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    activeVideoView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(videoUri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f) // Silent animation loop
                        start()
                    }
                    setOnCompletionListener {
                        start()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("ChinupVideoPlayer", "VideoView playback error: what=$what, extra=$extra")
                        true
                    }
                }
            },
            update = { videoView ->
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            }
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraWithPoseOverlay(
    exerciseType: String,
    userWeightKg: Double = 75.0,
    userHeightCm: Double = 175.0,
    onFrameAnalysis: (Int, String?, String, Boolean, String, PoseSkeleton?, Double, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    val previewView = remember(context) { PreviewView(context) }

    // Real-time local evaluation states
    val evaluator = remember { ExerciseFormEvaluator(userWeightKg, userHeightCm) }
    LaunchedEffect(userWeightKg, userHeightCm) {
        evaluator.updateBodyParams(userWeightKg, userHeightCm)
    }
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
                                onFrameAnalysis(
                                    result.score,
                                    result.mistake,
                                    result.feedback,
                                    result.isRepCompleted,
                                    result.exerciseState,
                                    skeleton,
                                    result.velocityMps,
                                    result.peakPowerW,
                                    result.speedLossPct
                                )
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
fun PullUpDashboardHUD(metrics: com.example.cv.PullUpMetrics, targetReps: Int = 10) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HUD
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${metrics.repCount}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                if (targetReps > 0) {
                    Text(" / $targetReps", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 14.dp, start = 4.dp))
                }
                Text(" REPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 14.dp, start = 8.dp))
            }
            
            if (targetReps > 0) {
                val progress = (metrics.repCount.toFloat() / targetReps).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = DuoGreen,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Box(modifier = Modifier
                .background(
                    if (metrics.phase == "HOLD" || metrics.phase == "TOP") DuoOrange 
                    else if (metrics.phase == "PULL") DuoGreen 
                    else DuoInk, 
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("STATE: ${metrics.phase}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("${metrics.speedLossPct} % SPEED VS REP 1", color = Color.White, fontSize = 12.sp)
            Text("${metrics.peakPowerW} W PEAK POWER", color = Color.White, fontSize = 12.sp)
            Text(String.format("+%.2f °C LATS · MODELLED", metrics.latsTempRise), color = Color.White, fontSize = 12.sp)
            Text("${metrics.latsFatiguedPct} % · ${metrics.bicepsFatiguedPct} % LATS · BICEPS FATIGUED", color = Color.White, fontSize = 12.sp)
            Text(String.format("≈ %.1f kcal / %.1f kJ WORK", metrics.totalKcal, metrics.totalHeatKj), color = Color.White, fontSize = 12.sp)
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
                            if (rep.swayCm < 10) Box(modifier = Modifier.background(DuoGreen, RoundedCornerShape(4.dp)).padding(4.dp)) { Text("STRICT NO SWING", color = Color.White, fontSize = 10.sp) }
                        }
                        Text(String.format("up %.1f s hold %.1f s down %.1f s", rep.durationConcentric, rep.durationHold, rep.durationEccentric), color = Color.White, fontSize = 12.sp)
                        Text(String.format("peak %.2f m/s %d W ≈ %.1f kcal", rep.peakVelocity, rep.peakPower.toInt(), rep.energyKcal), color = Color.White, fontSize = 12.sp)
                        Text(String.format("speed loss %d %% sway %.0f cm", rep.speedLossPct, rep.swayCm), color = Color.White, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .background(if (rep.fullLockout) DuoGreen else DuoOrange, RoundedCornerShape(8.dp))
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
