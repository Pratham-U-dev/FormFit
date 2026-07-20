package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DuoCard
import com.example.ui.components.DuoProgressBar
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel

@Composable
fun ProfileScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.userStats.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val badges = viewModel.badgesList

    val unlockedSet = remember(stats.unlockedBadgesCsv) {
        stats.unlockedBadgesCsv.split(",").filter { it.isNotEmpty() }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // PROFILE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Giant Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(DuoBlue, shape = CircleShape)
                    .border(3.dp, DuoBlueDark, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("💪", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "FormFit Athlete",
                    color = DuoInk,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Level ${stats.level} Coach",
                    color = DuoInkMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Streak
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stats.streakDays} Day Workout Streak",
                        color = DuoOrange,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // STATS PROGRESS CARD
        DuoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            val neededXp = stats.level * 100
            val progress = if (neededXp > 0) stats.currentXp.toFloat() / neededXp else 0f
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "XP PROGRESS",
                        color = DuoInkMuted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${stats.currentXp} / $neededXp XP",
                        color = DuoYellow,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                DuoProgressBar(
                    progress = progress,
                    fillColor = DuoYellow,
                    height = 14.dp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Earn ${neededXp - stats.currentXp} more XP to reach Level ${stats.level + 1}!",
                    color = DuoInkMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // BADGES SHELF TITLE
        Text(
            text = "ACHIEVEMENTS SHELF",
            color = DuoInkMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // BADGES SHELF GRID
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(badges) { badge ->
                val isUnlocked = unlockedSet.contains(badge.id)
                val cardAlpha = if (isUnlocked) 1.0f else 0.5f
                val border = if (isUnlocked) DuoGreen else DuoBorder

                DuoCard(
                    modifier = Modifier.alpha(cardAlpha),
                    backgroundColor = if (isUnlocked) Color.White else DuoSurface1,
                    borderColor = border
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (isUnlocked) Color(0xFFFFF9C4) else DuoSurface2,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isUnlocked) badge.icon else "🔒",
                                fontSize = 26.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = badge.title,
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = badge.description,
                            color = DuoInkMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
