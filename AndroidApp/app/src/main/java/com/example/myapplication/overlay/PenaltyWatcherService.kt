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
import com.example.myapplication.util.WallpaperHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class OverlayKind { NONE, HEAVY }

class PenaltyWatcherService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var watching = false
    private var overlayView: View? = null
    private var currentOverlayKind: OverlayKind = OverlayKind.NONE
    private var lastKnownForegroundPackage: String? = null
    private var lastEventQueryTime: Long = System.currentTimeMillis() - 10_000

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
                // 🐣 ヤニモグラ自身を使っている間は壁紙変更・警告・操作ロックを一切出さない。
                // 他アプリを開いている時だけ発生させる(保存直後の当日は発動せず、日付が変わってから発動する)。
                val usingOtherApp = fg != null && fg != packageName

                val penaltyVal = repo.calculateWeightedPenaltyValue()
                val weeklyTotal = repo.getWeeklyTotalExcludingToday()
                val shouldHeavy = penaltyVal >= 5.0 || weeklyTotal >= 2

                if (!usingOtherApp) {
                    if (currentOverlayKind != OverlayKind.NONE) {
                        removeOverlay()
                    }
                } else {
                    if (shouldHeavy && !repo.isHeavyPenaltyActive()) {
                        repo.triggerHeavyPenaltyLock()
                        // 🚨 スマホ端末本体のシステム壁紙を「重度ペナルティ危険警告壁紙」に変更
                        WallpaperHelper.setPenaltyWallpaper(applicationContext)
                    }

                    if (repo.isHeavyPenaltyActive()) {
                        if (overlayView == null || currentOverlayKind != OverlayKind.HEAVY) {
                            removeOverlay()
                            showHeavyOverlay(repo)
                        }
                    } else if (currentOverlayKind != OverlayKind.NONE) {
                        removeOverlay()
                    }
                }

                delay(1000)
            }
        }
    }

    /**
     * 直近のMOVE_TO_FOREGROUNDイベントを見て「今どのアプリが前面にいるか」を返す。
     * 固定窓(例: 直近10秒)だけを見ると、ユーザーが1つのアプリに10秒以上とどまった瞬間に
     * 新規イベントが窓から外れてnullに戻ってしまう(=前面判定を見失う)ため、
     * 前回クエリした時刻から現在までを毎回積み上げて見ることで、最後に検出した前面アプリを
     * 新しいイベントが来るまで保持し続ける。
     */
    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return lastKnownForegroundPackage
        val end = System.currentTimeMillis()
        val begin = lastEventQueryTime
        val events = usm.queryEvents(begin, end)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastKnownForegroundPackage = event.packageName
            }
        }
        lastEventQueryTime = end
        return lastKnownForegroundPackage
    }

    private fun showHeavyOverlay(repo: AppRepo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        val density = resources.displayMetrics.density

        val countdownText = TextView(this).apply {
            setTextColor(Color.parseColor("#8A9099"))
            textSize = 14f
            gravity = Gravity.CENTER
            text = "あと${repo.getRemainingPenaltySeconds()}秒は操作できません"
        }
        val closeButton = Button(this).apply {
            text = "閉じる"
            visibility = View.GONE
            setOnClickListener {
                repo.clearHeavyPenaltyLock()
                removeOverlay()
            }
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
            text = String.format("重度ペナルティ (今週合計: %d本)", repo.getWeeklyTotalExcludingToday())
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
            while (isActive && repo.isHeavyPenaltyActive()) {
                val rem = repo.getRemainingPenaltySeconds()
                if (rem > 0) {
                    countdownText.text = "あと${rem}秒は操作できません"
                } else {
                    break
                }
                delay(1000)
            }
            if (overlayView === root) {
                countdownText.text = "ペナルティ時間が経過しました"
                closeButton.visibility = View.VISIBLE
            }
        }
    }

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
            false
        }
    }

    private fun removeOverlay() {
        val view = overlayView
        if (view != null) {
            try {
                (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view)
            } catch (e: Exception) {
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
