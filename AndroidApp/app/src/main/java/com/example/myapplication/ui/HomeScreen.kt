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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
    onNavigateToHistory: () -> Unit,
    onNavigateToGoalSetting: () -> Unit,
    onNavigateToDebug: () -> Unit,
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

        // 🧭 各専用画面へのナビゲーションバー
        NavigationHeader(
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToGoalSetting = onNavigateToGoalSetting,
            onNavigateToDebug = onNavigateToDebug
        )

        // 日付(日本時間)
        Text(
            text = today,
            color = Ink,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // 本数入力 ＆ 保存セクション
        CountSection(
            tempCount = uiState.tempCount,
            onIncrementTemp = onIncrementTemp,
            onDecrementTemp = onDecrementTemp,
            onSaveReport = onSaveReport,
        )
    }
}

/**
 * 🧭 各機能画面へ遷移するナビルートバー
 */
@Composable
private fun NavigationHeader(
    onNavigateToHistory: () -> Unit,
    onNavigateToGoalSetting: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onNavigateToHistory,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("📊 履歴", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onNavigateToGoalSetting,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("🎯 減煙設定", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onNavigateToDebug,
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("🛠️ デバッグ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
    } else {
        Text(
            text = "目標(3か月0本)まで残り${daysUntilGoal}日",
            color = MutedSoft,
            fontSize = 13.sp,
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
        fontSize = 13.sp,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "何本吸いましたか？",
                color = Muted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                OutlinedButton(
                    onClick = onDecrementTemp,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("−", fontSize = 26.sp)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = "$tempCount", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "本",
                        color = Muted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    )
                }
                Button(
                    onClick = onIncrementTemp,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onSaveReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

/**
 * 1週間に2本以上吸った場合の重いペナルティ全画面ブロック。
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
            onNavigateToHistory = {},
            onNavigateToGoalSetting = {},
            onNavigateToDebug = {}
        )
    }
}
