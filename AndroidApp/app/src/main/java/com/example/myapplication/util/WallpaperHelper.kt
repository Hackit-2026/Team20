package com.example.myapplication.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.Log

object WallpaperHelper {

    /**
     * 🚨 スマホ端末本体のシステム壁紙を「重度ペナルティ赤黒警告壁紙」に自動変更する
     */
    fun setPenaltyWallpaper(context: Context) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1080)
            val height = metrics.heightPixels.coerceAtLeast(1920)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 背景: ディープダークレッド & ブラック
            val paint = Paint().apply {
                color = Color.parseColor("#1A0509")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            // 危険警告枠
            val borderPaint = Paint().apply {
                color = Color.parseColor("#FF1744")
                style = Paint.Style.STROKE
                strokeWidth = 40f
            }
            canvas.drawRect(20f, 20f, width.toFloat() - 20f, height.toFloat() - 20f, borderPaint)

            // 警告テキスト
            val textPaint = Paint().apply {
                color = Color.parseColor("#FF5252")
                textSize = 72f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val subTextPaint = Paint().apply {
                color = Color.parseColor("#FFCDD2")
                textSize = 44f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            val centerY = height / 2f
            canvas.drawText("🚨 重度ペナルティ発令中 🚨", width / 2f, centerY - 100f, textPaint)
            canvas.drawText("今週2本以上吸ったため", width / 2f, centerY + 20f, subTextPaint)
            canvas.drawText("スマホ壁紙が警告壁紙に変更されました", width / 2f, centerY + 90f, subTextPaint)
            canvas.drawText("もっと禁煙してください！", width / 2f, centerY + 180f, textPaint)

            // 壁紙適用 (ホーム画面 & ロック画面)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
            Log.d("WallpaperHelper", "Penalty wallpaper applied successfully!")
        } catch (e: Exception) {
            Log.e("WallpaperHelper", "Failed to set penalty wallpaper", e)
        }
    }
}
