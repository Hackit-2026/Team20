package com.example.myapplication.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.myapplication.data.AppRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ペナルティ中(=今日「吸った」と申告している間)に、他のアプリを開いたことを
 * 検知して警告オーバーレイを重ねて表示するための常駐サービス。
 *
 * 「アプリを使う時にメッセージを表示する。通知ではない」という要件のための実装。
 * UsageStatsManagerでフォアグラウンドアプリの切り替わりをポーリングし、
 * 自アプリ以外に切り替わった瞬間だけオーバーレイを表示する。
 */
class PenaltyWatcherService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var watching = false
    private var overlayView: View? = null
    private var lastForegroundPackage: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(FOREGROUND_NOTIF_ID, buildForegroundNotification())
        if (!watching) {
            watching = true
            startWatching()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        removeOverlay()
    }

    private fun startWatching() {
        val repo = AppRepo(applicationContext)
        scope.launch {
            while (isActive) {
                val fg = getForegroundPackage()
                val report = repo.getTodayReport()
                val penaltyActive = report.reported && report.smoked

                if (!penaltyActive) {
                    removeOverlay()
                } else if (fg != null && fg != packageName && fg != lastForegroundPackage) {
                    showOverlay(report.count)
                } else if (fg == packageName) {
                    removeOverlay()
                }
                if (fg != null) lastForegroundPackage = fg

                delay(1500)
            }
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val end = System.currentTimeMillis()
        val begin = end - 10_000
        val events = usm.queryEvents(begin, end)
        val event = UsageEvents.Event()
        var result: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                result = event.packageName
            }
        }
        return result
    }

    private fun showOverlay(count: Int) {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding((28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        card.addView(TextView(this).apply {
            text = "⚠ ペナルティ中"
            setTextColor(Color.parseColor("#B3261E"))
            textSize = 20f
            gravity = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text = "今日は${count}本吸ったと申告されています\n禁煙を続けましょう"
            setTextColor(Color.parseColor("#20242B"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, (16 * density).toInt(), 0, (20 * density).toInt())
        })
        card.addView(Button(this).apply {
            text = "閉じる"
            setOnClickListener { removeOverlay() }
        })

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(
                (280 * density).toInt(),
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER }
        )

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            0,
            PixelFormat.TRANSLUCENT,
        )

        try {
            wm.addView(root, params)
            overlayView = root
        } catch (e: Exception) {
            // オーバーレイ権限が取り消されている等、失敗しても常駐サービス自体は落とさない
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
        } catch (e: Exception) {
            // no-op
        }
        overlayView = null
    }

    private fun buildForegroundNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WATCHER_CHANNEL_ID, "見守りサービス", NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, WATCHER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("禁煙サポートが動作中です")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val FOREGROUND_NOTIF_ID = 100
        private const val WATCHER_CHANNEL_ID = "watcher_service"

        fun start(context: Context) {
            val intent = Intent(context, PenaltyWatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
