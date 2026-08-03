package com.example.myapplication.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * stage: 0(健康) 〜 20(ヤニモンスター)。呼び出し側でcoerceIn(0, 20)して渡す。
 * 「羽を広げた状態(元絵そのまま)」と「羽を閉じた状態(res/drawable/char_stage_N_closed.png、
 * 羽の部分を取り除いた静止画)」の2枚を交互に切り替えて羽ばたきを表現する(パーツ分割はせず全身画像のswap)。
 */
@Composable
fun CharacterView(
    stage: Int,
    modifier: Modifier = Modifier,
) {
    val clampedStage = stage.coerceIn(0, 20)

    val infiniteTransition = rememberInfiniteTransition(label = "characterIdle")

    // 全体: 呼吸(拡縮)+ふわふわ上下移動
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bobOffset",
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )

    // 疑似まばたき: 数秒おきに全体を一瞬だけ縦につぶして戻す
    val blinkScaleY = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500, 5000))
            blinkScaleY.animateTo(0.15f, animationSpec = tween(durationMillis = 80))
            blinkScaleY.animateTo(1f, animationSpec = tween(durationMillis = 120))
        }
    }

    // 羽ばたき: 「広げた状態(元絵)」⇔「閉じた状態(羽なし)」を交互に切り替える
    var wingsClosed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            wingsClosed = !wingsClosed
        }
    }

    Box(
        modifier = modifier
            .size(160.dp)
            .graphicsLayer {
                translationY = bobOffset
                scaleX = breatheScale
                scaleY = breatheScale * blinkScaleY.value
            },
    ) {
        Crossfade(targetState = wingsClosed, label = "wingsClosed") { closed ->
            Image(
                painter = painterResource(
                    id = if (closed) closedWingsRes[clampedStage] else openWingsRes[clampedStage],
                ),
                contentDescription = "キャラクター Stage $clampedStage",
                modifier = Modifier.size(160.dp),
            )
        }
    }
}

// 羽を広げた状態(元絵そのまま)
private val openWingsRes = intArrayOf(
    R.drawable.char_stage_0, R.drawable.char_stage_1, R.drawable.char_stage_2,
    R.drawable.char_stage_3, R.drawable.char_stage_4, R.drawable.char_stage_5,
    R.drawable.char_stage_6, R.drawable.char_stage_7, R.drawable.char_stage_8,
    R.drawable.char_stage_9, R.drawable.char_stage_10, R.drawable.char_stage_11,
    R.drawable.char_stage_12, R.drawable.char_stage_13, R.drawable.char_stage_14,
    R.drawable.char_stage_15, R.drawable.char_stage_16, R.drawable.char_stage_17,
    R.drawable.char_stage_18, R.drawable.char_stage_19, R.drawable.char_stage_20,
)

// 羽を閉じた状態(羽の部分を取り除いた静止画)
private val closedWingsRes = intArrayOf(
    R.drawable.char_stage_0_closed, R.drawable.char_stage_1_closed, R.drawable.char_stage_2_closed,
    R.drawable.char_stage_3_closed, R.drawable.char_stage_4_closed, R.drawable.char_stage_5_closed,
    R.drawable.char_stage_6_closed, R.drawable.char_stage_7_closed, R.drawable.char_stage_8_closed,
    R.drawable.char_stage_9_closed, R.drawable.char_stage_10_closed, R.drawable.char_stage_11_closed,
    R.drawable.char_stage_12_closed, R.drawable.char_stage_13_closed, R.drawable.char_stage_14_closed,
    R.drawable.char_stage_15_closed, R.drawable.char_stage_16_closed, R.drawable.char_stage_17_closed,
    R.drawable.char_stage_18_closed, R.drawable.char_stage_19_closed, R.drawable.char_stage_20_closed,
)

@Preview(showBackground = true)
@Composable
private fun CharacterViewHealthyPreview() {
    MyApplicationTheme {
        CharacterView(stage = 0)
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterViewMonsterPreview() {
    MyApplicationTheme {
        CharacterView(stage = 20)
    }
}
