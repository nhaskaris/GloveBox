package com.eliteonetube.glovebox.util

import android.content.Context
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

        val allOdoLogs = (fuels.map { it.date to it.odometer } + 
                         services.map { it.date to it.mileage })
            .sortedBy { it.first }
        
        if (allOdoLogs.size < 2) return

        val first = allOdoLogs.first()
        val last = allOdoLogs.last()
        val totalDist = last.second - first.second
        val totalDays = TimeUnit.MILLISECONDS.toDays(last.first - first.first)
        
        if (totalDist <= 0 || totalDays <= 0) return
        val avgDailyDist = totalDist.toDouble() / totalDays

        // Standard maintenance intervals
        val unit = vehicle.odometerUnit
        val standardServices = listOf(
            "Oil Change" to if (unit == "mi") 5000 else 8000,
            "Tire Rotation" to if (unit == "mi") 6000 else 10000,
            "Brake Service" to if (unit == "mi") 40000 else 65000
        )

        standardServices.forEach { (name, interval) ->
            val lastService = services.filter { it.serviceType.contains(name, ignoreCase = true) }
                .maxByOrNull { it.date }
            
            val lastMileage = lastService?.mileage ?: 0
            val nextTarget = if (lastService != null) lastMileage + interval 
                             else ((vehicle.odometer / interval) + 1) * interval

            if (nextTarget > vehicle.odometer) {
                val distRemaining = nextTarget - vehicle.odometer
                val daysToTarget = (distRemaining / avgDailyDist).toLong()
                val predictedDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysToTarget)
                
                // Use a unique ID range for predictive alerts (e.g., 5,000,000 + name hash)
                val alertId = 5000000L + name.hashCode().toLong()
                
                NotificationHelper.scheduleNotification(
                    context,
                    alertId,
                    "Predicted Maintenance",
                    "Your $name is estimated to be due around ${last.first + TimeUnit.DAYS.toMillis(daysToTarget)}",
                    predictedDate,
                    NotificationHelper.TYPE_PREDICTIVE
                )
            }
        }
        
        // Also schedule for manual mileage-based reminders
        reminders.filter { !it.isCompleted && it.targetMileage != null }.forEach { reminder ->
            val target = reminder.targetMileage!!
            if (target > vehicle.odometer) {
                val distRemaining = target - vehicle.odometer
                val daysToTarget = (distRemaining / avgDailyDist).toLong()
                val predictedDate = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(daysToTarget)
                
                NotificationHelper.scheduleNotification(
                    context,
                    6000000L + reminder.id,
                    "Upcoming Maintenance",
                    "${reminder.description} is predicted to be due soon",
                    predictedDate,
                    NotificationHelper.TYPE_PREDICTIVE
                )
            }
        }
    }
}
