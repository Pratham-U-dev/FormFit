package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.DuoProgressBar
import com.example.ui.theme.DuoBlue
import com.example.ui.theme.DuoBlueDark
import com.example.ui.theme.DuoBorder
import com.example.ui.theme.DuoGreen
import com.example.ui.theme.DuoGreenDark
import com.example.ui.theme.DuoInk
import com.example.ui.theme.DuoInkMuted
import com.example.ui.theme.DuoOrange
import com.example.ui.theme.DuoSurface1
import com.example.ui.theme.DuoYellow
import com.example.viewmodel.WorkoutViewModel

@Composable
fun DashboardScreen(
    viewModel: WorkoutViewModel,
    onStartWorkout: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.userStats.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP STATS HEADER
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Level & XP Progress
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(DuoYellow, shape = CircleShape)
                            .border(2.dp, Color(0xFFDCAE00), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stats.level}",
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val neededXp = stats.level * 100
                        val progress = if (neededXp > 0) stats.currentXp.toFloat() / neededXp else 0f
                        Text(
                            text = "Level ${stats.level} Coach",
                            color = DuoInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        DuoProgressBar(
                            progress = progress,
                            fillColor = DuoYellow,
                            modifier = Modifier.width(120.dp).height(10.dp)
                        )
                    }
                }

                // Daily Streak Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFFFF2E0), shape = RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFFFFE0B2), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.streakDays} Day Streak",
                        color = DuoOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // HERO BANNER WITH THEMED DUMBBELL LOGO
        item {
            DuoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFF1F8E9),
                borderColor = DuoGreen
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DUO'S IRON TEMPLE",
                            color = DuoGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lift with perfect form to keep Duo happy and earn double XP!",
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    com.example.ui.components.DuoDumbbellLogo(
                        modifier = Modifier.size(72.dp)
                    )
                }
            }
        }

        // DUO THE OWL SPEECH BUBBLE
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Duo Mascot
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(DuoGreen, shape = CircleShape)
                        .border(3.dp, DuoGreenDark, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🦉",
                        fontSize = 42.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speech Bubble
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(DuoSurface1, shape = RoundedCornerShape(18.dp))
                        .border(2.dp, DuoBorder, shape = RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Hey there, Champ! Practice makes perfect. Choose an exercise below and let's polish your form!",
                        color = DuoInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // STATS SUMMARY CARD
        item {
            DuoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DuoSurface1
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Workouts", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${sessions.size}", color = DuoInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Total Reps", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val totalReps = sessions.sumOf { it.totalReps }
                        Text("$totalReps", color = DuoInk, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("Avg Score", color = DuoInkMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val avgScore = if (sessions.isNotEmpty()) sessions.map { it.averageScore }.average().toInt() else 0
                        Text("$avgScore%", color = DuoGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                }
            }
        }

        // TITLE SECTION
        item {
            Text(
                text = "CHOOSE YOUR WORKOUT",
                color = DuoInkMuted,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // WORKOUT MODULES
        val exercises = listOf(
            Triple("Squat", "Squat Training", "🏋️‍♂️"),
            Triple("Push-up", "Push-up Core", "💪"),
            Triple("Lunge", "Lunge Balance", "🏃"),
            Triple("Plank", "Plank Endurance", "🧘")
        )

        exercises.forEach { (type, title, emoji) ->
            item {
                DuoCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp))
                                    .border(2.dp, Color(0xFFC8E6C9), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 28.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = title,
                                    color = DuoInk,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = when(type) {
                                        "Squat" -> "Perfect your lower body squat depth."
                                        "Push-up" -> "Build standard chest and elbow posture."
                                        "Lunge" -> "Master leg symmetry and knee line."
                                        else -> "Hold horizontal spine alignment."
                                    },
                                    color = DuoInkMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        DuoButton(
                            onClick = { onStartWorkout(type) },
                            backgroundColor = DuoBlue,
                            shadowColor = DuoBlueDark,
                            modifier = Modifier.width(90.dp).height(44.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            testTag = "start_button_${type.lowercase()}"
                        ) {
                            Text(
                                "START",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
