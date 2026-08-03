package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val ScreenBg = Color(0xFFE7E9EA)
val PenaltyWallpaperBg = Color(0xFF2B0A11) // 🚨 1週間に2本以上吸った際の重度ペナルティ壁紙色
val Ink = Color(0xFF20242B)
val Muted = Color(0xFF6B7280)
val MutedSoft = Color(0xFF9AA1AB)
val Accent = Color(0xFF2E5C56)
val Warn = Color(0xFFB3261E)

@Composable
fun HomeScreen(
    uiState: UiState,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onSaveReport: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToGoalSetting: () -> Unit,
    onNavigateToDebug: () -> Unit,
    overlayPermissionGranted: Boolean = true,
    usageAccessGranted: Boolean = true,
    onRequestOverlayPermission: () -> Unit = {},
    onRequestUsageAccess: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val today = remember {
        LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("M/d"))
    }
    val report = uiState.today

    // 🚨 【要件判定】1週間に2本以上吸っている、またはペナルティ指数5.0以上の重度ペナルティ時
    // (今日の申告は含めない。壁紙変更・操作ロックはアプリ外でのみ発生するので、ここはアプリ内の配色切り替えにのみ使う)
    val isHeavyWeeklyPenalty = uiState.isPenaltyThresholdMet

    val currentBgColor = if (isHeavyWeeklyPenalty) PenaltyWallpaperBg else ScreenBg
    val textColor = if (isHeavyWeeklyPenalty) Color.White else Ink

    // ✨ 保存時、0本なら禁煙成功、開いた時点の本数より減っていれば減煙成功の演出を出す(タップするまで表示し続ける)
    var activeSuccessEffect by remember { mutableStateOf<SuccessEffectType?>(null) }
    val startingCount = remember { uiState.tempCount }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBgColor),
        // タブレットなど横長画面ではコンテンツを中央に寄せて読みやすくする
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!overlayPermissionGranted || !usageAccessGranted) {
                PermissionBanner(
                    overlayPermissionGranted = overlayPermissionGranted,
                    usageAccessGranted = usageAccessGranted,
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onRequestUsageAccess = onRequestUsageAccess,
                )
            }

            NavigationHeader(
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToGoalSetting = onNavigateToGoalSetting,
                onNavigateToDebug = onNavigateToDebug,
                isPenalty = isHeavyWeeklyPenalty
            )

            Text(
                text = today,
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp),
            )

            Text(
                text = "禁煙・減煙チャレンジ",
                color = if (isHeavyWeeklyPenalty) Color(0xFFE0B0B0) else Muted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )

            GoalLine(daysUntilGoal = uiState.daysUntilGoal, achieved = uiState.goalAchieved, isPenalty = isHeavyWeeklyPenalty)

            // 🐣 21段階成長キャラクター(前日〜1ヶ月の累積本数で変化、タップで反応)
            CharacterSection(stage = uiState.characterStage, isPenalty = isHeavyWeeklyPenalty)

            Spacer(modifier = Modifier.height(24.dp))

            CountSection(
                tempCount = uiState.tempCount,
                onIncrementTemp = onIncrementTemp,
                onDecrementTemp = onDecrementTemp,
                onSaveReport = {
                    activeSuccessEffect = when {
                        uiState.tempCount == 0 -> SuccessEffectType.QUIT
                        uiState.tempCount < startingCount -> SuccessEffectType.REDUCE
                        else -> null
                    }
                    onSaveReport()
                },
                isSavedToday = report.reported,
                isPenalty = isHeavyWeeklyPenalty
            )
        }

        activeSuccessEffect?.let { type ->
            SuccessEffectOverlay(
                type = type,
                onDismiss = { activeSuccessEffect = null },
            )
        }
    }
}

// タップ時のセリフ。成長度(悪化度)に応じて口調が変わる
private fun reactionFor(stage: Int): String {
    val lines = when {
        stage == 0 -> listOf("毎日吸ってなくてうれしいよ!", "今日も空気がおいしい!", "肺がピカピカだよ✨")
        stage <= 5 -> listOf("ちょっとヤニくさいかも…", "まだ引き返せるよ!", "のどがイガイガする…")
        stage <= 10 -> listOf("けむりが恋しくなってきた…", "最近ちょっと体が重いなあ", "そろそろ本気で減らさない?")
        stage <= 15 -> listOf("ライター…ライターどこ…", "飯より一服なんだよなぁ", "咳が止まらない…ゴホッ")
        else -> listOf("モクをよこせェェ!!", "換気扇の下がワシの玉座じゃ", "…まだ、戻れるかな…?")
    }
    return lines.random()
}

