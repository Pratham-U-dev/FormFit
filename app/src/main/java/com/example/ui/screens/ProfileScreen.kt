package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DuoButton
import com.example.ui.components.DuoCard
import com.example.ui.components.DuoProgressBar
import com.example.ui.theme.*
import com.example.viewmodel.WorkoutViewModel

@Composable
fun ProfileScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stats by viewModel.userStats.collectAsState()
    val sessions by viewModel.allSessions.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isVirtualCoachMode by viewModel.isVirtualCoachMode.collectAsState()
    val aiApiKey by viewModel.aiApiKey.collectAsState()
    val badges = viewModel.badgesList

    var apiKeyInput by remember(aiApiKey) { mutableStateOf(aiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var isKeySavedMessage by remember { mutableStateOf(false) }

    val unlockedSet = remember(stats.unlockedBadgesCsv) {
        stats.unlockedBadgesCsv.split(",").filter { it.isNotEmpty() }.toSet()
    }

    val badgePairs = remember(badges) { badges.chunked(2) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
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

        Spacer(modifier = Modifier.height(4.dp))

        // APP SETTINGS SECTION
        Text(
            text = "SETTINGS",
            color = DuoInkMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
        )

        DuoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Column {
                // Sound Effects Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isSoundEnabled) "🔊" else "🔇", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sound Effects",
                                color = DuoInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isSoundEnabled) "Audio feedback ON" else "Audio feedback OFF",
                                color = DuoInkMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.setSoundEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DuoGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = DuoBorder
                        ),
                        modifier = Modifier.testTag("sound_effects_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(DuoBorder)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Virtual Coach Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🤖", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Virtual Coach Simulation",
                                color = DuoInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isVirtualCoachMode) "Simulated workout ON" else "Camera / Sim mode",
                                color = DuoInkMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isVirtualCoachMode,
                        onCheckedChange = { enabled ->
                            viewModel.setVirtualCoachMode(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DuoBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = DuoBorder
                        ),
                        modifier = Modifier.testTag("virtual_coach_switch")
                    )
                }
            }
        }

        // SMART VISION RECOGNITION KEY SECTION
        Text(
            text = "AI VISION RECOGNITION KEY",
            color = DuoInkMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
        )

        DuoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🧠", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Vision API Key",
                                color = DuoInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (aiApiKey.isNotBlank()) "Cloud AI Recognition Active" else "Offline Local Vision Mode",
                                color = if (aiApiKey.isNotBlank()) DuoGreen else DuoInkMuted,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Mode Badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (aiApiKey.isNotBlank()) DuoGreen.copy(alpha = 0.15f) else DuoSurface2,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (aiApiKey.isNotBlank()) DuoGreen else DuoBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (aiApiKey.isNotBlank()) "✨ Cloud Mode" else "⚡ Local Mode",
                            color = if (aiApiKey.isNotBlank()) DuoGreenDark else DuoInkMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        isKeySavedMessage = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_api_key_input"),
                    label = { Text("API Key", color = DuoInkMuted) },
                    placeholder = { Text("Paste your API Key here...", color = DuoInkMuted) },
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Key Visibility",
                                tint = DuoInkMuted
                            )
                        }
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = DuoInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DuoInk,
                        unfocusedTextColor = DuoInk,
                        focusedContainerColor = DuoSurface1,
                        unfocusedContainerColor = DuoSurface1,
                        focusedBorderColor = DuoGreen,
                        unfocusedBorderColor = DuoBorder,
                        focusedLabelColor = DuoGreen,
                        unfocusedLabelColor = DuoInkMuted,
                        cursorColor = DuoGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save Button
                    DuoButton(
                        onClick = {
                            viewModel.saveAiApiKey(apiKeyInput)
                            isKeySavedMessage = true
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = DuoGreen,
                        borderColor = DuoGreenDark,
                        shadowColor = DuoGreenDark,
                        testTag = "save_ai_api_key_button"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isKeySavedMessage) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SAVED!", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            } else {
                                Text("SAVE KEY", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }

                    // Get Free Key Direct Link Button
                    DuoButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback: copy to clipboard
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("API Key URL", "https://aistudio.google.com/app/apikey")
                                clipboard.setPrimaryClip(clip)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = DuoSurface1,
                        borderColor = DuoBorder,
                        shadowColor = DuoBorder,
                        testTag = "get_api_key_link_button"
                    ) {
                        Text(
                            text = "GET KEY ↗",
                            color = DuoInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guidance Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DuoBlue.copy(alpha = 0.08f), shape = RoundedCornerShape(10.dp))
                        .border(1.dp, DuoBlue.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 How to enable Cloud AI Plate Recognition on your phone:",
                            color = DuoInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Click 'GET KEY ↗' to open aistudio.google.com/app/apikey\n2. Create a free API key & paste it in the box above\n3. Click SAVE KEY to activate cloud AI meal recognition!",
                            color = DuoInkMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // BADGES SHELF TITLE
        Text(
            text = "ACHIEVEMENTS SHELF",
            color = DuoInkMuted,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // BADGES SHELF GRID (unrolled into rows for smooth page scrolling)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            badgePairs.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { badge ->
                        val isUnlocked = unlockedSet.contains(badge.id)
                        val cardAlpha = if (isUnlocked) 1.0f else 0.5f
                        val border = if (isUnlocked) DuoGreen else DuoBorder

                        DuoCard(
                            modifier = Modifier
                                .weight(1f)
                                .alpha(cardAlpha),
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
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RESET PROGRESS BUTTON (Positioned cleanly after the achievements section)
        DuoButton(
            onClick = { viewModel.resetAllData() },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White,
            borderColor = DuoRed,
            shadowColor = DuoBorder,
            testTag = "reset_progress_debug_button"
        ) {
            Text(
                text = "RESET PROGRESS (DEBUG)",
                color = DuoRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

