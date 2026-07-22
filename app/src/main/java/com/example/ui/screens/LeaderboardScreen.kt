package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DuoCard
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel

@Composable
fun LeaderboardScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.leaderboard.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // LEADERBOARD HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ONLINE LEADERBOARD",
                color = DuoInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )

            // Live Indicator Badge
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DuoGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🟢", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        color = DuoGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Text(
            text = "Top form trainers online. Complete workouts to rank up!",
            color = DuoInkMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = DuoGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading Live Leaderboard...",
                        color = DuoInkMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries) { entry ->
                val isRank1 = entry.rank == 1
                val isRank2 = entry.rank == 2
                val isRank3 = entry.rank == 3

                val rowBg = if (entry.isUser) Color(0xFFF1F8E9) else Color.White
                val rowBorder = if (entry.isUser) DuoGreen else DuoBorder

                DuoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = rowBg,
                    borderColor = rowBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Rank Number
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        when {
                                            isRank1 -> DuoYellow
                                            isRank2 -> Color(0xFFE0E0E0)
                                            isRank3 -> Color(0xFFCD7F32)
                                            else -> Color.Transparent
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRank1) "👑" else if (isRank2) "🥈" else if (isRank3) "🥉" else "${entry.rank}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DuoInk,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Avatar Circle
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(DuoSurface2, shape = CircleShape)
                                    .border(2.dp, DuoBorder, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(entry.characterIcon, fontSize = 24.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name
                            Column {
                                Text(
                                    text = entry.name,
                                    color = DuoInk,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                if (entry.isUser) {
                                    Text(
                                        text = "You",
                                        color = DuoGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // XP display
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${entry.xp} XP",
                                color = DuoInk,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}