@Composable
private fun CharacterSection(stage: Int, isPenalty: Boolean) {
    var bubbleText by remember { mutableStateOf<String?>(null) }
    val charScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // 吹き出しは2.5秒後に自動で消える
    LaunchedEffect(bubbleText) {
        if (bubbleText != null) {
            delay(2500)
            bubbleText = null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp),
    ) {
        SpeechBubbleSlot(bubbleText)
        CharacterView(
            stage = stage,
            modifier = Modifier
                .padding(top = 8.dp)
                .scale(charScale.value)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    bubbleText = reactionFor(stage)
                    scope.launch {
                        charScale.animateTo(1.15f, animationSpec = tween(120))
                        charScale.animateTo(1f, animationSpec = tween(150))
                    }
                },
        )
        Text(
            text = "成長度 $stage / 20(前日までの1ヶ月累積)",
            color = if (isPenalty) Color(0xFFD0D0D0) else MutedSoft,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// 吹き出しが出てもレイアウトが動かないよう、常に同じ高さの領域を確保しておく
@Composable
private fun SpeechBubbleSlot(bubbleText: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = bubbleText != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Text(
                    text = bubbleText ?: "",
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

// ✨ 減煙/禁煙成功時のドット風エフェクト、緑の火花1粒分
private data class SparkleDot(
    val angleDeg: Float,   // 中心の四角から見た方向
    val distanceDp: Float, // 中心からの距離
    val sizeDp: Float,     // 四角のサイズ(ドット風なので小さめ)
    val delay: Float,      // 0〜1、点滅を始めるタイミングをずらす
)

private enum class SuccessEffectType { REDUCE, QUIT }

// ユーザーが画面をタップして閉じるまで表示し続ける演出。火花はループでチカチカし続ける
@Composable
private fun SuccessEffectOverlay(type: SuccessEffectType, onDismiss: () -> Unit) {
    val boxScale = remember { Animatable(0f) }  // 中央の四角がドット風にカクッと出てくる

    // 緑の火花は毎回違う位置になるようランダム生成
    val sparkles = remember {
        List(10) { i ->
            val rnd = Random(System.currentTimeMillis() + i * 17)
            SparkleDot(
                angleDeg = rnd.nextFloat() * 360f,
                distanceDp = 60f + rnd.nextFloat() * 50f,
                sizeDp = 5f + rnd.nextInt(6),
                delay = rnd.nextFloat() * 0.4f,
            )
        }
    }

    // 火花の点滅はタップされるまでループさせ続ける
    val infiniteTransition = rememberInfiniteTransition(label = "sparkleLoop")
    val loopProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1400, easing = LinearEasing)),
        label = "sparkleProgress",
    )

    LaunchedEffect(Unit) {
        // 丸みのない、カクッとした2段階のポップイン(ドット絵らしいステップ感)
        boxScale.animateTo(1.15f, animationSpec = tween(durationMillis = 120, easing = LinearEasing))
        boxScale.animateTo(1f, animationSpec = tween(durationMillis = 90, easing = LinearEasing))
    }

    val (line1, line2) = when (type) {
        SuccessEffectType.REDUCE -> "減煙" to "成功"
        SuccessEffectType.QUIT -> "禁煙" to "成功"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 緑の火花(ドット)。中心の四角の周りでチカチカ点滅する
        sparkles.forEach { s ->
            val local = (loopProgress - s.delay).let { if (it < 0f) it + 1f else it }
            val twinkle = abs(sin(local * Math.PI.toFloat() * 2.2f))
            val offsetX = (cos(Math.toRadians(s.angleDeg.toDouble())) * s.distanceDp).toFloat()
            val offsetY = (sin(Math.toRadians(s.angleDeg.toDouble())) * s.distanceDp).toFloat()
            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(s.sizeDp.dp)
                    .alpha(twinkle)
                    .background(Color(0xFF4CAF50), RectangleShape),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 中央の四角: 「減煙/禁煙」「成功」をドット風の四角の中に表示
            Box(
                modifier = Modifier
                    .scale(boxScale.value)
                    .background(Color.White, RectangleShape)
                    .border(width = 3.dp, color = Ink, shape = RectangleShape)
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = line1, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(text = line2, color = Color(0xFF4CAF50), fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }
            Text(
                text = "タップして閉じる",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun NavigationHeader(
    onNavigateToHistory: () -> Unit,
    onNavigateToGoalSetting: () -> Unit,
    onNavigateToDebug: () -> Unit,
    isPenalty: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onNavigateToHistory,
            shape = RoundedCornerShape(20.dp),
            colors = if (isPenalty) ButtonDefaults.outlinedButtonColors(contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("📊 履歴", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onNavigateToGoalSetting,
            shape = RoundedCornerShape(20.dp),
            colors = if (isPenalty) ButtonDefaults.outlinedButtonColors(contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("🎯 設定", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onNavigateToDebug,
            shape = RoundedCornerShape(20.dp),
            colors = if (isPenalty) ButtonDefaults.outlinedButtonColors(contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
        ) {
            Text("🛠️ デバッグ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionBanner(
    overlayPermissionGranted: Boolean,
    usageAccessGranted: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestUsageAccess: () -> Unit,
) {
    Text(
        text = "他アプリを開いた時に警告を出すための権限が未設定です",
        color = Warn,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        if (!overlayPermissionGranted) {
            OutlinedButton(onClick = onRequestOverlayPermission) {
                Text("重ね表示を許可", fontSize = 12.sp)
            }
        }
        if (!usageAccessGranted) {
            OutlinedButton(onClick = onRequestUsageAccess) {
                Text("使用状況アクセスを許可", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GoalLine(daysUntilGoal: Long, achieved: Boolean, isPenalty: Boolean) {
    if (achieved) {
        Text(
            text = "🎉 目標達成(3か月吸っていません)",
            color = Accent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
    } else {
        Text(
            text = "目標(3か月0本)まで残り${daysUntilGoal}日",
            color = if (isPenalty) Color(0xFFD0D0D0) else MutedSoft,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun CountSection(
    tempCount: Int,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onSaveReport: () -> Unit,
    isSavedToday: Boolean,
    isPenalty: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isPenalty) Color(0xFF4A1525) else Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
        // 保存すると次の日の0時になるまで「保存しました」のまま戻らない(uiState.today.reportedが翌日に自動でfalseに戻る)
        Crossfade(targetState = isSavedToday, label = "countSectionSaved") { saved ->
            if (saved) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✅ 保存しました",
                        color = if (isPenalty) Color.White else Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    Text(
                        text = "また明日も記録してくださいね",
                        color = if (isPenalty) Color(0xFFD0D0D0) else Muted,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "何本吸いましたか？",
                        color = if (isPenalty) Color.White else Muted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 20.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDecrementTemp,
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            colors = if (isPenalty) ButtonDefaults.outlinedButtonColors(contentColor = Color.White) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("−", fontSize = 26.sp)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(text = "$tempCount", color = if (isPenalty) Color.White else Ink, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "本",
                                color = if (isPenalty) Color(0xFFD0D0D0) else Muted,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                            )
                        }
                        Button(
                            onClick = onIncrementTemp,
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isPenalty) Color(0xFFFF6B6B) else Accent),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = onSaveReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPenalty) Color(0xFFFF4D4D) else Ink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HeavyPenaltyOverlay(
    remainingSeconds: Int,
    onDismiss: () -> Unit
) {
    val canClose = remainingSeconds <= 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1418))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "もっと禁煙してください！",
            color = Color(0xFFFF6B6B),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "重度ペナルティ中 (画面全ブロック)",
            color = Color(0xFFCBD0D6),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (canClose) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.padding(top = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("閉じる")
            }
        } else {
            Text(
                text = "あと${remainingSeconds}秒は操作できません",
                color = Color(0xFF8A9099),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenUnreportedPreview() {
    MyApplicationTheme {
        HomeScreen(
            uiState = UiState(),
            onIncrementTemp = {},
            onDecrementTemp = {},
            onSaveReport = {},
            onNavigateToHistory = {},
            onNavigateToGoalSetting = {},
            onNavigateToDebug = {}
        )
    }
}
