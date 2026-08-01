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

private enum class OverlayKind { NONE, LIGHT, HEAVY }

/**
 * ペナルティ中に、他のアプリを開いたことを検知して警告オーバーレイを重ねて
 * 表示するための常駐サービス。「アプリを使う時にメッセージを表示する。通知ではない」
 * という絶対要件のための実装。
 *
 * 2段階ある:
 * - LIGHT: 今日「吸った」と申告している間、閉じるボタンで即座に閉じられる警告
 * - HEAVY: 過去7日間の合計が2本以上のとき、60秒間閉じられない強めの警告
 *
 * UsageStatsManagerでフォアグラウンドアプリの切り替わりをポーリングし、
 * 自アプリ以外に切り替わった瞬間だけオーバーレイを表示する。
 */
class PenaltyWatcherService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var watching = false
    private var overlayView: View? = null
    private var currentOverlayKind: OverlayKind = OverlayKind.NONE
    private var stableForeignPackage: String? = null
    private var stableForeignStreak: Int = 0

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

                if (fg == null || fg == packageName) {
                    // 自アプリを使っている間は出さない。権限ダイアログ等がusage統計上
                    // 一瞬別パッケージとして記録されることがあるので、その揺れも
                    // ここでリセットしてノイズとして扱う。
                    removeOverlay()
                    stableForeignPackage = null
                    stableForeignStreak = 0
                } else {
                    // 同じ他アプリが2回連続(=3秒)観測できて初めて「本当に他アプリへ
                    // 切り替わった」とみなす。1回だけの揺れで即座に出さないため。
                    stableForeignStreak = if (fg == stableForeignPackage) stableForeignStreak + 1 else 1
                    stableForeignPackage = fg

                    if (stableForeignStreak >= 2) {
                        val report = repo.getTodayReport()
                        val weeklyTotal = repo.getWeeklyTotal()
                        val desiredKind = when {
                            weeklyTotal >= 2 -> OverlayKind.HEAVY
                            report.reported && report.smoked -> OverlayKind.LIGHT
                            else -> OverlayKind.NONE
                        }
                        if (desiredKind == OverlayKind.NONE) {
                            removeOverlay()
                        } else if (overlayView == null || currentOverlayKind != desiredKind) {
                            removeOverlay()
                            when (desiredKind) {
                                OverlayKind.HEAVY -> showHeavyOverlay(weeklyTotal)
                                OverlayKind.LIGHT -> showLightOverlay(report.count)
                                OverlayKind.NONE -> {}
                            }
                        }
                    }
                }

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

    private fun showLightOverlay(count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
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

        if (addOverlayView(root)) {
            currentOverlayKind = OverlayKind.LIGHT
        }
    }

    /** 週2本以上の重いペナルティ。60秒間は閉じるボタンを出さず操作を受け付けない。 */
    private fun showHeavyOverlay(weeklyTotal: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        val density = resources.displayMetrics.density

        val countdownText = TextView(this).apply {
            setTextColor(Color.parseColor("#8A9099"))
            textSize = 12f
            gravity = Gravity.CENTER
            text = "あと60秒は操作できません"
        }
        val closeButton = Button(this).apply {
            text = "閉じる"
            visibility = View.GONE
            setOnClickListener { removeOverlay() }
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((32 * density).toInt(), (32 * density).toInt(), (32 * density).toInt(), (32 * density).toInt())
        }
        card.addView(TextView(this).apply {
            text = "もっと禁煙してください！"
            setTextColor(Color.parseColor("#FF6B6B"))
            textSize = 24f
            gravity = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text = "今週の合計: ${weeklyTotal}本"
            setTextColor(Color.parseColor("#CBD0D6"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, (12 * density).toInt(), 0, (24 * density).toInt())
        })
        card.addView(countdownText)
        card.addView(closeButton)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#FF1A1418"))
        }
        root.addView(
            card,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ).apply { gravity = Gravity.CENTER }
        )

        if (!addOverlayView(root)) return
        currentOverlayKind = OverlayKind.HEAVY

        scope.launch {
            for (secondsLeft in 59 downTo 1) {
                delay(1000)
                if (overlayView !== root) return@launch // 途中で閉じられた/差し替わった場合は打ち切り
                countdownText.text = "あと${secondsLeft}秒は操作できません"
            }
            delay(1000)
            if (overlayView !== root) return@launch
            countdownText.text = ""
            closeButton.visibility = View.VISIBLE
        }
    }

    /** WindowManagerにビューを追加する共通処理。成功したらtrue。 */
    private fun addOverlayView(root: View): Boolean {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
        return try {
            wm.addView(root, params)
            overlayView = root
            true
        } catch (e: Exception) {
            // オーバーレイ権限が取り消されている等、失敗しても常駐サービス自体は落とさない
            false
        }
    }

    private fun removeOverlay() {
        val view = overlayView
        if (view != null) {
            try {
                (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
            } catch (e: Exception) {
                // no-op
            }
        }
        overlayView = null
        currentOverlayKind = OverlayKind.NONE
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
