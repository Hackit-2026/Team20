package com.example.myapplication

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.data.AppRepo
import com.example.myapplication.notify.Reminder
import com.example.myapplication.overlay.PenaltyWatcherService
import com.example.myapplication.ui.DebugScreen
import com.example.myapplication.ui.GoalSettingScreen
import com.example.myapplication.ui.HeavyPenaltyOverlay
import com.example.myapplication.ui.HistoryScreen
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

enum class ScreenRoute {
    HOME,
    HISTORY,
    GOAL_SETTING,
    DEBUG
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = AppRepo(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repo) as T
            }
        }
    }

    private var overlayGranted by mutableStateOf(false)
    private var usageAccessGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reminder.createChannel(this)
        requestNotificationPermission()
        Reminder.enableDaily(this, hour = 21, minute = 0)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                var currentRoute by remember { mutableStateOf(ScreenRoute.HOME) }

                LaunchedEffect(overlayGranted, usageAccessGranted) {
                    if (overlayGranted && usageAccessGranted) {
                        PenaltyWatcherService.start(this@MainActivity)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentRoute) {
                        ScreenRoute.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                onIncrementTemp = { viewModel.incrementTempCount() },
                                onDecrementTemp = { viewModel.decrementTempCount() },
                                onSaveReport = { viewModel.saveReport() },
                                onNavigateToHistory = { currentRoute = ScreenRoute.HISTORY },
                                onNavigateToGoalSetting = { currentRoute = ScreenRoute.GOAL_SETTING },
                                onNavigateToDebug = { currentRoute = ScreenRoute.DEBUG },
                                overlayPermissionGranted = overlayGranted,
                                usageAccessGranted = usageAccessGranted,
                                onRequestOverlayPermission = { requestOverlayPermission() },
                                onRequestUsageAccess = { requestUsageAccess() },
                            )
                        }
                        ScreenRoute.HISTORY -> {
                            HistoryScreen(
                                allReports = uiState.allReports,
                                onBack = { currentRoute = ScreenRoute.HOME }
                            )
                        }
                        ScreenRoute.GOAL_SETTING -> {
                            GoalSettingScreen(
                                uiState = uiState,
                                onApplyNextGoal = { difficulty -> viewModel.applyNextGoal(difficulty) },
                                onBack = { currentRoute = ScreenRoute.HOME }
                            )
                        }
                        ScreenRoute.DEBUG -> {
                            DebugScreen(
                                uiState = uiState,
                                onSubmitForDate = { dateStr, count -> viewModel.submitReportForDate(dateStr, count) },
                                onInject30DaysDemo = { viewModel.inject30DaysDemoData() },
                                onSaveNotifyTime = { hour, min ->
                                    viewModel.saveNotifyTime(hour, min)
                                    Reminder.enableDaily(this@MainActivity, hour, min)
                                },
                                onResetAll = {
                                    viewModel.resetAll()
                                    currentRoute = ScreenRoute.HOME
                                },
                                onBack = { currentRoute = ScreenRoute.HOME }
                            )
                        }
                    }

                    // 🚨 自アプリ内でも重度ペナルティ時は画面遷移や操作を一切不可にする全画面ロック
                    if (uiState.isHeavyPenaltyActive) {
                        LaunchedEffect(Unit) {
                            viewModel.triggerHeavyPenaltyLock()
                        }
                        HeavyPenaltyOverlay(
                            weeklyTotal = uiState.weeklyTotal,
                            onDismiss = { viewModel.refreshState() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
        overlayGranted = canDrawOverlays()
        usageAccessGranted = hasUsageAccess()
    }

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }

    private fun requestUsageAccess() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
