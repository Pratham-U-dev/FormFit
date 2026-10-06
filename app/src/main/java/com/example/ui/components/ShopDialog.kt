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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun ShopDialog(
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    var purchaseSuccessMessage by remember { mutableStateOf<String?>(null) }

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
                        modifier = Modifier.testTag("close_shop_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Text(
                        text = "Shop",
                        color = Color(0xFF8596A0),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )

                    // Gems Balance Counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("💎", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.gems}",
                            color = Color(0xFF49C0F8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (purchaseSuccessMessage != null) {
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
                            text = purchaseSuccessMessage ?: "",
                            color = DuoGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // SECTION 1: STREAK ITEMS
                    item {
                        Text(
                            text = "Streak",
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(Color(0xFF202F36), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🧊🔥", fontSize = 28.sp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Streak Freeze",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Protect your streak if you miss a day of practice. Equip up to 2 at once.",
                                        color = Color(0xFF8596A0),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (stats.streakFreezesEquipped >= 2) {
                                        Text(
                                            text = "2 / 2 EQUIPPED",
                                            color = DuoGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.buyStreakFreeze { success ->
                                                    purchaseSuccessMessage = if (success) "✓ Streak Freeze equipped!" else "⚠️ Need 200 gems to equip Streak Freeze"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49C0F8)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("EQUIP · 200 💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: GEMS PACKS (BUSINESS / MONETIZATION DEMO)
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gems",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "DEMO STORE",
                                    color = Color(0xFFFFC800),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "Instant top-up for power-ups and cosmetics (Demo Purchases active)",
                                color = Color(0xFF8596A0),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GemPackCard(
                                icon = "🎁",
                                amount = 1200,
                                price = "₹89.00 ($0.99)",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.buyGemsPack(1200, "₹89.00")
                                    purchaseSuccessMessage = "✓ Demo In-App Purchase Successful! +1,200 💎 credited."
                                }
                            )
                            GemPackCard(
                                icon = "🛢️",
                                amount = 3000,
                                price = "₹179.00 ($1.99)",
                                isPopular = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.buyGemsPack(3000, "₹179.00")
                                    purchaseSuccessMessage = "✓ Demo In-App Purchase Successful! +3,000 💎 credited."
                                }
                            )
                            GemPackCard(
                                icon = "🛒",
                                amount = 6500,
                                price = "₹349.00 ($3.99)",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.buyGemsPack(6500, "₹349.00")
                                    purchaseSuccessMessage = "✓ Demo In-App Purchase Successful! +6,500 💎 credited."
                                }
                            )
                        }
                    }

                    // SECTION 3: POWER-UPS
                    item {
                        Text(
                            text = "Power-Ups",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }

                    item {
                        PowerUpCard(
                            icon = "⏱️",
                            title = "Timer Boost",
                            description = "Add extra time and beat the clock on timed challenges!",
                            gemCost = 450,
                            iconBg = Color(0xFFD67EFF),
                            onClick = {
                                viewModel.buyPowerUp("Timer Boost", 450) { success ->
                                    purchaseSuccessMessage = if (success) "✓ Timer Boost active for next timed set!" else "⚠️ Need 450 gems for Timer Boost."
                                }
                            }
                        )
                    }

                    item {
                        PowerUpCard(
                            icon = "⚡",
                            title = "Double XP Potion",
                            description = "Earn 2x Experience Points (XP) on all workout reps for 30 minutes!",
                            gemCost = 500,
                            iconBg = Color(0xFFFF9600),
                            onClick = {
                                viewModel.buyPowerUp("Double XP Potion", 500) { success ->
                                    purchaseSuccessMessage = if (success) "✓ Double XP Potion active for 30 min!" else "⚠️ Need 500 gems for Double XP Potion."
                                }
                            }
                        )
                    }

                    item {
                        PowerUpCard(
                            icon = "🛡️",
                            title = "Form Shield",
                            description = "Protects your hearts from loss during challenging biomechanics attempts!",
                            gemCost = 600,
                            iconBg = Color(0xFF1CB0F6),
                            onClick = {
                                viewModel.buyPowerUp("Form Shield", 600) { success ->
                                    purchaseSuccessMessage = if (success) "✓ Form Shield activated!" else "⚠️ Need 600 gems for Form Shield."
                                }
                            }
                        )
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
private fun GemPackCard(
    icon: String,
    amount: Int,
    price: String,
    isPopular: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
            .border(
                1.5.dp,
                if (isPopular) Color(0xFFFFC800) else Color(0xFF283B42),
                RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPopular) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFC800), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("POPULAR", color = Color(0xFF18282E), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$amount",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = price,
                color = Color(0xFF49C0F8),
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PowerUpCard(
    icon: String,
    title: String,
    description: String,
    gemCost: Int,
    iconBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = Color(0xFF8596A0),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(Color(0xFF202F36), RoundedCornerShape(8.dp))
                        .clickable { onClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💎", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$gemCost",
                        color = Color(0xFF49C0F8),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
