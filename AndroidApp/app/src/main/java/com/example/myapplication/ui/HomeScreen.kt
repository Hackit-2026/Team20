package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

import com.example.myapplication.logic.CharacterStage

import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.notify.Reminder

@Composable
fun HomeScreen(
    todayCount: Int,
    dailyGoal: Int,
    remainingDays: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToResult: () -> Unit,
    onResetData: () -> Unit,
    onInjectDummyData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val stage = remember(todayCount) { CharacterStage.fromCount(todayCount) }
    val line = remember(stage) { stageLines[stage.ordinal].random() }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onNavigateToCalendar) {
                Text("カレンダー")
            }
            if (remainingDays <= 0) {
                Button(onClick = onNavigateToResult) {
                    Text("結果を見る")
                }
            }
        }

        CharacterView(stage = stage.ordinal, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

        Text(
            text = stage.label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "「$line」",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        Text(
            text = "今日: ${todayCount}本(目標 ${dailyGoal}本)",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = "残り${remainingDays}日",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 24.dp),
        ) {
            OutlinedButton(onClick = onDecrement, modifier = Modifier.size(width = 72.dp, height = 48.dp)) {
                Text("-1")
            }
            Button(onClick = onIncrement, modifier = Modifier.size(width = 120.dp, height = 48.dp)) {
                Text("至福の +1")
            }
        }

        OutlinedButton(
            onClick = { Reminder.scheduleIn(context, 5) },
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("通知テスト (5秒後)")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedButton(onClick = onInjectDummyData) {
                Text("デモデータ")
            }
            OutlinedButton(onClick = onResetData) {
                Text("リセット")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreen(
            todayCount = 6,
            dailyGoal = 0,
            remainingDays = 4,
            onIncrement = {},
            onDecrement = {},
            onNavigateToCalendar = {},
            onNavigateToResult = {},
            onResetData = {},
            onInjectDummyData = {},
        )
    }
}
