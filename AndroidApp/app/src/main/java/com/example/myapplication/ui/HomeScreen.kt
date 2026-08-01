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

@Composable
fun HomeScreen(
    todayCount: Int,
    dailyGoal: Int,
    remainingDays: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stage = remember(todayCount) { stageOf(todayCount) }
    val line = remember(stage) { stageLines[stage].random() }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CharacterView(stage = stage, modifier = Modifier.fillMaxWidth())

        Text(
            text = stageName[stage],
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
                Text("吸った +1")
            }
        }
    }
}

/**
 * RequirementsDefinition.md の閾値。logic/Stage.kt(A担当)が用意され次第、そちらに置き換える。
 */
private fun stageOf(count: Int): Int = when {
    count >= 10 -> 3
    count >= 5 -> 2
    count >= 2 -> 1
    else -> 0
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
        )
    }
}
