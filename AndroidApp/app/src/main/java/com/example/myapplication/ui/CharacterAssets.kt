package com.example.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

val stageEmoji = listOf("😊", "😮‍💨", "😵", "👹")
val stageName = listOf("けんこう", "喫煙し始め", "ヘビースモーカー", "ヤニモンスター")
val stageLines = listOf(
    listOf("今日も空気がうまい!", "肺がピカピカだよ"),
    listOf("ちょっとだけ…ちょっとだけだから…", "のどがイガイガするかも"),
    listOf("ライター…ライターどこ…", "飯より一服なんだよなぁ"),
    listOf("モクをよこせェェ!!", "換気扇の下がワシの玉座じゃ"),
)
val stageBackground = listOf(
    Color(0xFFFFFFFF),
    Color(0xFFFFF6D8),
    Color(0xFFF2E0A8),
    Color(0xFF3A3A3A),
)

@Composable
fun CharacterView(stage: Int, modifier: Modifier = Modifier) {
    val background by animateColorAsState(targetValue = stageBackground[stage], label = "stageBackground")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stageEmoji[stage],
            fontSize = 96.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterViewPreview() {
    MyApplicationTheme {
        CharacterView(stage = 3)
    }
}
