package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.GoalMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingScreen(
    uiState: UiState,
    onApplyGoalMode: (GoalMode, Int?) -> Unit,
    onSaveNotifyTime: (Int, Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(GoalMode.MODE_1_GRADUAL) }
    var averageInput by remember { mutableStateOf("") }
    val canApply = selectedMode != GoalMode.MODE_1_GRADUAL || averageInput.toIntOrNull()?.let { it >= 0 } == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 設定", fontWeight = FontWeight.Bold) },
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
                .background(ScreenBg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🎯 【目標調整モード選択】
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "モード選択",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text(
                        text = "状況に合わせてモードを選択してください。",
                        fontSize = 12.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Mode 1 オプション
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedMode = GoalMode.MODE_1_GRADUAL },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMode == GoalMode.MODE_1_GRADUAL) Color(0xFFE8F5E9) else ScreenBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == GoalMode.MODE_1_GRADUAL,
                                onClick = { selectedMode = GoalMode.MODE_1_GRADUAL }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🌱 減煙モード",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = "今の本数から少しずつ減らしていく",
                                    fontSize = 12.sp,
                                    color = Accent,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        if (selectedMode == GoalMode.MODE_1_GRADUAL) {
                            OutlinedTextField(
                                value = averageInput,
                                onValueChange = { averageInput = it.filter { c -> c.isDigit() } },
                                label = { Text("1日あたりの平均本数") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            )
                        }
                    }

                    // Mode 2 オプション
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedMode = GoalMode.MODE_2_ZERO },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedMode == GoalMode.MODE_2_ZERO) Color(0xFFFFEBEE) else ScreenBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMode == GoalMode.MODE_2_ZERO,
                                onClick = { selectedMode = GoalMode.MODE_2_ZERO }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🚫 完全禁煙モード",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = "0本からスタートする",
                                    fontSize = 12.sp,
                                    color = Warn,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val manualAverage = if (selectedMode == GoalMode.MODE_1_GRADUAL) averageInput.toIntOrNull() else null
                            onApplyGoalMode(selectedMode, manualAverage)
                            onBack()
                        },
                        enabled = canApply,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("選択したモードで目標を適用して戻る")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⏰ リマインド通知時刻の設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "現在の時刻: ${uiState.notifyHour}時 ${uiState.notifyMinute}分",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Row {
                            OutlinedButton(onClick = {
                                val newH = (uiState.notifyHour + 1) % 24
                                onSaveNotifyTime(newH, uiState.notifyMinute)
                            }) { Text("時+") }
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedButton(onClick = {
                                val newM = (uiState.notifyMinute + 15) % 60
                                onSaveNotifyTime(uiState.notifyHour, newM)
                            }) { Text("分+") }
                        }
                    }
                }
            }
        }
    }
}
