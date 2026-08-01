package com.example.myapplication.notify

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

// 通知リマインダーの設定画面。AppNavigationに "settings" ルートとして組み込める
@Composable
fun ReminderScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 画面の状態(値が変わると自動で描き直される)
    var enabled by remember { mutableStateOf(Reminder.isEnabled(context)) }
    var hour by remember { mutableIntStateOf(Reminder.savedHour(context)) }
    var minute by remember { mutableIntStateOf(Reminder.savedMinute(context)) }
    var showPicker by remember { mutableStateOf(false) }

    fun timeText() = "%02d:%02d".format(Locale.JAPAN, hour, minute)

    // 時刻を確定して(オンなら)アラームを予約し直す共通処理
    fun applyTime(newHour: Int, newMinute: Int) {
        hour = newHour
        minute = newMinute
        if (enabled) {
            Reminder.enableDaily(context, hour, minute)
            Toast.makeText(context, "毎日 ${timeText()} に通知します", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("記録リマインダー", style = MaterialTheme.typography.headlineSmall)

        // ── メインカード:オン/オフと時刻表示 ──
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("毎日の通知", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (enabled) "オン" else "オフ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            enabled = on
                            if (on) {
                                Reminder.enableDaily(context, hour, minute)
                                Toast.makeText(
                                    context, "毎日 ${timeText()} に通知します", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Reminder.disableDaily(context)
                                Toast.makeText(context, "通知をオフにしました", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // 大きな時刻表示。タップで時計ダイアログが開く
                Text(
                    text = timeText(),
                    style = MaterialTheme.typography.displayLarge,
                    color = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { showPicker = true }
                        .padding(vertical = 8.dp)
                )
                Text(
                    "タップして時刻を変更",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // ── ワンタッププリセット ──
        Text("かんたん設定", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { applyTime(9, 0) }, label = { Text("朝 9:00") })
            AssistChip(onClick = { applyTime(13, 30) }, label = { Text("昼 13:30") })
            AssistChip(onClick = { applyTime(21, 0) }, label = { Text("夜 21:00") })
        }

        HorizontalDivider()

        // ── 動作確認用(発表デモ向け) ──
        Text(
            "動作確認用",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { Reminder.showNow(context) }) {
                Text("いますぐ通知")
            }
            OutlinedButton(onClick = {
                Reminder.scheduleIn(context, seconds = 10)
                Toast.makeText(context, "10秒後に通知します", Toast.LENGTH_SHORT).show()
            }) {
                Text("10秒後に通知")
            }
        }
    }

    // ── 時刻選択ダイアログ ──
    if (showPicker) {
        TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onConfirm = { h, m ->
                showPicker = false
                applyTime(h, m)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("通知時刻を選択") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("決定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}
