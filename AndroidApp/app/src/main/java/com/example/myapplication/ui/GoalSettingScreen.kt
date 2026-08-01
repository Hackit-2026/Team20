package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingScreen(
    uiState: UiState,
    onApplyNextGoal: (Double) -> Unit,
    onBack: () -> Unit
) {
    // 🎯 目標/きつさの値を 0.2 ～ 20.0 の範囲で選択可能に！ (初期値 1.0)
    var difficulty by remember { mutableDoubleStateOf(1.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 段階的減煙設定", fontWeight = FontWeight.Bold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📊 喫煙量の分析結果",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "直近1週間の重み付き平均:", fontSize = 13.sp, color = Muted)
                        Text(
                            text = String.format("%.1f 本/日", uiState.weightedAverage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "今週の1日あたり平均:", fontSize = 13.sp, color = Muted)
                        Text(
                            text = String.format("%.1f 本/日", uiState.weeklyDailyAverage),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "現在の目標本数:", fontSize = 13.sp, color = Muted)
                        Text(
                            text = "${uiState.currentGoal} 本",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
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
                        text = "⚙️ きつさ（難易度）の設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text(
                        text = "計算式: 次の目標 = floor(今週平均 - きつさ)\n範囲: 0.2 ～ 20.0 の間で選択可能",
                        fontSize = 12.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // きつさの数値表示 (0.2 ～ 20.0)
                    Text(
                        text = "きつさの値: ${String.format("%.1f", difficulty)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )

                    // 🎚️ スライダー (0.2f ～ 20.0f)
                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { newValue ->
                            // 0.1刻みに丸めて 0.2f ～ 20.0f の範囲に制限
                            val rounded = (newValue * 10).roundToInt() / 10.0
                            difficulty = rounded.coerceIn(0.2, 20.0)
                        },
                        valueRange = 0.2f..20.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ➕/➖ 微調整ボタン
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(onClick = {
                            difficulty = ((difficulty - 1.0) * 10).roundToInt() / 10.0.coerceIn(0.2, 20.0)
                        }) { Text("-1.0") }
                        OutlinedButton(onClick = {
                            difficulty = ((difficulty - 0.1) * 10).roundToInt() / 10.0.coerceIn(0.2, 20.0)
                        }) { Text("-0.1") }
                        OutlinedButton(onClick = {
                            difficulty = ((difficulty + 0.1) * 10).roundToInt() / 10.0.coerceIn(0.2, 20.0)
                        }) { Text("+0.1") }
                        OutlinedButton(onClick = {
                            difficulty = ((difficulty + 1.0) * 10).roundToInt() / 10.0.coerceIn(0.2, 20.0)
                        }) { Text("+1.0") }
                    }

                    val calculatedNext = if (uiState.currentGoal <= 0) 0 
                                         else kotlin.math.floor(uiState.weeklyDailyAverage - difficulty).toInt().coerceIn(0, uiState.currentGoal)

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ScreenBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "計算後の次回目標:", fontSize = 14.sp, color = Ink)
                            Text(
                                text = "${calculatedNext} 本",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (calculatedNext == 0) Accent else Ink
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onApplyNextGoal(difficulty)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("この目標を適用して戻る")
                    }
                }
            }
        }
    }
}
