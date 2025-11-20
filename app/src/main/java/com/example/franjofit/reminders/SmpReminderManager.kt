package com.example.franjofit.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.franjofit.MainActivity
import com.example.franjofit.R

object SmpReminderManager {

    private const val CHANNEL_ID = "SMP_CHANNEL"
    private const val CHANNEL_NAME = "Recordatorios SMP"
    private const val ALARM_REQUEST_CODE = 5511

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios para encuesta SMP post comida"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendNotification(context: Context) {
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_smp", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("¿Cómo te sentiste después de tu última comida?")
                    .bigText("Toca para responder una mini-encuesta SMP.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        SmpNotifier.show(context, notification)
    }


    fun schedulePostprandialReminder(
        context: Context,
        minutes: Long
    ) {
        val appContext = context.applicationContext
        val alarmManager =
            appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(appContext, SmpReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + minutes * 60_000L


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }
}
