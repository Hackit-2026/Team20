package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import com.example.myapplication.logic.CharacterStage

// この画面だけのローカルパレット。「説教しないアプリです、ただ鏡を置くだけ」という
// コンセプトに合わせて、彩度を落とした静かな背景 + 深いティールのアクセント1色のみ。
private val ScreenBg = Color(0xFFE7E9EA)
private val Plate = Color(0xFFFFFFFF)
private val Ink = Color(0xFF20242B)
private val Muted = Color(0xFF6B7280)
private val MutedSoft = Color(0xFF9AA1AB)
private val Accent = Color(0xFF2E5C56)

@Composable
fun HomeScreen(
    todayCount: Int,
    dailyGoal: Int,
    remainingDays: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stage = remember(todayCount) { CharacterStage.fromCount(todayCount) }
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("M/d")) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 上部プレート: 日付 + 履歴への導線
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Plate)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = today, color = Muted, fontSize = 13.sp)
            TextButton(onClick = onNavigateToCalendar) {
                Text("履歴")
            }
        }

        // 残り日数を主役級に大きく見せる
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Accent)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "残り", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
            Text(
                text = "$remainingDays",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(text = "日", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
        }

        if (remainingDays <= 0) {
            Button(
                onClick = onNavigateToResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink),
            ) {
                Text("結果を見る")
            }
        }

        // 鏡フレームに入ったキャラクター本体
        CharacterView(
            stage = stage,
            modifier = Modifier
                .size(220.dp)
                .padding(top = 20.dp),
        )
        Text(
            text = "S$stage ・ ${stageColorNames[stage]}",
            color = MutedSoft,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        // 本数ステッパー(数字を主役に、単位は添え字として横に添える)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Plate)
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$todayCount",
                    color = Ink,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "本",
                    color = Muted,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                )
            }
            Text(
                text = "目標 ${dailyGoal}本まで",
                color = MutedSoft,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDecrement,
                    enabled = todayCount > 0,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("−", fontSize = 22.sp)
                }
                Button(
                    onClick = onIncrement,
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "説教しないアプリです。ただ、鏡を置くだけ。",
            color = MutedSoft,
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreen(
            todayCount = 12,
            dailyGoal = 5,
            remainingDays = 4,
            onIncrement = {},
            onDecrement = {},
            onNavigateToCalendar = {},
            onNavigateToResult = {},
        )
    }
}
