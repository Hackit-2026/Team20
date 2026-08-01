package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.data.AppRepo
import com.example.myapplication.notify.Reminder
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.MainViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Manual Dependency Injection for simplicity in this project
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = AppRepo(applicationContext)
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reminder.createChannel(this)
        requestNotificationPermission()
        // 一日の最後(21:00)に、未申告なら申告を促す通知を送る
        Reminder.enableDaily(this, hour = 21, minute = 0)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                HomeScreen(
                    uiState = uiState,
                    onChooseSmoked = { viewModel.chooseSmoked() },
                    onReportNoSmoke = { viewModel.reportNoSmoke() },
                    onIncrementTemp = { viewModel.incrementTempCount() },
                    onDecrementTemp = { viewModel.decrementTempCount() },
                    onConfirmSmokedReport = { viewModel.confirmSmokedReport() },
                )
            }
        }
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
