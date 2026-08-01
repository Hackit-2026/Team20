package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.example.myapplication.logic.CharacterStage

// アプリ全体で使う静かなパレット(ui パッケージ内で共有)。
val ScreenBg = Color(0xFFE7E9EA)
val Plate = Color(0xFFFFFFFF)
val Ink = Color(0xFF20242B)
val Muted = Color(0xFF6B7280)
val MutedSoft = Color(0xFF9AA1AB)
val Accent = Color(0xFF2E5C56)

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
    // 端末のタイムゾーンに関わらず、常に日本時間の日付を表示する
    val today = remember {
        LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("M/d"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 上部: 設定(歯車)ボタンのみ、右端に。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .border(1.5.dp, Muted, CircleShape)
                    .clickable(onClick = onNavigateToCalendar),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⚙", color = Ink, fontSize = 16.sp)
            }
        }

        // 日付(日本時間)を中央に大きく表示
        Text(
            text = today,
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        // 項目名(今後の複数項目対応の置き場所。現状は禁煙のみ)
        Text(
            text = "禁煙",
            color = Muted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )

        // 残り日数を画面の主役として中央に大きく
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(text = "残り", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 14.dp))
            Text(
                text = "$remainingDays",
                color = Accent,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(text = "日", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 14.dp))
        }

        if (remainingDays <= 0) {
            Button(
                onClick = onNavigateToResult,
                modifier = Modifier.padding(top = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("結果を見る")
            }
        }

        // 黒いドットで縁取ったキャラクター本体(色が薄いステージでも埋もれないように)
        CharacterView(
            stage = stage,
            modifier = Modifier
                .size(200.dp)
                .padding(top = 24.dp),
        )

        // 本数ステッパー: -1 / 本数 / +1 を1列に並べる
        Row(
            modifier = Modifier.padding(top = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$todayCount", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "本",
                    color = Muted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
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
        Text(
            text = "目標 ${dailyGoal}本まで",
            color = MutedSoft,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp),
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
