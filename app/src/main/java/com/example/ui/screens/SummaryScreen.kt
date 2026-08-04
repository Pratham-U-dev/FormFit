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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.WorkoutViewModel

// Custom colors based on the screenshot
val DarkBackground = Color(0xFF131722)
val CardBackground = Color(0xFF202638)
val AccentGreen = Color(0xFF1FD57E)
val TextGray = Color(0xFF8692A6)
val TextWhite = Color(0xFFF3F4F6)
val AlertRed = Color(0xFFEF4444)
val GraphLineColor = Color(0xFF3B82F6)

@Composable
fun SummaryScreen(
    viewModel: WorkoutViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by viewModel.lastCompletedSession.collectAsState()
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
            text = "${safeSession.exerciseType} · Session Stats",
            color = TextGray,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // New Personal Best Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF113227), shape = RoundedCornerShape(16.dp))
                .border(1.5.dp, AccentGreen.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "PB Trophy",
                    tint = AccentGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "New Personal Best!",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "New rep record — you're getting stronger!",
                        color = TextGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2x2 Grid of Stats
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
                    subtitle = "Completed this session",
                    icon = Icons.Default.Repeat,
                    modifier = Modifier.weight(1f)
                )
                
                val avgTime = if (safeSession.totalReps > 0) safeSession.durationSeconds.toFloat() / safeSession.totalReps else 0f
                StatCard(
                    title = "Avg Time / Rep",
                    value = String.format("%.1fs", avgTime),
                    subtitle = "Seconds per rep",
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Approximate faults based on score
                val faults = if (safeSession.averageScore >= 95) 0.0 else if (safeSession.averageScore >= 80) 0.5 else 1.2
                StatCard(
                    title = "Form Breaks / Rep",
                    value = String.format("%.1f", faults),
                    subtitle = "Avg faults per rep",
                    icon = Icons.Default.WarningAmber,
                    iconTint = if (faults > 0) AlertRed else TextGray,
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
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Velocity Telemetry Card
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
                        text = "VELOCITY TELEMETRY",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    // Icon placeholder for expand
                    Icon(
                        imageVector = Icons.Default.Repeat, // Reusing icon for visual placeholder
                        contentDescription = "Expand",
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Graph mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        // Draw background horizontal lines
                        val lineCount = 4
                        for (i in 0 until lineCount) {
                            val y = height * (i / (lineCount - 1).toFloat())
                            drawLine(
                                color = TextGray.copy(alpha = 0.2f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }
                        
                        // Draw filled areas (bottom brown/yellow, middle green/blue overlay)
                        drawRect(
                            color = Color(0xFF5E492B).copy(alpha = 0.5f),
                            topLeft = Offset(0f, height * 0.7f),
                            size = androidx.compose.ui.geometry.Size(width, height * 0.3f)
                        )
                        drawRect(
                            color = Color(0xFF1E3A8A).copy(alpha = 0.3f),
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(width, height * 0.7f)
                        )
                        
                        // Draw the telemetry line
                        val path = Path()
                        path.moveTo(0f, height * 0.95f)
                        path.lineTo(width * 0.1f, height * 0.93f)
                        path.lineTo(width * 0.2f, height * 0.96f)
                        path.lineTo(width * 0.3f, height * 0.94f)
                        path.lineTo(width * 0.4f, height * 0.95f)
                        path.lineTo(width * 0.5f, height * 0.92f)
                        path.lineTo(width * 0.6f, height * 0.95f)
                        
                        // The big spike
                        path.lineTo(width * 0.65f, height * 0.1f)
                        path.lineTo(width * 0.7f, height * 0.95f)
                        
                        path.lineTo(width * 0.8f, height * 0.93f)
                        path.lineTo(width * 0.9f, height * 0.96f)
                        path.lineTo(width, height * 0.94f)
                        
                        drawPath(
                            path = path,
                            color = GraphLineColor,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        // Draw the active tracking dot
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = Offset(width * 0.62f, height * 0.5f)
                        )
                    }
                    
                    Text(
                        text = "1.7 u/s",
                        color = TextWhite,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                    Text(
                        text = "0.0 u/s",
                        color = TextWhite,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 8.dp, bottom = 8.dp)
                    )
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

