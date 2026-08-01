package com.example.myapplication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

import com.example.myapplication.logic.CharacterStage

@Composable
fun ResultScreen(
    targetTotal: Int,
    actualTotal: Int,
    averagePerDay: Double,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val endingStage = remember(averagePerDay) { CharacterStage.fromAverage(averagePerDay.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("この期間のあなたの姿", style = MaterialTheme.typography.titleMedium)

        CharacterView(stage = endingStage, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))

        Text(
            text = "S$endingStage ・ ${stageColorNames[endingStage]}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        Text(
            text = "目標合計 ${targetTotal}本 / 実際合計 ${actualTotal}本",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 24.dp),
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
            targetTotal = 0,
            actualTotal = 21,
            averagePerDay = 3.0,
            onRetry = {},
        )
    }
}
