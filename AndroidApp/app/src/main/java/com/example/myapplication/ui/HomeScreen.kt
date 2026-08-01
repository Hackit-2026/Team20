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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.logic.StageLogic
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// b-screen-character 採用の鏡モチーフカラーパレット
private val ScreenBg = Color(0xFFE7E9EA)
private val Plate = Color(0xFFFFFFFF)
private val Ink = Color(0xFF20242B)
private val Muted = Color(0xFF6B7280)
private val MutedSoft = Color(0xFF9AA1AB)
private val Accent = Color(0xFF2E5C56)

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
    val line = uiState.currentLine
    val progress = (uiState.points.toFloat() / StageLogic.MAX_POINTS).coerceIn(0f, 1f)
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("M/d")) }
    val daysLeft = uiState.challengeStats?.daysLeft ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 上部ナビゲーションプレート: 日付 + 履歴 / コミュニティ / デバッグ導線ボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Plate)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = today, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        }

        // 残り日数を主役級に大きく見せるカード (b-screen-character UI)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Accent)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "残り", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
            Text(
                text = "$daysLeft",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(text = "日", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
        }

        // 鏡フレームに入ったキャラクター本体 (b-screen-character デザイン)
        CharacterView(
            stage = stageIndex,
            modifier = Modifier
                .size(200.dp)
                .padding(top = 12.dp),
        )

        Text(
            text = if (uiState.isWithdrawal) "👿 禁断症状中！" else stageName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (uiState.isWithdrawal) MaterialTheme.colorScheme.error else Ink,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "「$line」",
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp),
        )

        // ポイントゲージ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ヤニ度ポイント: ${uiState.points} / ${StageLogic.MAX_POINTS} pt",
                style = MaterialTheme.typography.labelMedium,
                color = Muted
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .padding(top = 4.dp),
                color = Accent
            )
        }

        // 本数ステッパーカード (数字を主役に、確定ボタン制限付き)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Plate)
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.isConfirmedToday) {
                Text(
                    "本日の本数は確定済みです",
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "今日の本数: ${uiState.todayCount}本",
                    color = Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "今日の至福の本数を調整",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${uiState.tempCount}",
                        color = Ink,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "本",
                        color = Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                    )
                }
                Text(
                    text = "目標 ${uiState.settings.dailyGoal}本から我慢した分下げましょう",
                    color = MutedSoft,
                    fontSize = 11.sp,
                )
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onDecrementTemp,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onIncrementTemp,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) {
                    Text("本数を確定して保存")
                }
            }
        }

        Text(
            text = "説教しないアプリです。ただ、鏡を置くだけ。",
            color = MutedSoft,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
        )
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
            onNavigateToDebug = {}
        )
    }
}
