package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import com.example.myapplication.notify.Reminder
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    // 設定が未完了の場合はオンボーディングから開始
    val startDestination = if (uiState.isInitialized) "home" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            val context = LocalContext.current
            OnboardingScreen(
                onStart = { days, dailyGoal ->
                    viewModel.saveSettings(days, dailyGoal, uiState.settings.notifyHour)
                    // 通知を有効化
                    Reminder.enableDaily(context, uiState.settings.notifyHour, 0)
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            val stats = uiState.challengeStats
            HomeScreen(
                todayCount = uiState.todayCount,
                dailyGoal = uiState.settings.dailyGoal,
                remainingDays = stats?.daysLeft ?: uiState.settings.days,
                onIncrement = { viewModel.incrementCount() },
                onDecrement = { viewModel.decrementCount() },
                onNavigateToCalendar = { navController.navigate("calendar") },
                onNavigateToResult = { navController.navigate("result") },
                onResetData = { viewModel.resetData() },
                onInjectDummyData = { viewModel.injectDummyData() }
            )
        }
        composable("calendar") {
            CalendarScreen(viewModel) {
                navController.popBackStack()
            }
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
    }
}
