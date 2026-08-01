package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.notify.Reminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: MainViewModel,
    onNavigateToResult: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var serverUrlInput by remember(uiState.settings.serverUrl) { mutableStateOf(uiState.settings.serverUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("デバッグ & 設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // サーバーアドレス設定エリア
            Text("サーバーアドレス設定", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = serverUrlInput,
                onValueChange = { serverUrlInput = it },
                label = { Text("サーバーURL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(
                onClick = { viewModel.saveServerUrl(serverUrlInput) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("サーバーアドレスを保存")
            }

            Text("通知時刻設定", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { 
                        val newHour = (uiState.settings.notifyHour + 1) % 24
                        viewModel.saveSettings(uiState.settings.days, uiState.settings.dailyGoal, newHour, uiState.settings.notifyMinute)
                        Reminder.enableDaily(context, newHour, uiState.settings.notifyMinute)
                    }) { Text("+") }
                    Text("${uiState.settings.notifyHour}時", fontSize = 20.sp)
                    Button(onClick = { 
                        val newHour = (uiState.settings.notifyHour + 23) % 24
                        viewModel.saveSettings(uiState.settings.days, uiState.settings.dailyGoal, newHour, uiState.settings.notifyMinute)
                        Reminder.enableDaily(context, newHour, uiState.settings.notifyMinute)
                    }) { Text("-") }
                }
                Text(":", fontSize = 24.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = { 
                        val newMin = (uiState.settings.notifyMinute + 1) % 60
                        viewModel.saveSettings(uiState.settings.days, uiState.settings.dailyGoal, uiState.settings.notifyHour, newMin)
                        Reminder.enableDaily(context, uiState.settings.notifyHour, newMin)
                    }) { Text("+") }
                    Text("${uiState.settings.notifyMinute}分", fontSize = 20.sp)
                    Button(onClick = { 
                        val newMin = (uiState.settings.notifyMinute + 59) % 60
                        viewModel.saveSettings(uiState.settings.days, uiState.settings.dailyGoal, uiState.settings.notifyHour, newMin)
                        Reminder.enableDaily(context, uiState.settings.notifyHour, newMin)
                    }) { Text("-") }
                }
            }

            Button(
                onClick = onNavigateToResult,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("最終リザルト画面を表示")
            }

            Button(
                onClick = { 
                    Reminder.scheduleIn(context, 1)
                    viewModel.simulateSensorTrigger()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("⌚ スマートウォッチ連携デモ")
            }

            Button(
                onClick = { viewModel.injectDummyData() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("デモデータ投入 (30日分)")
            }

            Button(
                onClick = { viewModel.resetData() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("全データをリセット")
            }
        }
    }
}
