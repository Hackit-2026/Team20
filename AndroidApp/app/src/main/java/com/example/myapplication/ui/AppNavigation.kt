package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.notify.Reminder

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    // 設定が未完了の場合は「+」の起動画面から開始し、そこからオンボーディングへ進む
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
                        popUpTo("onboarding") { inclusive = true }
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
                onNavigateToDebug = { navController.navigate("debug") },
            )
        }
        composable("calendar") {
            CalendarScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("timeline") {
            CommunityFeedScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("debug") {
            DebugScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToResult = { navController.navigate("result") }
            )
        }
        composable("result") {
            val stats = uiState.challengeStats
            ResultScreen(
                targetTotal = stats?.targetTotal ?: 0,
                actualTotal = stats?.totalCount ?: 0,
                averagePerDay = (stats?.averageDaily ?: 0f).toDouble(),
                onRetry = {
                    viewModel.resetData()
                    navController.navigate("onboarding") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
