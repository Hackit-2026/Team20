package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.DailyReport
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val ScreenBg = Color(0xFFE7E9EA)
val Ink = Color(0xFF20242B)
val Muted = Color(0xFF6B7280)
val MutedSoft = Color(0xFF9AA1AB)
val Accent = Color(0xFF2E5C56)
val Warn = Color(0xFFB3261E)

@Composable
fun HomeScreen(
    uiState: UiState,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onSaveReport: () -> Unit,
    onResetAll: () -> Unit = {},
    onSubmitForDate: (String, Int) -> Unit = { _, _ -> },
    onApplyNextGoal: (Double) -> Unit = {},
    onInject30DaysDemo: () -> Unit = {},
    onSaveNotifyTime: (Int, Int) -> Unit = { _, _ -> },
    overlayPermissionGranted: Boolean = true,
    usageAccessGranted: Boolean = true,
    onRequestOverlayPermission: () -> Unit = {},
    onRequestUsageAccess: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val today = remember {
        LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("M/d"))
    }
    val report = uiState.today
    var showDebugPanel by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!overlayPermissionGranted || !usageAccessGranted) {
            PermissionBanner(
                overlayPermissionGranted = overlayPermissionGranted,
                usageAccessGranted = usageAccessGranted,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestUsageAccess = onRequestUsageAccess,
            )
        }

        Text(
            text = today,
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        Text(
            text = "禁煙・減煙チャレンジ",
            color = Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )

        GoalLine(daysUntilGoal = uiState.daysUntilGoal, achieved = uiState.goalAchieved)

        StatusLine(report)

        WeightedAverageSection(
            uiState = uiState,
            onApplyNextGoal = onApplyNextGoal
        )

        CountSection(
            tempCount = uiState.tempCount,
            onIncrementTemp = onIncrementTemp,
            onDecrementTemp = onDecrementTemp,
            onSaveReport = onSaveReport,
        )

        HistorySection(allReports = uiState.allReports)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { showDebugPanel = !showDebugPanel },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showDebugPanel) "🛠️ デバッグ・詳細設定を閉じる" else "🛠️ デバッグ・詳細設定を開く")
        }

        if (showDebugPanel) {
            DebugToolsSection(
                uiState = uiState,
                onSubmitForDate = onSubmitForDate,
                onInject30DaysDemo = onInject30DaysDemo,
                onSaveNotifyTime = onSaveNotifyTime,
                onResetAll = onResetAll
            )
        }
    }
}

