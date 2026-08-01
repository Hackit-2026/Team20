package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "home") {
        composable("onboarding") {
            OnboardingScreen(
                onStart = { days, dailyGoal ->
                    viewModel.saveSettings(days, dailyGoal, uiState.settings.notifyHour)
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
                onNavigateToResult = { navController.navigate("result") }
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

@Composable
fun CalendarScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Calendar Screen (Member B)")
    }
}
