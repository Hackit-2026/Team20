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
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 上部ナビゲーション
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
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

        // キャラクター表示 (もっと大きく)
        CharacterView(
            stageIndex = stageIndex, 
            modifier = Modifier.fillMaxWidth().size(350.dp)
        )

        Text(
            text = if (uiState.isWithdrawal) "👿 禁断症状中！" else stageName,
            style = MaterialTheme.typography.headlineLarge,
            color = if (uiState.isWithdrawal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "「$line」",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp),
        )

        // ポイントゲージ
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
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
                    .height(12.dp)
                    .padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 入力エリア
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isConfirmedToday) {
                Text(
                    "本日の分は確定済みです", 
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "今日吸った本数: ${uiState.todayCount}本",
                    style = MaterialTheme.typography.headlineSmall
                )
            } else {
                Text("今日の至福の本数を入力", style = MaterialTheme.typography.labelLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    OutlinedButton(onClick = onDecrementTemp) { Text("-", fontSize = 24.sp) }
                    Text("${uiState.tempCount}", style = MaterialTheme.typography.displaySmall)
                    OutlinedButton(onClick = onIncrementTemp) { Text("+", fontSize = 24.sp) }
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("本数を確定して保存")
                }
            }
        }

        Text(
            text = "残り${uiState.challengeStats?.daysLeft ?: 0}日",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
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
