package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCard
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel

@Composable
fun SummaryScreen(
    viewModel: WorkoutViewModel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by viewModel.lastCompletedSession.collectAsState()

    val safeSession = session ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // CELEBRATION mascot
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(DuoGreen, shape = CircleShape)
                .border(4.dp, DuoGreenDark, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🦉", fontSize = 54.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WORKOUT COMPLETE!",
            color = DuoGreen,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your muscle memory is growing stronger!",
            color = DuoInkMuted,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // XP EARNED POPUP
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFDE7), shape = RoundedCornerShape(20.dp))
                .border(3.dp, DuoYellow, shape = RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "+${safeSession.xpEarned} XP EARNED!",
                    color = Color(0xFFDCAE00),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // GRID STATS CARDS
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Reps
                DuoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = DuoSurface1
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL ACTIVITY", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(
                            text = if (safeSession.exerciseType == "Plank") "${safeSession.totalReps}s Hold" else "${safeSession.totalReps} Reps",
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Duration
                DuoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = DuoSurface1
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DURATION", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        val m = safeSession.durationSeconds / 60
                        val s = safeSession.durationSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", m, s),
                            color = DuoBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Average Form Score
                DuoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = if (safeSession.averageScore >= 90) Color(0xFFE8F5E9) else DuoSurface1,
                    borderColor = if (safeSession.averageScore >= 90) DuoGreen else DuoBorder
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AVG FORM SCORE", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(
                            text = "${safeSession.averageScore}%",
                            color = if (safeSession.averageScore >= 90) DuoGreen else DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Mistakes Caught
                DuoCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = if (safeSession.mistakeCount > 0) Color(0xFFFFEBEE) else DuoSurface1,
                    borderColor = if (safeSession.mistakeCount > 0) DuoRed else DuoBorder
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MISTAKES DETECTED", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(
                            text = "${safeSession.mistakeCount}",
                            color = if (safeSession.mistakeCount > 0) DuoRed else DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic coaching suggestion card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DuoSurface1, shape = RoundedCornerShape(16.dp))
                .border(2.dp, DuoBorder, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Text(
                text = when {
                    safeSession.averageScore >= 90 -> "🎯 Duo says: \"Amazing! Your posture is immaculate. Keep up the perfect form!\""
                    safeSession.mistakeCount > 3 -> "💡 Duo says: \"You're doing great! Try going slightly slower next time to keep your joints stable.\""
                    else -> "💪 Duo says: \"Solid effort! Each session trains your muscle memory. Let's practice again tomorrow!\""
                },
                color = DuoInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CONTINUE CTA
        DuoButton(
            onClick = onContinue,
            backgroundColor = DuoGreen,
            shadowColor = DuoGreenDark,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            testTag = "summary_continue_button"
        ) {
            Text(
                "CONTINUE",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
