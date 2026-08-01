package com.example.myapplication.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MyApplicationTheme

// キャラシートのS0〜S20、21段階の実アセット画像
private val stageDrawables = listOf(
    R.drawable.char_stage_0, R.drawable.char_stage_1, R.drawable.char_stage_2,
    R.drawable.char_stage_3, R.drawable.char_stage_4, R.drawable.char_stage_5,
    R.drawable.char_stage_6, R.drawable.char_stage_7, R.drawable.char_stage_8,
    R.drawable.char_stage_9, R.drawable.char_stage_10, R.drawable.char_stage_11,
    R.drawable.char_stage_12, R.drawable.char_stage_13, R.drawable.char_stage_14,
    R.drawable.char_stage_15, R.drawable.char_stage_16, R.drawable.char_stage_17,
    R.drawable.char_stage_18, R.drawable.char_stage_19, R.drawable.char_stage_20,
)

val stageGlow = listOf(
    Color(0xFFFFFFFF), Color(0xFFCFF8FF), Color(0xFF7EEBFF), Color(0xFF3FD9D6),
    Color(0xFF72F0A6), Color(0xFFA9F542), Color(0xFFCBEF36), Color(0xFFFFE14A),
    Color(0xFFFFC53B), Color(0xFFFF9836), Color(0xFFFF7230), Color(0xFFF54848),
    Color(0xFFF33A7A), Color(0xFFD93BD9), Color(0xFFA552F5), Color(0xFF6558F0),
    Color(0xFF3F78F0), Color(0xFF2755C8), Color(0xFF1C2F78), Color(0xFF0E1638),
    Color(0xFF0A0A0A),
)

val stageColorNames = listOf(
    "ホワイト", "アイスブルー", "シアン", "ターコイズ", "ミントグリーン", "ライム", "イエローグリーン",
    "イエロー", "ゴールド", "オレンジ", "ダークオレンジ", "レッド", "ローズ", "マゼンタ", "パープル",
    "インディゴ", "ブルー", "ディープブルー", "ネイビー", "ダークネイビー", "ブラック",
)

/**
 * 鏡モチーフのフレーム ＋ グロー（発光）演出でキャラクターを描画する
 */
@Composable
fun CharacterView(stage: Int, modifier: Modifier = Modifier) {
    val safeStage = stage.coerceIn(0, stageDrawables.lastIndex)
    val glow by animateColorAsState(targetValue = stageGlow[safeStage], label = "stageGlow")

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        // 背景ににじむグロー
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .background(
                    Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0f)))
                )
        )
        // 鏡ベゼル + 暗いスクリーン
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color(0xFF12151B)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(stageDrawables[safeStage]),
                contentDescription = "キャラクター(S$safeStage ${stageColorNames[safeStage]})",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.66f),
            )
        }
    }
}

@Composable
fun CharacterView(stageIndex: Int, modifier: Modifier = Modifier, dummy: Boolean = false) {
    CharacterView(stage = stageIndex, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
private fun CharacterViewPreview() {
    MyApplicationTheme {
        CharacterView(stage = 12, modifier = Modifier.size(220.dp))
    }
}
