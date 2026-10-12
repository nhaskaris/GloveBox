package com.eliteonetube.glovebox.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.GloveboxDatabase
import kotlinx.coroutines.flow.first
import com.eliteonetube.glovebox.receiver.NotificationReceiver

object NotificationHelper {
    const val CHANNEL_ID = "glovebox_notifications"
    const val CHANNEL_NAME = "Glovebox Alerts"

    // Types to help the receiver choose icons/actions
    const val TYPE_MAINTENANCE = "maintenance"
    const val TYPE_DOCUMENT = "document"
    const val TYPE_PREDICTIVE = "predictive"

    // Used to open the relevant screen when a notification is tapped.
    const val ACTION_OPEN_FROM_NOTIFICATION = "com.eliteonetube.glovebox.ACTION_OPEN_FROM_NOTIFICATION"
    const val EXTRA_TYPE = "type"
    const val EXTRA_VEHICLE_ID = "vehicleId"

    fun createNotificationChannel(context: Context) {
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

    fun scheduleNotification(
        context: Context,
        id: Long,
        title: String,
        message: String,
        timeMillis: Long,
        type: String = TYPE_MAINTENANCE,
        vehicleId: Long? = null
    ) {
        // Don't schedule if time is in the past
        if (timeMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id.toInt())
            putExtra("title", title)
            putExtra("message", message)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_VEHICLE_ID, vehicleId ?: -1L)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Maintenance and document reminders don't need to-the-minute precision, so use an
        // inexact alarm: no SCHEDULE_EXACT_ALARM permission, and still delivered during Doze.
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
    }

    // Alarm request-code namespaces. Reminder alarms keep using the raw reminder id.
    private const val DOC_30D_OFFSET = 1_000_000L
    private const val DOC_7D_OFFSET = 2_000_000L
    private const val DOC_DAY_OFFSET = 3_000_000L
    private const val PREDICTIVE_SERVICE_BASE = 5_000_000L
    private const val PREDICTIVE_REMINDER_BASE = 6_000_000L
    private const val PREDICTIVE_SERVICE_SLOTS = 10L

    /** Stable id for the predictive alert of service [serviceIndex] of [vehicleId]. */
    fun predictiveServiceId(vehicleId: Long, serviceIndex: Int): Long =
        PREDICTIVE_SERVICE_BASE + (vehicleId % 90_000L) * PREDICTIVE_SERVICE_SLOTS + serviceIndex

    fun predictiveReminderId(reminderId: Long): Long = PREDICTIVE_REMINDER_BASE + reminderId

    /**
     * Schedules a sequence of alerts for a document (e.g., 30 days, 7 days, and day of).
     */
    fun scheduleDocumentExpiries(context: Context, docId: Long, docName: String, expiryDate: Long, vehicleId: Long? = null) {
        val thirtyDays = 30L * 24 * 60 * 60 * 1000
        val sevenDays = 7L * 24 * 60 * 60 * 1000

        scheduleNotification(
            context, docId + DOC_30D_OFFSET,
            context.getString(R.string.notif_doc_expiry_title),
            context.getString(R.string.notif_doc_30d, docName),
            expiryDate - thirtyDays,
            TYPE_DOCUMENT,
            vehicleId
        )
        scheduleNotification(
            context, docId + DOC_7D_OFFSET,
            context.getString(R.string.notif_doc_expiry_title),
            context.getString(R.string.notif_doc_7d, docName),
            expiryDate - sevenDays,
            TYPE_DOCUMENT,
            vehicleId
        )
        scheduleNotification(
            context, docId + DOC_DAY_OFFSET,
            context.getString(R.string.notif_doc_expired_title),
            context.getString(R.string.notif_doc_today, docName),
            expiryDate,
            TYPE_DOCUMENT,
            vehicleId
        )
    }

    /**
     * Re-creates every pending alarm from the database. AlarmManager forgets all alarms on
     * reboot, app update and (for RTC alarms) clock/timezone changes, so this runs from
     * [com.eliteonetube.glovebox.receiver.BootReceiver].
     */
    suspend fun rescheduleAll(context: Context) {
        val db = GloveboxDatabase.getDatabase(context)

        db.reminderDao().getAllReminders().first()
            .filter { !it.isCompleted && it.targetDate != null }
            .forEach { reminder ->
                scheduleNotification(
                    context,
                    reminder.id,
                    context.getString(R.string.notif_maintenance_due_title),
                    reminder.description,
                    reminder.targetDate!!,
                    TYPE_MAINTENANCE,
                    reminder.vehicleId
                )
            }

        val vehicles = db.vehicleDao().getAllVehicles().first()
        val documents = db.vehicleDocumentDao().getUniversalDocuments().first() +
            vehicles.flatMap { db.vehicleDocumentDao().getDocumentsForVehicle(it.id).first() }
        documents.forEach { doc ->
            doc.expiryDate?.let { scheduleDocumentExpiries(context, doc.id, doc.name, it, doc.vehicleId) }
        }

        vehicles.forEach { MaintenancePredictor.schedulePredictiveAlerts(context, it.id) }
    }

    /** Cancels every alarm this app has scheduled for rows currently in the database. */
    suspend fun cancelAll(context: Context) {
        val db = GloveboxDatabase.getDatabase(context)
        db.reminderDao().getAllReminders().first().forEach {
            cancelNotification(context, it.id)
            cancelNotification(context, predictiveReminderId(it.id))
        }
        val vehicles = db.vehicleDao().getAllVehicles().first()
        val documents = db.vehicleDocumentDao().getUniversalDocuments().first() +
            vehicles.flatMap { db.vehicleDocumentDao().getDocumentsForVehicle(it.id).first() }
        documents.forEach {
            cancelNotification(context, it.id + DOC_30D_OFFSET)
            cancelNotification(context, it.id + DOC_7D_OFFSET)
            cancelNotification(context, it.id + DOC_DAY_OFFSET)
        }
        vehicles.forEach { v ->
            for (i in 0 until PREDICTIVE_SERVICE_SLOTS.toInt()) {
                cancelNotification(context, predictiveServiceId(v.id, i))
            }
        }
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
