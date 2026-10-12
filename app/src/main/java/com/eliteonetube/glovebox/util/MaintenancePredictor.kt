package com.eliteonetube.glovebox.util

import android.content.Context
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.Reminder
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.data.entity.Vehicle
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object MaintenancePredictor {

    suspend fun schedulePredictiveAlerts(context: Context, vehicleId: Long) {
        val db = GloveboxDatabase.getDatabase(context)
        val vehicle = db.vehicleDao().getVehicleById(vehicleId) ?: return
        val fuels = db.fuelLogDao().getFuelLogsForVehicle(vehicleId).first()
        val services = db.serviceRecordDao().getServiceRecordsForVehicle(vehicleId).first()
        val reminders = db.reminderDao().getRemindersForVehicle(vehicleId).first()

        val avgDailyDist = OdometerStats.averageDailyDistance(
            fuels.map { it.date to it.odometer } + services.map { it.date to it.mileage }
        ) ?: return

        // Standard maintenance intervals
        val unit = vehicle.odometerUnit
        val standardServices = listOf(
            "Oil Change" to if (unit == "mi") 5000 else 8000,
            "Tire Rotation" to if (unit == "mi") 6000 else 10000,
            "Brake Service" to if (unit == "mi") 40000 else 65000
        )

        val dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)

        standardServices.forEachIndexed { index, (name, interval) ->
            val lastService = services.filter { it.serviceType.contains(name, ignoreCase = true) }
                .maxByOrNull { it.date }
            
            val lastMileage = lastService?.mileage ?: 0
            val nextTarget = if (lastService != null) lastMileage + interval 
                             else ((vehicle.odometer / interval) + 1) * interval

            if (nextTarget > vehicle.odometer) {
                val distRemaining = nextTarget - vehicle.odometer
                val daysToTarget = (distRemaining / avgDailyDist).toLong()
                val predictedDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysToTarget)
                
                NotificationHelper.scheduleNotification(
                    context,
                    NotificationHelper.predictiveServiceId(vehicleId, index),
                    context.getString(R.string.notif_predicted_title),
                    context.getString(R.string.notif_predicted_message, name, dateFormat.format(java.util.Date(predictedDate))),
                    predictedDate,
                    NotificationHelper.TYPE_PREDICTIVE,
                    vehicleId
                )
            }
        }
        
        // Also schedule for manual mileage-based reminders
        reminders.filter { !it.isCompleted && it.targetMileage != null }.forEach { reminder ->
            val target = reminder.targetMileage ?: return@forEach
            if (target > vehicle.odometer) {
                val distRemaining = target - vehicle.odometer
                val daysToTarget = (distRemaining / avgDailyDist).toLong()
                val predictedDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysToTarget)
                
                NotificationHelper.scheduleNotification(
                    context,
                    NotificationHelper.predictiveReminderId(reminder.id),
                    context.getString(R.string.notif_upcoming_title),
                    context.getString(R.string.notif_upcoming_message, reminder.description),
                    predictedDate,
                    NotificationHelper.TYPE_PREDICTIVE,
                    vehicleId
                )
            }
        }
    }
}
