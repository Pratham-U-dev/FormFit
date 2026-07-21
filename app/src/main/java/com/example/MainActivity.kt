package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.FormFitTheme
import com.example.ui.theme.DuoBlue
import com.example.ui.theme.DuoGreen
import com.example.ui.theme.DuoInkMuted
import com.example.viewmodel.WorkoutViewModel

enum class MainTab {
    COACH, NUTRITION, LEADERBOARD, PROFILE
}

enum class ActiveScreen {
    TABS, PRACTICE, SUMMARY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.audio.DuoSoundPlayer.init(this)
        enableEdgeToEdge()
        setContent {
            FormFitTheme {
                FormFitApp()
            }
        }
    }
}

@Composable
fun FormFitApp() {
    val viewModel: WorkoutViewModel = viewModel()
    var activeScreen by remember { mutableStateOf(ActiveScreen.TABS) }
    var currentTab by remember { mutableStateOf(MainTab.COACH) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (activeScreen == ActiveScreen.TABS) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.COACH,
                        onClick = { currentTab = MainTab.COACH },
                        label = { Text("Coach") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DuoGreen,
                            selectedTextColor = DuoGreen,
                            unselectedIconColor = DuoInkMuted,
                            unselectedTextColor = DuoInkMuted,
                            indicatorColor = Color(0xFFE8F5E9)
                        ),
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.COACH) Icons.Default.FitnessCenter else Icons.Outlined.FitnessCenter,
                                contentDescription = "Coach Tab",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.NUTRITION,
                        onClick = { currentTab = MainTab.NUTRITION },
                        label = { Text("Nutrition") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DuoGreen,
                            selectedTextColor = DuoGreen,
                            unselectedIconColor = DuoInkMuted,
                            unselectedTextColor = DuoInkMuted,
                            indicatorColor = Color(0xFFE8F5E9)
                        ),
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.NUTRITION) Icons.Default.Restaurant else Icons.Outlined.Restaurant,
                                contentDescription = "Nutrition Tab",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.LEADERBOARD,
                        onClick = { currentTab = MainTab.LEADERBOARD },
                        label = { Text("Leaderboard") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DuoGreen,
                            selectedTextColor = DuoGreen,
                            unselectedIconColor = DuoInkMuted,
                            unselectedTextColor = DuoInkMuted,
                            indicatorColor = Color(0xFFE8F5E9)
                        ),
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.LEADERBOARD) Icons.Default.Leaderboard else Icons.Outlined.Leaderboard,
                                contentDescription = "Leaderboard Tab",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.PROFILE,
                        onClick = { currentTab = MainTab.PROFILE },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DuoGreen,
                            selectedTextColor = DuoGreen,
                            unselectedIconColor = DuoInkMuted,
                            unselectedTextColor = DuoInkMuted,
                            indicatorColor = Color(0xFFE8F5E9)
                        ),
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                                contentDescription = "Profile Tab",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (activeScreen) {
            ActiveScreen.TABS -> {
                when (currentTab) {
                    MainTab.COACH -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onStartWorkout = { exerciseType ->
                                viewModel.startWorkout(exerciseType)
                                activeScreen = ActiveScreen.PRACTICE
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    MainTab.NUTRITION -> {
                        NutritionScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    MainTab.LEADERBOARD -> {
                        LeaderboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    MainTab.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
            ActiveScreen.PRACTICE -> {
                PracticeScreen(
                    viewModel = viewModel,
                    onWorkoutFinished = {
                        activeScreen = if (viewModel.lastCompletedSession.value != null) {
                            ActiveScreen.SUMMARY
                        } else {
                            ActiveScreen.TABS // Abandoned
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            ActiveScreen.SUMMARY -> {
                SummaryScreen(
                    viewModel = viewModel,
                    onContinue = {
                        activeScreen = ActiveScreen.TABS
                        currentTab = MainTab.COACH
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
