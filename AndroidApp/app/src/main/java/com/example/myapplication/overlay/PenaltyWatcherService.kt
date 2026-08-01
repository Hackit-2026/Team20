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
                val isHeavyActive = repo.isHeavyPenaltyActive()
                val penaltyVal = repo.calculateWeightedPenaltyValue()

                if (penaltyVal >= 5.0 && !isHeavyActive) {
                    repo.triggerHeavyPenaltyLock()
                }

                val currentHeavyActive = repo.isHeavyPenaltyActive()

                if (currentHeavyActive) {
                    if (overlayView == null || currentOverlayKind != OverlayKind.HEAVY) {
                        removeOverlay()
                        showHeavyOverlay(repo)
                    }
                } else if (fg == null || fg == packageName) {
                    removeOverlay()
                    stableForeignPackage = null
                    stableForeignStreak = 0
                } else {
                    stableForeignStreak = if (fg == stableForeignPackage) stableForeignStreak + 1 else 1
                    stableForeignPackage = fg

                    if (stableForeignStreak >= 2) {
                        val desiredKind = when {
                            penaltyVal >= 2.0 -> OverlayKind.LIGHT
                            else -> OverlayKind.NONE
                        }
                        if (desiredKind == OverlayKind.NONE) {
                            removeOverlay()
                        } else if (overlayView == null || currentOverlayKind != desiredKind) {
                            removeOverlay()
                            if (desiredKind == OverlayKind.LIGHT) {
                                showLightOverlay(penaltyVal)
                            }
                        }
                    }
                }

                delay(1000)
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

    private fun showLightOverlay(penaltyVal: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return
        val density = resources.displayMetrics.density

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding((28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt(), (28 * density).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }
        card.addView(TextView(this).apply {
            text = "⚠ ペナルティ注意"
            setTextColor(Color.parseColor("#B3261E"))
            textSize = 20f
            gravity = Gravity.CENTER
        })
        card.addView(TextView(this).apply {
            text = String.format("直近6日間のペナルティ指数: %.1f\n禁煙ペースを守りましょう！", penaltyVal)
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

    /** 🚨 重度ペナルティ: 60秒間カウントダウン。タイマー完了時に「閉じる」を押して解除 */
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
                repo.clearHeavyPenaltyLock() // 🚨 カウントダウン完了後に閉じるを押して初めてロック解除！
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
            text = String.format("重み付きペナルティ指数: %.1f (5.0以上)", repo.calculateWeightedPenaltyValue())
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
