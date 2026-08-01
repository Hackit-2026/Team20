package com.example.myapplication.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import java.util.Calendar

object Reminder {
    private const val CHANNEL_ID = "daily_reminder"
    private const val REQ_DAILY = 1001
    private const val REQ_TEST = 1002
    private const val PREFS = "reminder_prefs"

    // ── 設定の保存・読み出し(SharedPreferences = アプリ専用の小さな保存領域) ──

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun savedHour(context: Context) = prefs(context).getInt("hour", 13)
    fun savedMinute(context: Context) = prefs(context).getInt("minute", 30)

    // ── 通知チャンネル(起動時に1回呼ぶ。複数回呼んでも害なし) ──

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(
                CHANNEL_ID, "記録リマインダー", NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }

    // ── いますぐ通知を出す(動作確認用) ──

    fun showNow(context: Context) {
        val tap = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("今日は何本吸った?")
            .setContentText("記録してキャラを観察しよう!")
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1, notif)
    }

    // ── n秒後に通知(発表用テスト通知) ──

    fun scheduleIn(context: Context, seconds: Int) {
        val at = System.currentTimeMillis() + seconds * 1000L
        setAlarm(context, at, pending(context, REQ_TEST, daily = false))
    }

    // ── 毎日通知のオン(時刻を保存してアラームをセット) ──

    fun enableDaily(context: Context, hour: Int, minute: Int) {
        prefs(context).edit()
            .putInt("hour", hour)
            .putInt("minute", minute)
            .putBoolean("enabled", true)
            .apply()
        scheduleNext(context)
    }

    // ── 毎日通知のオフ(設定を無効化してアラームを取り消し) ──

    fun disableDaily(context: Context) {
        prefs(context).edit().putBoolean("enabled", false).apply()
        context.getSystemService(AlarmManager::class.java)
            .cancel(pending(context, REQ_DAILY, daily = true))
    }

    // ── 保存済みの時刻で「次の1回」を予約(翌日分はReceiverが再予約する) ──

    fun scheduleNext(context: Context) {
        if (!isEnabled(context)) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, savedHour(context))
            set(Calendar.MINUTE, savedMinute(context))
            set(Calendar.SECOND, 0)
            // 指定時刻が過ぎていたら翌日にずらす
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        setAlarm(context, cal.timeInMillis, pending(context, REQ_DAILY, daily = true))
    }

    private fun pending(context: Context, req: Int, daily: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context, req,
            Intent(context, ReminderReceiver::class.java).putExtra("daily", daily),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun setAlarm(context: Context, at: Long, pi: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}
