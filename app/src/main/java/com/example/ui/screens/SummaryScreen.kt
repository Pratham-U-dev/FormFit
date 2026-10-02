package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.WorkoutViewModel

// Custom colors
val DarkBackground = Color(0xFF131722)
val CardBackground = Color(0xFF202638)
val AccentGreen = Color(0xFF1FD57E)
val TextGray = Color(0xFF8692A6)
val TextWhite = Color(0xFFF3F4F6)
val AlertRed = Color(0xFFEF4444)
val WarningOrange = Color(0xFFF59E0B)
val DuoBlueAccent = Color(0xFF3B82F6)

@Composable
fun SummaryScreen(
    viewModel: WorkoutViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by viewModel.lastCompletedSession.collectAsState()
    val isPersonalBest by viewModel.isPersonalBest.collectAsState()
    val repScores by viewModel.lastSessionRepScores.collectAsState()
    val mistakes by viewModel.lastSessionMistakes.collectAsState()
    val calories by viewModel.lastSessionCalories.collectAsState()
    val peakPower by viewModel.lastSessionPeakPower.collectAsState()
    
    val safeSession = session ?: return
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trophy Icon
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Trophy",
            tint = AccentGreen,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Titles
        Text(
            text = "Workout Complete",
            color = TextWhite,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "${safeSession.exerciseType.uppercase()} · Biomechanics Analytics",
            color = TextGray,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic Achievement Banner (Calculated from real session performance)
        val bannerBg = if (isPersonalBest) Color(0xFF113227) else if (safeSession.averageScore >= 90) Color(0xFF1E293B) else Color(0xFF1F2430)
        val bannerBorder = if (isPersonalBest) AccentGreen else if (safeSession.averageScore >= 90) DuoBlueAccent else TextGray.copy(alpha = 0.4f)
        val bannerIcon = if (isPersonalBest) Icons.Default.EmojiEvents else if (safeSession.averageScore >= 90) Icons.Default.Star else Icons.Default.CheckCircle
        val bannerIconTint = if (isPersonalBest) AccentGreen else if (safeSession.averageScore >= 90) WarningOrange else AccentGreen

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerBg, shape = RoundedCornerShape(16.dp))
                .border(1.5.dp, bannerBorder.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = bannerIcon,
                    contentDescription = "Status Icon",
                    tint = bannerIconTint,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    val titleText = when {
                        isPersonalBest -> "New Personal Best!"
                        safeSession.averageScore >= 90 && safeSession.totalReps >= 3 -> "Form Master: 90%+ Accuracy"
                        safeSession.totalReps > 0 -> "Target Reps Completed"
                        else -> "Session Recorded"
                    }
                    val subtitleText = when {
                        isPersonalBest -> "New rep record: ${safeSession.totalReps} completed repetitions!"
                        safeSession.averageScore >= 90 && safeSession.totalReps >= 3 -> "Elite biomechanical control maintained throughout."
                        safeSession.totalReps > 0 -> "Completed ${safeSession.totalReps} reps in ${safeSession.durationSeconds}s."
                        else -> "Movement logged. Keep training for progress."
                    }
                    Text(
                        text = titleText,
                        color = bannerIconTint,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2x2 Grid of Real Session Metrics
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Total Reps",
                    value = safeSession.totalReps.toString(),
                    subtitle = if (safeSession.exerciseType == "Plank") "Seconds held" else "Reps completed",
                    icon = Icons.Default.Repeat,
                    modifier = Modifier.weight(1f)
                )
                
                val avgTime = if (safeSession.totalReps > 0) safeSession.durationSeconds.toFloat() / safeSession.totalReps else 0f
                StatCard(
                    title = "Average Pace",
                    value = if (avgTime > 0) String.format("%.1fs", avgTime) else "--",
                    subtitle = "Tempo per rep",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Form Score",
                    value = if (safeSession.averageScore > 0) "${safeSession.averageScore}%" else "--",
                    subtitle = when {
                        safeSession.averageScore >= 90 -> "Technique: Excellent"
                        safeSession.averageScore >= 80 -> "Technique: Good"
                        safeSession.averageScore > 0 -> "Technique: Needs Work"
                        else -> "No reps scored"
                    },
                    icon = Icons.Default.CheckCircle,
                    iconTint = if (safeSession.averageScore >= 85) AccentGreen else WarningOrange,
                    modifier = Modifier.weight(1f)
                )
                
                val m = safeSession.durationSeconds / 60
                val s = safeSession.durationSeconds % 60
                StatCard(
                    title = "Session Time",
                    value = String.format("%02d:%02d", m, s),
                    subtitle = "Total elapsed time",
                    icon = Icons.Default.AccessTime,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Energy Burned",
                    value = String.format("%.1f kcal", calories),
                    subtitle = if (peakPower > 0) "$peakPower W Peak Power" else "Active metabolic work",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = WarningOrange,
                    modifier = Modifier.weight(1f)
                )
                
                StatCard(
                    title = "Form Breaks",
                    value = safeSession.mistakeCount.toString(),
                    subtitle = if (safeSession.mistakeCount == 0) "Zero technique errors" else "Detected kinematic faults",
                    icon = Icons.Default.WarningAmber,
                    iconTint = if (safeSession.mistakeCount > 0) AlertRed else TextGray,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dynamic Rep-by-Rep Form Accuracy Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REP-BY-REP FORM ACCURACY (%)",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (repScores.isNotEmpty()) "${repScores.size} REPS ANALYZED" else "NO REPS",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (repScores.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            // Horizontal grid guide lines
                            val gridLines = listOf(1.0f, 0.75f, 0.5f, 0.25f, 0.0f)
                            gridLines.forEach { frac ->
                                val y = h * (1f - frac)
                                drawLine(
                                    color = TextGray.copy(alpha = 0.15f),
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }
                            
                            val count = repScores.size
                            val barSpacing = w / (count + 0.5f)
                            val barWidth = (barSpacing * 0.6f).coerceIn(12f, 40f)
                            
                            repScores.forEachIndexed { index, score ->
                                val xCenter = (index + 0.8f) * barSpacing
                                val barHeight = (score / 100f) * (h * 0.85f)
                                val yTop = h - barHeight
                                
                                val barColor = when {
                                    score >= 85 -> AccentGreen
                                    score >= 70 -> WarningOrange
                                    else -> AlertRed
                                }
                                
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(xCenter - barWidth / 2f, yTop),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rep 1", color = TextGray, fontSize = 11.sp)
                        if (repScores.size > 1) {
                            Text("Rep ${repScores.size}", color = TextGray, fontSize = 11.sp)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No completed repetitions recorded in this session.",
                            color = TextGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Detected Faults & Form Coaching
        val uniqueMistakes = mistakes.distinct()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "BIOMECHANICAL FEEDBACK",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (uniqueMistakes.isNotEmpty()) {
                    uniqueMistakes.forEach { mistakeItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mistakeItem,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clean execution! No technique faults detected.",
                            color = AccentGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bottom Button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Back to Dashboard",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = TextGray,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardBackground, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = TextGray.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}
