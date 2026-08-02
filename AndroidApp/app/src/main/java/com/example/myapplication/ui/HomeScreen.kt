package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.DailyReport
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.WallpaperHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val context = LocalContext.current
    val today = remember {
        LocalDate.now(ZoneId.of("Asia/Tokyo")).format(DateTimeFormatter.ofPattern("M/d"))
    }
    val report = uiState.today

    // 🚨 【要件判定】1週間に2本以上吸っている、またはペナルティ指数5.0以上の重度ペナルティ時
    val isHeavyWeeklyPenalty = uiState.weeklyTotal >= 2 || uiState.weightedPenaltyValue >= 5.0 || uiState.isHeavyPenaltyActive

    // 🚨 重度ペナルティ発火時、スマホ本体の端末システム壁紙を自動で警告壁紙に変更！
    LaunchedEffect(isHeavyWeeklyPenalty) {
        if (isHeavyWeeklyPenalty) {
            WallpaperHelper.setPenaltyWallpaper(context)
        }
    }

    val currentBgColor = if (isHeavyWeeklyPenalty) PenaltyWallpaperBg else ScreenBg
    val textColor = if (isHeavyWeeklyPenalty) Color.White else Ink

    // ✨ マイナスボタンを押した際のエフェクト状態
    var showMinusEffect by remember { mutableStateOf(false) }

    // 🚨 警告壁紙時のパルス点滅アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(currentBgColor)
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

            // 🚨 スマホ端末壁紙変更 ＆ アプリ内警告壁紙バナー
            if (isHeavyWeeklyPenalty) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .alpha(pulseAlpha),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF1744)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🚨 【警告壁紙変更完了】今週合計: ${uiState.weeklyTotal}本\nスマホのシステム壁紙がペナルティ警告壁紙に変更されました",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }

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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )

            GoalLine(daysUntilGoal = uiState.daysUntilGoal, achieved = uiState.goalAchieved, isPenalty = isHeavyWeeklyPenalty)

            StatusLine(report)

            // TODO: 動作確認用の一時組み込み。確認後は削除する
            CharacterView(
                stage = uiState.weightedPenaltyValue.toInt(),
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            CountSection(
                tempCount = uiState.tempCount,
                onIncrementTemp = onIncrementTemp,
                onDecrementTemp = {
                    onDecrementTemp()
                    showMinusEffect = true
                },
                onSaveReport = onSaveReport,
                isPenalty = isHeavyWeeklyPenalty
            )
        }

        if (showMinusEffect) {
            MinusEffectOverlay(
                onEffectComplete = { showMinusEffect = false }
            )
        }
    }
}

@Composable
private fun MinusEffectOverlay(onEffectComplete: () -> Unit) {
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.3f,
            animationSpec = tween(durationMillis = 300)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 200)
        )
        delay(600)
        onEffectComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.scale(scale.value),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✨ 減煙成功！ -1本",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "素晴らしい！この調子で我慢しましょう🎉",
                    color = Color(0xFFE8F5E9),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
            Text("🎯 減煙設定", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        fontSize = 11.sp,
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
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun StatusLine(report: DailyReport) {
    val text = when {
        !report.reported -> "本日の申告: まだです"
        report.smoked -> "本日の申告: 吸った(${report.count}本) ・ ペナルティ中"
        else -> "本日の申告: 吸わなかった"
    }
    val color = when {
        !report.reported -> MutedSoft
        report.smoked -> Warn
        else -> Accent
    }
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun CountSection(
    tempCount: Int,
    onIncrementTemp: () -> Unit,
    onDecrementTemp: () -> Unit,
    onSaveReport: () -> Unit,
    isPenalty: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isPenalty) Color(0xFF4A1525) else Color.White),
        shape = RoundedCornerShape(20.dp)
    ) {
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
