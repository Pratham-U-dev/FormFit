package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel

@Composable
fun HeartsDialog(
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF131F24)),
            color = Color(0xFF131F24)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_hearts_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Text(
                        text = "Hearts & Energy",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )

                    // Gems Balance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("💎", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.gems}",
                            color = Color(0xFF49C0F8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                if (statusMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E3A2F), RoundedCornerShape(10.dp))
                            .border(1.dp, DuoGreen, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusMessage ?: "",
                            color = DuoGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO HEARTS VISUAL
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                                .border(1.5.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (stats.isSuperSubscriber) {
                                    Text("💖", fontSize = 54.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "UNLIMITED HEARTS",
                                        color = Color(0xFFFFC800),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "Super FormFit Member",
                                        color = Color(0xFF8596A0),
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (i in 1..stats.maxHearts) {
                                            Text(
                                                text = if (i <= stats.hearts) "❤️" else "🖤",
                                                fontSize = 32.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "${stats.hearts} / ${stats.maxHearts} HEARTS",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = if (stats.hearts == stats.maxHearts) "You have full energy!" else "Refills 1 heart every 30 minutes",
                                        color = Color(0xFF8596A0),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // SECTION: SUPER FORMFIT PRO SUBSCRIPTION (BUSINESS DEMO SHOWCASE)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B1D54), Color(0xFF1B2A4A))
                                    ),
                                    RoundedCornerShape(18.dp)
                                )
                                .border(2.dp, Color(0xFFFFC800), RoundedCornerShape(18.dp))
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFC800), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "SUPER FORMFIT",
                                            color = Color(0xFF18282E),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = if (stats.isSuperSubscriber) "ACTIVE ✓" else "PRO TIER",
                                        color = if (stats.isSuperSubscriber) DuoGreen else Color(0xFFFFC800),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Accelerate your fitness with Super",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                SuperFeatureRow("💖 Unlimited workout hearts & infinite practice")
                                SuperFeatureRow("🥗 Unlimited AI meal macro recognition (Gemini Vision)")
                                SuperFeatureRow("📊 Advanced Biomechanics heat & fatigue modeling")
                                SuperFeatureRow("🛡️ Automatic 2x streak freeze insurance")

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val newState = !stats.isSuperSubscriber
                                        viewModel.toggleSuperSubscription(newState)
                                        statusMessage = if (newState) "✓ Super FormFit Activated! Enjoy unlimited hearts." else "Super subscription canceled."
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("super_formfit_trial_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (stats.isSuperSubscriber) Color(0xFF202F36) else Color(0xFFFFC800)
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = if (stats.isSuperSubscriber) "MANAGE SUBSCRIPTION (ACTIVE)" else "TRY 7 DAYS FREE · $7.99/mo (DEMO)",
                                        color = if (stats.isSuperSubscriber) Color.White else Color(0xFF18282E),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // SECTION: REFILL OPTIONS
                    if (!stats.isSuperSubscriber && stats.hearts < stats.maxHearts) {
                        item {
                            Text(
                                text = "Refill Energy",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                                    .border(1.5.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("❤️", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Full Refill",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Restore all 5 hearts immediately",
                                                color = Color(0xFF8596A0),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.refillHeartsWithGems { success ->
                                                statusMessage = if (success) "✓ Hearts fully restored to 5/5!" else "⚠️ Need 350 gems to refill hearts."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49C0F8)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("350 💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                                    .border(1.5.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🧘", fontSize = 28.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Practice to Earn",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "Warm up and regain +1 heart",
                                                color = Color(0xFF8596A0),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.practiceToEarnHeart()
                                            statusMessage = "✓ Practice complete! +1 Heart added."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DuoGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("PRACTICE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SuperFeatureRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
