package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme

private val periodOptions = listOf(3 to "3日間", 7 to "1週間", 14 to "2週間", 30 to "1ヶ月")

@Composable
fun OnboardingScreen(
    onStart: (days: Int, dailyGoal: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableIntStateOf(7) }
    var dailyGoal by remember { mutableIntStateOf(10) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("チャレンジ期間", style = MaterialTheme.typography.titleMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            periodOptions.forEach { (days, label) ->
                FilterChip(
                    selected = selectedDays == days,
                    onClick = { selectedDays = days },
                    label = { Text(label) },
                )
            }
        }

        Text("1日の目標本数", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 32.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(onClick = { if (dailyGoal > 0) dailyGoal-- }) { Text("-") }
            Text("${dailyGoal}本", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { if (dailyGoal < 30) dailyGoal++ }) { Text("+") }
        }

        Button(
            onClick = { onStart(selectedDays, dailyGoal) },
            modifier = Modifier.padding(top = 40.dp),
        ) {
            Text("スタート")
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
