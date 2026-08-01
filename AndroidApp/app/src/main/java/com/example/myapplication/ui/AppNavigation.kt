package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("onboarding") {
            OnboardingScreen(viewModel) {
                navController.navigate("home") {
                    popUpTo("onboarding") { inclusive = true }
                }
            }
        }
        composable("home") {
            HomeScreen(viewModel, 
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
            ResultScreen(viewModel) {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }
}

// Placeholder Screens for Member B to implement UI
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onFinish: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Onboarding Screen (Member B)")
    }
}

@Composable
fun HomeScreen(viewModel: MainViewModel, onNavigateToCalendar: () -> Unit, onNavigateToResult: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Home Screen (Member B)")
    }
}

@Composable
fun CalendarScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Calendar Screen (Member B)")
    }
}

@Composable
fun ResultScreen(viewModel: MainViewModel, onRestart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Result Screen (Member B)")
    }
}
