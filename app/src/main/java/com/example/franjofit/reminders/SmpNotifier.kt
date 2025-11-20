package com.example.franjofit.reminders

import android.app.Notification
import android.app.NotificationManager
import android.content.Context

object SmpNotifier {
    private const val NOTIFICATION_ID = 9921

    fun show(context: Context, notification: Notification?) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        if (manager != null && notification != null) {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }
}
