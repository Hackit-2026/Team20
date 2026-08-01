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
import com.example.myapplication.ui.HeavyPenaltyOverlay
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

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

                LaunchedEffect(overlayGranted, usageAccessGranted) {
                    if (overlayGranted && usageAccessGranted) {
                        PenaltyWatcherService.start(this@MainActivity)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        uiState = uiState,
                        onIncrementTemp = { viewModel.incrementTempCount() },
                        onDecrementTemp = { viewModel.decrementTempCount() },
                        onSaveReport = { viewModel.saveReport() },
                        onResetAll = { viewModel.resetAll() },
                        onSubmitForDate = { dateStr, count -> viewModel.submitReportForDate(dateStr, count) },
                        onApplyNextGoal = { difficulty -> viewModel.applyNextGoal(difficulty) },
                        onInject30DaysDemo = { viewModel.inject30DaysDemoData() },
                        onSaveNotifyTime = { hour, min ->
                            viewModel.saveNotifyTime(hour, min)
                            Reminder.enableDaily(this@MainActivity, hour, min)
                        },
                        overlayPermissionGranted = overlayGranted,
                        usageAccessGranted = usageAccessGranted,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestUsageAccess = { requestUsageAccess() },
                    )

                    var showHeavyPenalty by remember {
                        mutableStateOf(uiState.weeklyTotal >= 2)
                    }

                    LaunchedEffect(showHeavyPenalty) {
                        try {
                            if (showHeavyPenalty) startLockTask() else stopLockTask()
                        } catch (e: Exception) {
                        }
                    }

                    if (showHeavyPenalty) {
                        HeavyPenaltyOverlay(
                            weeklyTotal = uiState.weeklyTotal,
                            onDismiss = { showHeavyPenalty = false },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
