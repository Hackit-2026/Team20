package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

// アプリ全体で使う静かなパレット(ui パッケージ内で共有)。
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
    overlayPermissionGranted: Boolean = true,
    usageAccessGranted: Boolean = true,
    onRequestOverlayPermission: () -> Unit = {},
    onRequestUsageAccess: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 端末のタイムゾーンに関わらず、常に日本時間の日付を表示する
    val today = remember {
        LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("M/d"))
    }
    val report = uiState.today

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

        // 日付(日本時間)を中央に大きく表示
        Text(
            text = today,
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        // 項目名(今後の複数項目対応の置き場所。現状は禁煙のみ)
        Text(
            text = "禁煙",
            color = Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )

        // 目標は0本から。吸わずに3か月経つと達成。
        GoalLine(daysUntilGoal = uiState.daysUntilGoal, achieved = uiState.goalAchieved)

        // 本日の申告状況(あくまで小さいステータス表示。ペナルティの警告自体は
        // 他アプリを開いた時のオーバーレイ側=アプリの外に出すので、ここは画面を占領しない)
        StatusLine(report)

        // 本数入力は申告済みかどうかに関わらず常にここから操作できる(再申告も可能)。
        // 0本のまま保存すれば「吸わなかった」扱いになるので、吸った/吸わなかったの
        // 二択画面自体を挟まない。
        CountSection(
            tempCount = uiState.tempCount,
            onIncrementTemp = onIncrementTemp,
            onDecrementTemp = onDecrementTemp,
            onSaveReport = onSaveReport,
        )

        // これまでの申告履歴を一覧表示
        HistorySection(allReports = uiState.allReports)

        TextButton(onClick = onResetAll, modifier = Modifier.padding(top = 12.dp)) {
            Text("全データをリセット", fontSize = 11.sp, color = MutedSoft)
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
        modifier = Modifier.padding(top = 32.dp),
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
            .padding(top = 24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink),
    ) {
        Text("保存")
    }
}

@Composable
private fun HistorySection(allReports: Map<String, DailyReport>) {
    val sortedDates = remember(allReports) { allReports.keys.sortedDescending() }
    if (sortedDates.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
        Text(
            text = "履歴",
            color = Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        sortedDates.forEach { date ->
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

/**
 * 1週間に2本以上吸った場合の重いペナルティ。1分間は閉じられない全画面ブロック。
 * 「アプリを使う時にメッセージ表示」の絶対要件のうち、特に重いケース用。
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
                onClick = {}, // 背後のUIへのタップを吸収する
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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPenaltyPreview() {
    MyApplicationTheme {
        HomeScreen(
            uiState = UiState(today = DailyReport(reported = true, smoked = true, count = 3)),
            onIncrementTemp = {},
            onDecrementTemp = {},
            onSaveReport = {},
            overlayPermissionGranted = false,
            usageAccessGranted = false,
        )
    }
}
