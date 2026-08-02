package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.myapplication.data.GoalMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingScreen(
    uiState: UiState,
    onApplyNextGoal: (Double) -> Unit,
    onApplyGoalMode: (GoalMode) -> Unit,
    onBack: () -> Unit
) {
    var difficulty by remember { mutableDoubleStateOf(1.5) }
    var selectedMode by remember { mutableStateOf(GoalMode.MODE_1_GRADUAL) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 段階的減煙 ＆ モード設定", fontWeight = FontWeight.Bold) },
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
            // 🚨 【新要件】目標達成が厳しければ2つのModeから選択
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "⚡ 目標調整モード選択 (達成が厳しい時)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text(
                        text = "達成が厳しければ、状況に合わせて以下の2つのモードを選択できます。",
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
                                    text = "🌱 Mode 1: 段階的減煙モード",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = "今日の目標 = 今までの1週間平均 / 1.5 (切り捨て)\n▶ 算出目標: ${uiState.mode1Goal} 本",
                                    fontSize = 12.sp,
                                    color = Accent,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
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
                                    text = "🚫 Mode 2: 完全禁煙固定モード",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ink
                                )
                                Text(
                                    text = "目標は常に 0 本にする (完全禁煙維持)\n▶ 算出目標: 0 本",
                                    fontSize = 12.sp,
                                    color = Warn,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onApplyGoalMode(selectedMode)
                            onBack()
                        },
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
                        text = "⚙️ カスタムきつさ（難易度）の設定",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text(
                        text = "計算式: 次の目標 = floor(今週平均 / きつさ)\n範囲: 1.2 ～ 20.0 の間で選択可能",
                        fontSize = 12.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "きつさの値: ${String.format("%.1f", difficulty)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    )

                    Slider(
                        value = difficulty.toFloat(),
                        onValueChange = { newValue ->
                            val rounded = (newValue * 10).roundToInt() / 10.0
                            difficulty = rounded.coerceIn(1.2, 20.0)
                        },
                        valueRange = 1.2f..20.0f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val calculatedNext = if (uiState.currentGoal <= 0) 0 
                                         else kotlin.math.floor(uiState.weeklyDailyAverage / difficulty).toInt().coerceIn(0, uiState.currentGoal)

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
                        Text("カスタムきつさ目標を適用して戻る")
                    }
                }
            }
        }
    }
}
