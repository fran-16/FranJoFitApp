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

import android.app.Notification


object SmpReminderManager {

    private const val CHANNEL_ID = "SMP_CHANNEL"
    private const val CHANNEL_NAME = "Recordatorios SMP"
    private const val ALARM_REQUEST_CODE = 5511
    private const val REMINDER_INTERVAL_MINUTES = 1L

    // =========================================
    //  CREAR CANAL (Android 8+)
    // =========================================
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

    // =========================================
    //  ENVIAR NOTIFICACIÓN SMP
    // =========================================
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

        // Usamos BigTextStyle en lugar de setContentTitle / setContentText directos
        val bigStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle("¿Cómo te sentiste después de tu última comida?")
            .bigText("Toca para responder una mini-encuesta SMP.")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)   // ← Ícono que sí existe en tu proyecto
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("¿Cómo te sentiste después de tu última comida?")
                    .bigText("Toca para responder una mini-encuesta SMP.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()


        // Si sigues usando SmpNotifier:
        SmpNotifier.show(context, notification)
        // Si NO tienes SmpNotifier, en vez de la línea de arriba usa:
        // val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // manager.notify(9921, notification)
    }

    // =========================================
    //   PROGRAMAR RECORDATORIO CADA X MINUTOS
    // =========================================
    fun scheduleRepeating(context: Context) {

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, SmpReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMs = REMINDER_INTERVAL_MINUTES * 60 * 1000L

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pendingIntent
        )
    }
}
