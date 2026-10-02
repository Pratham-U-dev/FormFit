package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserBodyProfile
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCard
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun BodyMetricsScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val bodyProfile by viewModel.userBodyProfile.collectAsState()

    var weightKg by remember(bodyProfile.weightKg) { mutableDoubleStateOf(bodyProfile.weightKg) }
    var heightCm by remember(bodyProfile.heightCm) { mutableDoubleStateOf(bodyProfile.heightCm) }
    var armLengthCm by remember(bodyProfile.armLengthCm) { mutableDoubleStateOf(bodyProfile.armLengthCm) }
    var age by remember(bodyProfile.age) { mutableIntStateOf(bodyProfile.age) }
    var gender by remember(bodyProfile.gender) { mutableStateOf(bodyProfile.gender) }
    var fitnessLevel by remember(bodyProfile.fitnessLevel) { mutableStateOf(bodyProfile.fitnessLevel) }
    var trainingGoal by remember(bodyProfile.trainingGoal) { mutableStateOf(bodyProfile.trainingGoal) }
    var vbtCutoffPct by remember(bodyProfile.vbtCutoffPct) { mutableIntStateOf(bodyProfile.vbtCutoffPct) }
    var isMetric by remember(bodyProfile.isMetric) { mutableStateOf(bodyProfile.isMetric) }

    var isSavedSnackbar by remember { mutableStateOf(false) }

    // BMI Calculation
    val heightM = heightCm / 100.0
    val bmi = if (heightM > 0) weightKg / heightM.pow(2) else 22.0
    val (bmiCategory, bmiColor) = when {
        bmi < 18.5 -> "Underweight" to DuoBlue
        bmi < 25.0 -> "Optimal / Normal" to DuoGreen
        bmi < 30.0 -> "Overweight" to DuoYellow
        else -> "High Mass / Athletic" to DuoOrange
    }

    // Effective lifted masses calculated for exercise physics
    val pullUpMass = (weightKg * 0.95).roundToInt()
    val squatMass = (weightKg * 0.88).roundToInt()
    val pushUpMass = (weightKg * 0.64).roundToInt()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("body_metrics_screen")
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = "BODY & BIOMETRICS",
                    color = DuoInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
                Text(
                    text = "Calibrates Force, Power (W), & VBT Speed",
                    color = DuoInkMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Unit Switcher (Metric kg/cm vs Imperial lbs/in)
            Row(
                modifier = Modifier
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .border(1.5.dp, DuoBorder, RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMetric) DuoBlue else Color.Transparent)
                        .clickable { isMetric = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("KG / CM", color = if (isMetric) Color.White else DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isMetric) DuoBlue else Color.Transparent)
                        .clickable { isMetric = false }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("LBS / IN", color = if (!isMetric) Color.White else DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HERO BIOMECHANICS SUMMARY CARD
        DuoCard(
            borderColor = DuoBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEBF5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Kinematics Profile", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DuoInk, maxLines = 1)
                            Text("Live AI vision calibration", fontSize = 10.5.sp, color = DuoInkMuted, maxLines = 1)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(bmiColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "BMI ${String.format("%.1f", bmi)} · $bmiCategory",
                            color = bmiColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // 3 Key Physics Metrics Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("PULL-UP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DuoInkMuted)
                        Text(if (isMetric) "$pullUpMass kg" else "${(pullUpMass * 2.20462).roundToInt()} lbs", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DuoInk, maxLines = 1)
                        Text("95% BW", fontSize = 9.sp, color = DuoGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("SQUAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DuoInkMuted)
                        Text(if (isMetric) "$squatMass kg" else "${(squatMass * 2.20462).roundToInt()} lbs", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DuoInk, maxLines = 1)
                        Text("88% BW", fontSize = 9.sp, color = DuoBlue)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("PUSH-UP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DuoInkMuted)
                        Text(if (isMetric) "$pushUpMass kg" else "${(pushUpMass * 2.20462).roundToInt()} lbs", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DuoInk, maxLines = 1)
                        Text("64% BW", fontSize = 9.sp, color = DuoOrange)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: BODY MEASUREMENTS
        Text(
            text = "BODY MEASUREMENTS",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = DuoInk,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // 1. WEIGHT ADJUSTER
        DuoCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Body Weight", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DuoInk)
                    Text("Force & Power calculation", fontSize = 11.sp, color = DuoInkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0))
                            .clickable { weightKg = (weightKg - 0.5).coerceAtLeast(30.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DuoInk)
                    }

                    Text(
                        text = if (isMetric) "${String.format("%.1f", weightKg)} kg" else "${String.format("%.1f", weightKg * 2.20462)} lbs",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DuoInk,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DuoBlue)
                            .clickable { weightKg = (weightKg + 0.5).coerceAtMost(220.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. HEIGHT ADJUSTER
        DuoCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Body Height", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DuoInk)
                    Text("Camera pixel scale", fontSize = 11.sp, color = DuoInkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0))
                            .clickable { heightCm = (heightCm - 1.0).coerceAtLeast(100.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DuoInk)
                    }

                    val feet = (heightCm / 30.48).toInt()
                    val inches = ((heightCm % 30.48) / 2.54).roundToInt()
                    Text(
                        text = if (isMetric) "${heightCm.roundToInt()} cm" else "${feet}ft ${inches}in",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DuoInk,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DuoBlue)
                            .clickable { heightCm = (heightCm + 1.0).coerceAtMost(240.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. ARM LENGTH / SPAN ADJUSTER
        DuoCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Arm Length", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DuoInk)
                    Text("Pull-up stroke calibration", fontSize = 11.sp, color = DuoInkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0))
                            .clickable { armLengthCm = (armLengthCm - 1.0).coerceAtLeast(40.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = DuoInk)
                    }

                    Text(
                        text = if (isMetric) "${armLengthCm.roundToInt()} cm" else "${(armLengthCm / 2.54).roundToInt()} in",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = DuoInk,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DuoBlue)
                            .clickable { armLengthCm = (armLengthCm + 1.0).coerceAtMost(100.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: VELOCITY BASED TRAINING (VBT) CONFIGURATION
        Text(
            text = "VELOCITY BASED TRAINING (VBT) TARGETS",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = DuoInk,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        DuoCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Velocity Loss Threshold", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DuoInk)
                Text(
                    text = "Controls when the AI advises ending your set based on rep speed loss vs Rep 1.",
                    fontSize = 11.sp,
                    color = DuoInkMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Grid for VBT pills
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            10 to "10% · Power",
                            20 to "20% · Hypertrophy"
                        ).forEach { (pct, label) ->
                            val isSelected = vbtCutoffPct == pct
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DuoGreen else Color(0xFFF1F5F9))
                                    .border(1.5.dp, if (isSelected) DuoGreenDark else DuoBorder, RoundedCornerShape(10.dp))
                                    .clickable { vbtCutoffPct = pct }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DuoInk,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            30 to "30% · Strength",
                            40 to "40% · Endurance"
                        ).forEach { (pct, label) ->
                            val isSelected = vbtCutoffPct == pct
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) DuoGreen else Color(0xFFF1F5F9))
                                    .border(1.5.dp, if (isSelected) DuoGreenDark else DuoBorder, RoundedCornerShape(10.dp))
                                    .clickable { vbtCutoffPct = pct }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DuoInk,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val vbtDesc = when (vbtCutoffPct) {
                    10 -> "⚡ Maximum Neural Power: Set terminates immediately when speed drops 10%. Prevents neuromuscular fatigue."
                    20 -> "💪 Optimal Hypertrophy: Ideal 20% velocity loss. Maximizes muscle tension while preserving form quality."
                    30 -> "🏋️ Pure Strength: Allows 30% speed decrease for maximal motor unit recruitment."
                    else -> "🔥 Muscular Endurance: Trains extreme metabolic fatigue resistance up to 40% speed drop."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(vbtDesc, fontSize = 11.sp, color = DuoInk, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 3: TRAINING GOAL & FITNESS LEVEL
        Text(
            text = "TRAINING PROFILE",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = DuoInk,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        DuoCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Primary Training Goal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DuoInk)
                Spacer(modifier = Modifier.height(8.dp))

                // 2x2 Grid for Training Goal
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hypertrophy", "Strength").forEach { goal ->
                            val isSelected = trainingGoal == goal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DuoBlue else Color(0xFFF8FAFC))
                                    .border(1.5.dp, if (isSelected) DuoBlueDark else DuoBorder, RoundedCornerShape(8.dp))
                                    .clickable { trainingGoal = goal }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = goal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DuoInk,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Power & Speed", "Endurance").forEach { goal ->
                            val isSelected = trainingGoal == goal || (goal == "Power & Speed" && trainingGoal == "Power")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DuoBlue else Color(0xFFF8FAFC))
                                    .border(1.5.dp, if (isSelected) DuoBlueDark else DuoBorder, RoundedCornerShape(8.dp))
                                    .clickable { trainingGoal = if (goal == "Power & Speed") "Power" else goal }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = goal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DuoInk,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Experience Level", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DuoInk)
                Spacer(modifier = Modifier.height(8.dp))

                // 2x2 Grid for Experience Level
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Beginner", "Intermediate").forEach { level ->
                            val isSelected = fitnessLevel == level
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DuoGreen else Color(0xFFF8FAFC))
                                    .border(1.5.dp, if (isSelected) DuoGreenDark else DuoBorder, RoundedCornerShape(8.dp))
                                    .clickable { fitnessLevel = level }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DuoInk,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Advanced", "Elite Athlete").forEach { level ->
                            val isSelected = fitnessLevel == level || (level == "Elite Athlete" && fitnessLevel == "Elite")
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DuoGreen else Color(0xFFF8FAFC))
                                    .border(1.5.dp, if (isSelected) DuoGreenDark else DuoBorder, RoundedCornerShape(8.dp))
                                    .clickable { fitnessLevel = if (level == "Elite Athlete") "Elite" else level }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else DuoInk,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SAVE BUTTON
        DuoButton(
            onClick = {
                val updated = UserBodyProfile(
                    weightKg = weightKg,
                    heightCm = heightCm,
                    armLengthCm = armLengthCm,
                    age = age,
                    gender = gender,
                    fitnessLevel = fitnessLevel,
                    trainingGoal = trainingGoal,
                    vbtCutoffPct = vbtCutoffPct,
                    isMetric = isMetric
                )
                viewModel.updateUserBodyProfile(updated)
                isSavedSnackbar = true
            },
            backgroundColor = DuoGreen,
            shadowColor = DuoGreenDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_body_metrics_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE BIOMETRIC PROFILE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }

        if (isSavedSnackbar) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓ Biometrics & VBT calibration saved to device!",
                    color = DuoGreenDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
