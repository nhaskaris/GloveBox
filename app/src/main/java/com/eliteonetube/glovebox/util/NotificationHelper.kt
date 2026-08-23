package com.eliteonetube.glovebox.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.eliteonetube.glovebox.MainActivity
import com.eliteonetube.glovebox.receiver.NotificationReceiver

object NotificationHelper {
    const val CHANNEL_ID = "glovebox_notifications"
    const val CHANNEL_NAME = "Glovebox Alerts"

    // Types to help the receiver choose icons/actions
    const val TYPE_MAINTENANCE = "maintenance"
    const val TYPE_DOCUMENT = "document"
    const val TYPE_PREDICTIVE = "predictive"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for vehicle document expiry and maintenance"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(
        context: Context,
        id: Long,
        title: String,
        message: String,
        timeMillis: Long,
        type: String = TYPE_MAINTENANCE
    ) {
        // Don't schedule if time is in the past
        if (timeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id.toInt())
            putExtra("title", title)
            putExtra("message", message)
            putExtra("type", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback for missing exact alarm permission
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    /**
     * Schedules a sequence of alerts for a document (e.g., 30 days, 7 days, and day of).
     */
    fun scheduleDocumentExpiries(context: Context, docId: Long, docName: String, expiryDate: Long) {
        val thirtyDays = 30L * 24 * 60 * 60 * 1000
        val sevenDays = 7L * 24 * 60 * 60 * 1000

        // Alert IDs are offset to avoid collisions
        // docId, docId + 1M, docId + 2M
        
        // 30 days before
        scheduleNotification(
            context, docId + 1000000,
            "Document Expiry",
            "$docName expires in 30 days",
            expiryDate - thirtyDays,
            TYPE_DOCUMENT
        )

        // 7 days before
        scheduleNotification(
            context, docId + 2000000,
            "Document Expiry",
            "$docName expires in 1 week",
            expiryDate - sevenDays,
            TYPE_DOCUMENT
        )

        // On the day
        scheduleNotification(
            context, docId + 3000000,
            "Document Expired",
            "$docName expires today!",
            expiryDate,
            TYPE_DOCUMENT
        )
    }

    fun cancelNotification(context: Context, id: Long) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}
