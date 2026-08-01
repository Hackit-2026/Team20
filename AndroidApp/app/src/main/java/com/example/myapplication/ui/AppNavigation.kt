package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.example.myapplication.notify.Reminder

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    // リセット時などに強制的に start 画面へ戻す
    LaunchedEffect(uiState.isInitialized) {
        if (!uiState.isInitialized) {
            navController.navigate("start") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (uiState.isInitialized) "home" else "start"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("start") {
            StartScreen(onStartClick = { navController.navigate("onboarding") })
        }
        composable("onboarding") {
            val context = LocalContext.current
            OnboardingScreen(
                onStart = { days, dailyGoal ->
                    viewModel.saveSettings(days, dailyGoal, uiState.settings.notifyHour, uiState.settings.notifyMinute)
                    // 通知を有効化
                    Reminder.enableDaily(context, uiState.settings.notifyHour, uiState.settings.notifyMinute)
                    navController.navigate("home") {
                        popUpTo("start") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                uiState = uiState,
                onIncrementTemp = { viewModel.incrementTempCount() },
                onDecrementTemp = { viewModel.decrementTempCount() },
                onConfirm = { viewModel.confirmTodayCount() },
                onNavigateToCalendar = { navController.navigate("calendar") },
                onNavigateToTimeline = { navController.navigate("timeline") },
                onNavigateToDebug = { navController.navigate("debug") }
            )
        }
        composable("calendar") {
            CalendarScreen(viewModel) {
                navController.popBackStack()
            }
        }
        composable("timeline") {
            CommunityFeedScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("result") {
            val stats = uiState.challengeStats
            ResultScreen(
                targetTotal = stats?.targetTotal ?: 0,
                actualTotal = stats?.totalCount ?: 0,
                averagePerDay = (stats?.averageDaily ?: 0f).toDouble(),
                onRetry = {
                    navController.navigate("onboarding") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
        composable("debug") {
            DebugScreen(
                viewModel = viewModel,
                onNavigateToResult = { navController.navigate("result") },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
