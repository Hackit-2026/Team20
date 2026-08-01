package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

private val periodOptions = listOf(3 to "3日間", 7 to "1週間", 14 to "2週間", 30 to "1ヶ月")

@Composable
fun OnboardingScreen(
    onStart: (days: Int, dailyGoal: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableIntStateOf(7) }
    // 初期値を 10本 に設定
    var dailyGoal by remember { mutableIntStateOf(10) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("チャレンジ期間を選択", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            periodOptions.forEach { (days, label) ->
                FilterChip(
                    selected = selectedDays == days,
                    onClick = { selectedDays = days },
                    label = { Text(label) },
                )
            }
        }

        Text("1日の目標本数", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 40.dp))
        Text("初期値: 10本（お好みの設定本数に調整してください）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            OutlinedButton(onClick = { if (dailyGoal > 0) dailyGoal-- }) { Text("-", fontSize = 24.sp) }
            Text("${dailyGoal}本", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { dailyGoal++ }) { Text("+", fontSize = 24.sp) }
        }

        Button(
            onClick = { onStart(selectedDays, dailyGoal) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp),
        ) {
            Text("目標を設定してスタート", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    MyApplicationTheme {
        OnboardingScreen(onStart = { _, _ -> })
    }
}
