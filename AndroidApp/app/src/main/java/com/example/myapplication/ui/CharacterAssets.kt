package com.example.myapplication.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MyApplicationTheme

// キャラシート(Code_Generated_Image.png)のS0〜S20、21段階をそのまま切り出したもの。
// 吸った本数(0〜20本)がそのままインデックスに対応する。
private val stageDrawables = listOf(
    R.drawable.char_stage_0, R.drawable.char_stage_1, R.drawable.char_stage_2,
    R.drawable.char_stage_3, R.drawable.char_stage_4, R.drawable.char_stage_5,
    R.drawable.char_stage_6, R.drawable.char_stage_7, R.drawable.char_stage_8,
    R.drawable.char_stage_9, R.drawable.char_stage_10, R.drawable.char_stage_11,
    R.drawable.char_stage_12, R.drawable.char_stage_13, R.drawable.char_stage_14,
    R.drawable.char_stage_15, R.drawable.char_stage_16, R.drawable.char_stage_17,
    R.drawable.char_stage_18, R.drawable.char_stage_19, R.drawable.char_stage_20,
)

val stageColorNames = listOf(
    "ホワイト", "アイスブルー", "シアン", "ターコイズ", "ミントグリーン", "ライム", "イエローグリーン",
    "イエロー", "ゴールド", "オレンジ", "ダークオレンジ", "レッド", "ローズ", "マゼンタ", "パープル",
    "インディゴ", "ブルー", "ディープブルー", "ネイビー", "ダークネイビー", "ブラック",
)

private val DotRing = Color(0xFF1A1A1A)

/**
 * キャラクター本体。背景に何も敷かず、黒いドットの輪郭だけで縁取る。
 * 明るい色のステージ(ホワイト等)でも背景に埋もれないようにするための最小限の処理。
 */
@Composable
fun CharacterView(stage: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.dp.toPx()
            drawCircle(
                color = DotRing,
                radius = (size.minDimension / 2f) - strokeWidth,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(0.1f, strokeWidth * 2.6f),
                        phase = 0f,
                    ),
                ),
            )
        }
        Image(
            painter = painterResource(stageDrawables[stage]),
            contentDescription = "キャラクター(S$stage ${stageColorNames[stage]})",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(0.62f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterViewPreview() {
    MyApplicationTheme {
        CharacterView(stage = 12, modifier = Modifier.size(200.dp))
    }
}
