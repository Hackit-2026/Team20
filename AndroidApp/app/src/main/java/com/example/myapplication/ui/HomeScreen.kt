package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.myapplication.logic.StageLogic
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

@Composable
fun HomeScreen(
    uiState: UiState,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stageIndex = StageLogic.getStageIndex(uiState.points)
    val stageName = StageLogic.stageNames[stageIndex]
    val progress = (uiState.points.toFloat() / StageLogic.MAX_POINTS).coerceIn(0f, 1f)
    val daysLeft = uiState.challengeStats?.daysLeft ?: 0
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
        // 上部: 履歴 / コミュニティ / デバッグへの導線。枠なしでフラットに。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onNavigateToCalendar) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "履歴", tint = Ink)
            }
            IconButton(onClick = onNavigateToTimeline) {
                Icon(Icons.Default.Forum, contentDescription = "コミュニティ", tint = Ink)
            }
            IconButton(onClick = onNavigateToDebug) {
                Icon(Icons.Default.Settings, contentDescription = "デバッグ", tint = Ink)
            }
        }

        // 日付(日本時間)を中央に大きく表示
        Text(
            text = today,
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp),
        )

        // 現在のステージ名(禁断症状中はそれを優先表示)
        Text(
            text = if (uiState.isWithdrawal) "👿 禁断症状中" else stageName,
            color = if (uiState.isWithdrawal) MaterialTheme.colorScheme.error else Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "「${uiState.currentLine}」",
            color = MutedSoft,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )

        // 残り日数を画面の主役として中央に大きく
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = "残り", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 14.dp))
            Text(
                text = "$daysLeft",
                color = Accent,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(text = "日", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 14.dp))
        }

        // 黒いドットで縁取ったキャラクター本体(色が薄いステージでも埋もれないように)
        CharacterView(
            stage = stageIndex,
            modifier = Modifier
                .size(200.dp)
                .padding(top = 20.dp),
        )

        // ヤニ度ポイントゲージ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ヤニ度ポイント: ${uiState.points} / ${StageLogic.MAX_POINTS} pt",
                color = Muted,
                fontSize = 11.sp,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .padding(top = 4.dp),
                color = Accent,
                trackColor = Plate,
            )
        }

        // 本数: 確定済みならその日の記録を表示、未確定ならステッパー+確定ボタン
        if (uiState.isConfirmedToday) {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "本日の本数は確定済み",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = "${uiState.todayCount}", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "本",
                        color = Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                        Text(text = "${uiState.tempCount}", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
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
                Text(
                    text = "目標 ${uiState.settings.dailyGoal}本",
                    color = MutedSoft,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                ) {
                    Text("本数を確定して保存")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreen(
            uiState = UiState(points = 50, tempCount = 10),
            onIncrementTemp = {},
            onDecrementTemp = {},
            onConfirm = {},
            onNavigateToCalendar = {},
            onNavigateToTimeline = {},
            onNavigateToDebug = {},
        )
    }
}
