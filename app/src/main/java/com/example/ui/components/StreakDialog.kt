package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
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

data class FriendStreak(
    val id: String,
    val name: String,
    val streak: Int,
    val avatarEmoji: String,
    val isPending: Boolean = false,
    var isCheered: Boolean = false
)

@Composable
fun StreakDialog(
    viewModel: WorkoutViewModel,
    onDismiss: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    var selectedTab by remember { mutableStateOf("PERSONAL") }

    val friendsList = remember {
        mutableStateListOf(
            FriendStreak("1", "V S Anish Bangera", 92, "👨‍🎤"),
            FriendStreak("2", "Yojan D", 77, "🕶️"),
            FriendStreak("3", "Phelicia Dsilva", 58, "👩‍🦰"),
            FriendStreak("4", "Sean Dsouza", 30, "🤓"),
            FriendStreak("5", "Regherethere", 0, "🤖", isPending = true)
        )
    }

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
                        modifier = Modifier.testTag("close_streak_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Text(
                        text = "Streak",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )

                    IconButton(
                        onClick = {
                            viewModel.cheerFriend("your friends")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // TOP TABS (PERSONAL / FRIENDS)
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabItem(
                        title = "PERSONAL",
                        isSelected = selectedTab == "PERSONAL",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "PERSONAL" }
                    )
                    TabItem(
                        title = "FRIENDS",
                        isSelected = selectedTab == "FRIENDS",
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = "FRIENDS" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == "PERSONAL") {
                    PersonalStreakContent(
                        streakDays = if (stats.streakDays > 0) stats.streakDays else 93,
                        onStartWorkout = {
                            onDismiss()
                            onStartWorkout()
                        }
                    )
                } else {
                    FriendsStreakContent(
                        friends = friendsList,
                        onCheer = { friend ->
                            friend.isCheered = true
                            viewModel.cheerFriend(friend.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = if (isSelected) Color(0xFF49C0F8) else Color(0xFF8596A0),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (isSelected) Color(0xFF49C0F8) else Color.Transparent, shape = RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun PersonalStreakContent(
    streakDays: Int,
    onStartWorkout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // HERO STREAK DISPLAY
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF202F36), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STREAK SOCIETY",
                            color = Color(0xFF8596A0),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$streakDays",
                        color = Color(0xFF8596A0),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 56.sp
                    )
                    Text(
                        text = "day streak!",
                        color = Color(0xFF8596A0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                // Flame graphic right side
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(100.dp)
                        .background(Color(0xFF202F36), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 52.sp)
                }
            }
        }

        // RESET NOTIFICATION CARD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFFFF4B4B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏱️", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "60 minutes until your streak resets!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "START LESSON",
                            color = Color(0xFF49C0F8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { onStartWorkout() }
                                .testTag("streak_start_workout_action")
                        )
                    }
                }
            }
        }

        // STREAK CALENDAR
        item {
            Text(
                text = "Streak Calendar",
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
                Column {
                    // Month selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("<", color = Color(0xFF8596A0), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("October 2026", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(">", color = Color(0xFF8596A0), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Days of week
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                            Text(
                                text = day,
                                color = Color(0xFF8596A0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sample Calendar Matrix
                    CalendarWeekRow(listOf("27", "28", "29", "30", "1", "2", "3"), streakRange = 4..6)
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarWeekRow(listOf("4", "5", "6", "7", "8", "9", "10"), streakRange = 0..1, currentDay = 2)
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarWeekRow(listOf("11", "12", "13", "14", "15", "16", "17"))
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarWeekRow(listOf("18", "19", "20", "21", "22", "23", "24"))
                    Spacer(modifier = Modifier.height(8.dp))
                    CalendarWeekRow(listOf("25", "26", "27", "28", "29", "30", "31"))
                }
            }
        }

        // STREAK GOAL
        item {
            Text(
                text = "Streak Goal",
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
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("7", "30", "50", "100").forEachIndexed { idx, goal ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(if (idx < 3) Color(0xFFFF9600) else Color(0xFF202F36), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (idx < 3) "✓" else goal, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                                if (idx < 3) {
                                    Box(
                                        modifier = Modifier
                                            .width(42.dp)
                                            .height(8.dp)
                                            .background(Color(0xFFFF9600))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "$streakDays / 100 DAYS",
                        color = Color(0xFF8596A0),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // STREAK SOCIETY REWARDS
        item {
            Text(
                text = "Streak Society",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }

        item {
            StreakRewardCard(
                days = "100 days",
                description = "Reach a streak of 100 days to unlock VIP Streak badge.",
                isUnlocked = streakDays >= 100
            )
        }

        item {
            StreakRewardCard(
                days = "365 days",
                description = "Reach a streak of 365 days to unlock the Immortal Form Crown.",
                isUnlocked = streakDays >= 365
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CalendarWeekRow(
    days: List<String>,
    streakRange: IntRange? = null,
    currentDay: Int? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val isInStreak = streakRange != null && index in streakRange
            val isCurrent = currentDay == index

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        when {
                            isCurrent -> Color(0xFF253840)
                            isInStreak -> Color(0xFFE53935).copy(alpha = 0.35f)
                            else -> Color.Transparent
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    color = when {
                        isCurrent -> Color(0xFF49C0F8)
                        isInStreak -> Color(0xFFFF4B4B)
                        else -> Color(0xFF52656D)
                    },
                    fontWeight = if (isInStreak || isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StreakRewardCard(
    days: String,
    description: String,
    isUnlocked: Boolean
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
                    .size(54.dp)
                    .background(Color(0xFF202F36), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) Color(0xFFFFC800) else Color(0xFF52656D),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = days,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    color = Color(0xFF8596A0),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                    color = if (isUnlocked) DuoGreen else Color(0xFF52656D),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FriendsStreakContent(
    friends: List<FriendStreak>,
    onCheer: (FriendStreak) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FRIEND HEADER BANNER
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFFF9600), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🦉", fontSize = 48.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("🧘‍♀️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("🔥", fontSize = 48.sp)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Friend Streaks",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Text(
                    text = "EDIT",
                    color = Color(0xFF49C0F8),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { }
                )
            }
        }

        items(friends) { friend ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18282E), RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFF283B42), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF202F36), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(friend.avatarEmoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = friend.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (friend.isPending) {
                                Text(
                                    text = "Request pending",
                                    color = Color(0xFF8596A0),
                                    fontSize = 12.sp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔥", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${friend.streak}",
                                        color = Color(0xFF8596A0),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (!friend.isPending) {
                        Button(
                            onClick = { onCheer(friend) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (friend.isCheered) Color(0xFF202F36) else Color(0xFFFF9600)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (friend.isCheered) "Cheered! 👏" else "Cheer 👏",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF49C0F8)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "+ ADD WORKOUT BUDDY",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
