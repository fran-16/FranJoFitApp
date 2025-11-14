package com.example.franjofit.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SmpReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SmpReminderManager.sendNotification(context)
    }
}
