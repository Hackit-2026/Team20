package com.example.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.logic.StageLogic

@Composable
fun ResultScreen(
    targetTotal: Int,
    actualTotal: Int,
    averagePerDay: Double,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1日平均本数から最終ポイントを推定
    val estimatedPoints = (averagePerDay * 10).toInt().coerceIn(0, StageLogic.MAX_POINTS)
    val stageIndex = StageLogic.getStageIndex(estimatedPoints)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("この期間のあなたの姿", style = MaterialTheme.typography.titleMedium)

        CharacterView(stageIndex = stageIndex, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

        Text(
            text = StageLogic.stageNames[stageIndex],
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = if (actualTotal >= targetTotal) "育成大成功！素晴らしい紫煙です。" else "もっと高みを目指せたはずだ…",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 24.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "目標合計 ${targetTotal}本 / 実際合計 ${actualTotal}本",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "1日平均 %.1f本".format(averagePerDay),
            style = MaterialTheme.typography.bodyLarge,
        )

        Button(onClick = onRetry, modifier = Modifier.padding(top = 40.dp)) {
            Text("もう一度挑戦する")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    MyApplicationTheme {
        ResultScreen(
            targetTotal = 20,
            actualTotal = 35,
            averagePerDay = 5.0,
            onRetry = {},
        )
    }
}
