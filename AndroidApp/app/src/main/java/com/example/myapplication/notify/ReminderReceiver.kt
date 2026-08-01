package com.example.myapplication.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.AppRepo

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = AppRepo(context)
        
        // 本日分が既に申告済みであれば通知を出さずにスキップ
        if (repo.isReportedToday()) {
            if (intent.getBooleanExtra("daily", false)) {
                Reminder.scheduleNext(context)
            }
            return
        }

        Reminder.createChannel(context)
        Reminder.showNow(context)
        if (intent.getBooleanExtra("daily", false)) {
            // 翌日分を再予約する
            Reminder.scheduleNext(context)
        }
    }
}
