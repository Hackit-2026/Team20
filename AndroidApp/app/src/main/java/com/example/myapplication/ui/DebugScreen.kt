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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    uiState: UiState,
    onSubmitForDate: (String, Int) -> Unit,
    onInject30DaysDemo: () -> Unit,
    onResetAll: () -> Unit,
    onBack: () -> Unit
) {
    var dateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var countInput by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛠️ デバッグ ＆ 運用設定", fontWeight = FontWeight.Bold) },
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
                        text = "📅 何月何日を選択してデータを追加 (デバッグ用)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            label = { Text("日付 (YYYY-MM-DD)", fontSize = 11.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = countInput,
                            onValueChange = { countInput = it.filter { c -> c.isDigit() } },
                            label = { Text("本数", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val count = countInput.toIntOrNull() ?: 0
                            if (dateInput.isNotBlank()) {
                                onSubmitForDate(dateInput, count)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("指定日付のデータを登録")
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
                        text = "🚨 重度ペナルティ ＆ 壁紙変更テストボタン",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Warn
                    )

                    Text(
                        text = "昨日の喫煙データに3本を投入し、今週合計2本以上の重度ペナルティ（警告壁紙＆60秒ロック）を一発で発火させます。",
                        fontSize = 12.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val yesterday = LocalDate.now().minusDays(1).toString()
                            onSubmitForDate(yesterday, 3)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Warn)
                    ) {
                        Text("昨日に3本吸ったデータを入れて警告壁紙をテスト")
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
                        text = "📊 過去30日間のデモデータ一括追加",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )

                    Text(
                        text = "ランダムな申告データ（30日分）を自動生成して投入します。",
                        fontSize = 12.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onInject30DaysDemo,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent)
                    ) {
                        Text("過去30日分のデモデータを投入")
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
                        text = "🚨 全データ初期化",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Warn
                    )

                    Text(
                        text = "すべての申告ログ・設定・達成状況を消去して初期状態へ戻します。",
                        fontSize = 12.sp,
                        color = MutedSoft,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onResetAll,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Warn)
                    ) {
                        Text("すべてのデータをリセットして最初からスタート")
                    }
                }
            }
        }
    }
}
