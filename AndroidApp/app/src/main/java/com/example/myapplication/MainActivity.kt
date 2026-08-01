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

    private var overlayGranted by mutableStateOf(false)
    private var usageAccessGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Reminder.createChannel(this)
        requestNotificationPermission()
        // 一日の最後(21:00)に、未申告なら申告を促す通知を送る(これは通知でOK)
        Reminder.enableDaily(this, hour = 21, minute = 0)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()

                // ペナルティは通知ではなく、他アプリを開いた時に重ねて出す警告で表示する。
                // そのための常駐サービスを、必要な権限が揃っている間だけ動かす。
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
                        overlayPermissionGranted = overlayGranted,
                        usageAccessGranted = usageAccessGranted,
                        onRequestOverlayPermission = { requestOverlayPermission() },
                        onRequestUsageAccess = { requestUsageAccess() },
                    )

                    // 1週間に2本以上吸っている場合、アプリを開くたびに1分間閉じられない
                    // 重いペナルティ画面を表示する(絶対要件のメッセージ)。
                    // キーを付けずにrememberすることで、この画面を開いた瞬間の状態だけで
                    // 一度だけ判定する。保存操作の直後に条件を満たしても、同じ滞在中に
                    // 即座には出てこないようにするため。
                    var showHeavyPenalty by remember {
                        mutableStateOf(uiState.weeklyTotal >= 2)
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
        // Settings画面から戻ってきたタイミングで権限状態を再チェックする
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
