package com.example.myapplication.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.DailyReport
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// アプリ全体で使う静かなパレット(ui パッケージ内で共有)。
val ScreenBg = Color(0xFFE7E9EA)
val Plate = Color(0xFFFFFFFF)
val Ink = Color(0xFF20242B)
val Muted = Color(0xFF6B7280)
val MutedSoft = Color(0xFF9AA1AB)
val Accent = Color(0xFF2E5C56)
val Warn = Color(0xFFB3261E)

@Composable
fun HomeScreen(
    uiState: UiState,
    onChooseSmoked: () -> Unit,
    onReportNoSmoke: () -> Unit,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onConfirmSmokedReport: () -> Unit,
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

        // 申告UIは申告済みかどうかに関わらず常にここから操作できる(再申告も可能)
        ReportSection(
            tempSmoked = uiState.tempSmoked,
            tempCount = uiState.tempCount,
            onChooseSmoked = onChooseSmoked,
            onReportNoSmoke = onReportNoSmoke,
            onIncrementTemp = onIncrementTemp,
            onDecrementTemp = onDecrementTemp,
            onConfirmSmokedReport = onConfirmSmokedReport,
        )
    }
}

@Composable
private fun PermissionBanner(
    overlayPermissionGranted: Boolean,
    usageAccessGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Plate)
            .padding(14.dp),
    ) {
        Text(
            text = "他アプリを開いた時に警告を出すための権限が未設定です",
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
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
private fun ReportSection(
    tempSmoked: Boolean?,
    tempCount: Int,
    onChooseSmoked: () -> Unit,
    onReportNoSmoke: () -> Unit,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onConfirmSmokedReport: () -> Unit,
) {
    if (tempSmoked != true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 32.dp),
        ) {
            Button(
                onClick = onChooseSmoked,
                modifier = Modifier.size(width = 140.dp, height = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Warn),
            ) {
                Text("吸った", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onReportNoSmoke,
                modifier = Modifier.size(width = 140.dp, height = 56.dp),
            ) {
                Text("吸わなかった", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
        }
    } else {
        Text(
            text = "何本吸いましたか？",
            color = Muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 28.dp),
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
            onClick = onConfirmSmokedReport,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
        ) {
            Text("申告する")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenUnreportedPreview() {
    MyApplicationTheme {
        HomeScreen(
            uiState = UiState(),
            onChooseSmoked = {},
            onReportNoSmoke = {},
            onIncrementTemp = {},
            onDecrementTemp = {},
            onConfirmSmokedReport = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPenaltyPreview() {
    MyApplicationTheme {
        HomeScreen(
            uiState = UiState(today = DailyReport(reported = true, smoked = true, count = 3)),
            onChooseSmoked = {},
            onReportNoSmoke = {},
            onIncrementTemp = {},
            onDecrementTemp = {},
            onConfirmSmokedReport = {},
            overlayPermissionGranted = false,
            usageAccessGranted = false,
        )
    }
}
