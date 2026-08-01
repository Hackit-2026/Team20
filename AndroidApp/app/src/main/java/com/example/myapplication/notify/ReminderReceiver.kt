package com.example.myapplication.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminder.createChannel(context)
        Reminder.showNow(context)
        if (intent.getBooleanExtra("daily", false)) {
            // ユーザーが保存した時刻で翌日分を再予約する
            Reminder.scheduleNext(context)
        }
    }
}

