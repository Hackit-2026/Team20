package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.logic.StageLogic
import com.example.myapplication.ui.theme.MyApplicationTheme

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 上部ナビゲーション
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row {
                IconButton(onClick = onNavigateToCalendar) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "履歴")
                }
                IconButton(onClick = onNavigateToTimeline) {
                    Icon(Icons.Default.Forum, contentDescription = "コミュニティ")
                }
            }
            IconButton(onClick = onNavigateToDebug) {
                Icon(Icons.Default.Settings, contentDescription = "デバッグ")
            }
        }

        // キャラクター表示 (高さを少し上に寄せてバランス調整)
        CharacterView(
            stageIndex = stageIndex, 
            modifier = Modifier
                .fillMaxWidth()
                .size(220.dp)
        )

        Text(
            text = if (uiState.isWithdrawal) "👿 禁断症状中！" else stageName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (uiState.isWithdrawal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "「$line」",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp),
        )

        // ポイントゲージ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ヤニ度ポイント: ${uiState.points} / ${StageLogic.MAX_POINTS} pt",
                style = MaterialTheme.typography.titleSmall
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 入力エリア
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isConfirmedToday) {
                Text(
                    "本日の分は確定済みです", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "今日吸った本数: ${uiState.todayCount}本",
                    style = MaterialTheme.typography.headlineSmall
                )
            } else {
                Text("今日の至福の本数を入力", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    OutlinedButton(onClick = onDecrementTemp) { Text("-", fontSize = 20.sp) }
                    Text("${uiState.tempCount}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    OutlinedButton(onClick = onIncrementTemp) { Text("+", fontSize = 20.sp) }
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("本数を確定して保存")
                }
            }
        }

        // 残り期間（画面下部に確実に見えるように位置調整）
        Text(
            text = "⏳ 残り期間: ${uiState.challengeStats?.daysLeft ?: 0}日",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
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
