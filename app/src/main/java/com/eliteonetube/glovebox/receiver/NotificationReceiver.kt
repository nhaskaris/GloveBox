package com.eliteonetube.glovebox.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.eliteonetube.glovebox.MainActivity
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.util.NotificationHelper

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Glovebox Alert"
        val message = intent.getStringExtra("message") ?: "You have a vehicle reminder."
        val type = intent.getStringExtra("type") ?: NotificationHelper.TYPE_MAINTENANCE

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Choose icon based on type
        val iconRes = when (type) {
            NotificationHelper.TYPE_DOCUMENT -> android.R.drawable.ic_menu_save
            NotificationHelper.TYPE_PREDICTIVE -> android.R.drawable.ic_menu_recent_history
            else -> android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
