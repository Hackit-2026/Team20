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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier,
) {
    // 端末のタイムゾーンに関わらず、常に日本時間の日付を表示する
    val today = remember {
        LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("M/d"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        val report = uiState.today
        when {
            !report.reported -> UnreportedSection(
                tempSmoked = uiState.tempSmoked,
                tempCount = uiState.tempCount,
                onChooseSmoked = onChooseSmoked,
                onReportNoSmoke = onReportNoSmoke,
                onIncrementTemp = onIncrementTemp,
                onDecrementTemp = onDecrementTemp,
                onConfirmSmokedReport = onConfirmSmokedReport,
            )
            report.smoked -> PenaltySection(report)
            else -> SuccessSection()
        }
    }
}

@Composable
private fun UnreportedSection(
    tempSmoked: Boolean?,
    tempCount: Int,
    onChooseSmoked: () -> Unit,
    onReportNoSmoke: () -> Unit,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onConfirmSmokedReport: () -> Unit,
) {
    Text(
        text = "今日はタバコを吸いましたか？",
        color = Ink,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 48.dp),
    )

    if (tempSmoked != true) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 28.dp),
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

@Composable
private fun PenaltySection(report: DailyReport) {
    Text(
        text = "⚠ ペナルティ中",
        color = Warn,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 56.dp),
    )
    Text(
        text = "今日は吸ったと申告されています",
        color = Ink,
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(
        text = "本日: ${report.count}本",
        color = Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SuccessSection() {
    Text(
        text = "今日は吸っていません",
        color = Accent,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 56.dp),
    )
    Text(
        text = "本日の申告は完了しています",
        color = Muted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
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
        )
    }
}