@Composable
private fun PermissionBanner(
    overlayPermissionGranted: Boolean,
    usageAccessGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    Text(
        text = "他アプリを開いた時に警告を出すための権限が未設定です",
        color = Warn,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        if (!overlayPermissionGranted) {
            OutlinedButton(onClick = onRequestOverlayPermission) {
                Text("重ね表示を許可", fontSize = 12.sp)
            }
        }
        if (!usageAccessGranted) {
            OutlinedButton(onClick = onRequestUsageAccess) {
                Text("使用状況アクセスを許可", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GoalLine(daysUntilGoal: Long, achieved: Boolean) {
    if (achieved) {
        Text(
            text = "🎉 目標達成(3か月吸っていません)",
            color = Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
    } else {
        Text(
            text = "目標(3か月0本)まで残り${daysUntilGoal}日",
            color = MutedSoft,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun StatusLine(report: DailyReport) {
    val text = when {
        !report.reported -> "本日の申告: まだです"
        report.smoked -> "本日の申告: 吸った(${report.count}本) ・ ペナルティ中"
        else -> "本日の申告: 吸わなかった"
    }
    val color = when {
        !report.reported -> MutedSoft
        report.smoked -> Warn
        else -> Accent
    }
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun WeightedAverageSection(
    uiState: UiState,
    onApplyNextGoal: (Double) -> Unit
) {
    var difficulty by remember { mutableDoubleStateOf(1.0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 喫煙量の分析 ＆ 目標減煙設定",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(modifier = Modifier.height(8.dp))

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

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "🎯 次の目標を少しずつ減らす",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "きつさの値: ${String.format("%.1f", difficulty)} (今週平均 - きつさ)",
                fontSize = 11.sp,
                color = MutedSoft,
                modifier = Modifier.padding(top = 2.dp)
            )

            Slider(
                value = difficulty.toFloat(),
                onValueChange = { difficulty = it.toDouble() },
                valueRange = 0.0f..3.0f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )

            val calculatedNext = if (uiState.currentGoal <= 0) 0 
                                 else kotlin.math.floor(uiState.weeklyDailyAverage - difficulty).toInt().coerceIn(0, uiState.currentGoal)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "計算後の次回目標: ${calculatedNext}本",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (calculatedNext == 0) Accent else Ink
                )
                Button(
                    onClick = { onApplyNextGoal(difficulty) },
                    colors = ButtonDefaults.buttonColors(containerColor = Accent)
                ) {
                    Text("新目標を適用", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CountSection(
    tempCount: Int,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onSaveReport: () -> Unit,
) {
    Text(
        text = "何本吸いましたか？",
        color = Muted,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 24.dp),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp),
    ) {
        OutlinedButton(
            onClick = onDecrementTemp,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("−", fontSize = 22.sp)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "$tempCount", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "本",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
            )
        }
        Button(
            onClick = onIncrementTemp,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
    Button(
        onClick = onSaveReport,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink),
    ) {
        Text("保存")
    }
}

@Composable
private fun HistorySection(allReports: Map<String, DailyReport>) {
    val sortedDates = remember(allReports) { allReports.keys.sortedDescending() }
    if (sortedDates.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            text = "履歴",
            color = Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        sortedDates.take(7).forEach { date ->
            val entry = allReports.getValue(date)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = date, color = Muted, fontSize = 12.sp)
                Text(
                    text = if (entry.smoked) "吸った(${entry.count}本)" else "吸わなかった",
                    color = if (entry.smoked) Warn else Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun DebugToolsSection(
    uiState: UiState,
    onSubmitForDate: (String, Int) -> Unit,
    onInject30DaysDemo: () -> Unit,
    onSaveNotifyTime: (Int, Int) -> Unit,
    onResetAll: () -> Unit
) {
    var dateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var countInput by remember { mutableStateOf("5") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🛠️ デバッグ機能パネル",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "📅 何月何日を選択してデータを追加 (デバッグ用)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("日付 (YYYY-MM-DD)", fontSize = 10.sp) },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = countInput,
                    onValueChange = { countInput = it.filter { c -> c.isDigit() } },
                    label = { Text("本数", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Button(
                onClick = {
                    val count = countInput.toIntOrNull() ?: 0
                    if (dateInput.isNotBlank()) {
                        onSubmitForDate(dateInput, count)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text("指定日付のデータを登録", fontSize = 12.sp)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "📊 過去30日間のデモデータを一括追加", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onInject30DaysDemo,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text("過去30日分のデモデータを投入", fontSize = 12.sp)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "⏰ リマインド通知時刻の設定", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "現在の時刻: ${uiState.notifyHour}時 ${uiState.notifyMinute}分", fontSize = 12.sp)
                Row {
                    OutlinedButton(onClick = {
                        val newH = (uiState.notifyHour + 1) % 24
                        onSaveNotifyTime(newH, uiState.notifyMinute)
                    }) { Text("時+", fontSize = 11.sp) }
                    Spacer(modifier = Modifier.width(4.dp))
                    OutlinedButton(onClick = {
                        val newM = (uiState.notifyMinute + 15) % 60
                        onSaveNotifyTime(uiState.notifyHour, newM)
                    }) { Text("分+", fontSize = 11.sp) }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(text = "🚨 全データ初期化", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Warn)
            Button(
                onClick = onResetAll,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Warn)
            ) {
                Text("すべてのデータをリセットして最初からスタート", fontSize = 12.sp)
            }
        }
    }
}

/**
 * 1週間に2本以上吸った場合の重いペナルティ。1分間は閉じられない全画面ブロック。
 */
@Composable
fun HeavyPenaltyOverlay(weeklyTotal: Int, onDismiss: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }
    val canClose = secondsLeft <= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1418))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "もっと禁煙してください！",
            color = Color(0xFFFF6B6B),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "今週の合計: ${weeklyTotal}本",
            color = Color(0xFFCBD0D6),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (canClose) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("閉じる")
            }
        } else {
            Text(
                text = "あと${secondsLeft}秒は操作できません",
                color = Color(0xFF8A9099),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenUnreportedPreview() {
    MyApplicationTheme {
        HomeScreen(
            uiState = UiState(),
            onIncrementTemp = {},
            onDecrementTemp = {},
            onSaveReport = {},
        )
    }
}
