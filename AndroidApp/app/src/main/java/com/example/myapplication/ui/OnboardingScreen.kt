package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class OnboardingMode { REDUCE, QUIT }

/**
 * 初回起動時の最初の画面。減煙モード/完全禁煙モードの2択を表示する。
 * 減煙モード: 1日あたりの平均本数を入力させ、それを初期目標にする。
 * 完全禁煙モード: 入力なしで初期目標を0本にする。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: (initialDailyGoal: Int) -> Unit) {
    var selectedMode by remember { mutableStateOf<OnboardingMode?>(null) }
    var averageInput by remember { mutableStateOf("") }

    val canStart = when (selectedMode) {
        OnboardingMode.REDUCE -> averageInput.toIntOrNull()?.let { it >= 0 } == true
        OnboardingMode.QUIT -> true
        null -> false
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScreenBg)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ヤニモグラ",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = "まずはどちらのモードで始めますか？",
                fontSize = 14.sp,
                color = Muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )

            // 減煙モード
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { selectedMode = OnboardingMode.REDUCE },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMode == OnboardingMode.REDUCE) Color(0xFFE8F5E9) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedMode == OnboardingMode.REDUCE,
                            onClick = { selectedMode = OnboardingMode.REDUCE },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🌱 減煙モード",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                            )
                            Text(
                                text = "今の本数から少しずつ減らしていく",
                                fontSize = 12.sp,
                                color = MutedSoft,
                            )
                        }
                    }

                    if (selectedMode == OnboardingMode.REDUCE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = averageInput,
                            onValueChange = { averageInput = it.filter { c -> c.isDigit() } },
                            label = { Text("1日あたりの平均本数") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 完全禁煙モード
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { selectedMode = OnboardingMode.QUIT },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedMode == OnboardingMode.QUIT) Color(0xFFFFEBEE) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedMode == OnboardingMode.QUIT,
                        onClick = { selectedMode = OnboardingMode.QUIT },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "🚫 完全禁煙モード",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                        Text(
                            text = "0本からスタートする",
                            fontSize = 12.sp,
                            color = MutedSoft,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val goal = when (selectedMode) {
                        OnboardingMode.REDUCE -> averageInput.toIntOrNull() ?: 0
                        OnboardingMode.QUIT -> 0
                        null -> 0
                    }
                    onComplete(goal)
                },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("はじめる", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
