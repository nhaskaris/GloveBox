package com.eliteonetube.glovebox.data.alerts

import android.content.Context
import com.eliteonetube.glovebox.data.GloveboxDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object AlertEngine {

    suspend fun computeAlerts(context: Context): List<VehicleAlert> {
        val alerts = mutableListOf<VehicleAlert>()
        try {
            val db = GloveboxDatabase.getDatabase(context)
            val vehicles = db.vehicleDao().getAllVehicles().first()
            val allDocuments = db.vehicleDocumentDao().getUniversalDocuments().first() // Start with universal

            for (vehicle in vehicles) {
                val vehicleName = vehicle.nickname ?: "${vehicle.make} ${vehicle.model}"
                
                // 1. Process Vehicle-Specific Documents
                val vehicleDocs = db.vehicleDocumentDao().getDocumentsForVehicle(vehicle.id).first()
                for (doc in vehicleDocs) {
                    doc.expiryDate?.let { expiry ->
                        val daysRemaining = TimeUnit.MILLISECONDS.toDays(expiry - System.currentTimeMillis()).toInt()
                        val severity = when {
                            daysRemaining <= 7 -> AlertSeverity.CRITICAL
                            daysRemaining <= 30 -> AlertSeverity.WARNING
                            else -> null
                        }
                        
                        if (severity != null) {
                            alerts.add(
                                VehicleAlert.DocumentExpiring(
                                    vehicleId = vehicle.id,
                                    vehicleName = vehicleName,
                                    severity = severity,
                                    message = "${doc.name} expires in $daysRemaining days",
                                    documentType = doc.category,
                                    daysRemaining = daysRemaining
                                )
                            )
                        }
                    }
                }

                // 2. Process Maintenance Reminders (including predictive)
                val reminders = db.reminderDao().getRemindersForVehicle(vehicle.id).first()
                val fuels = db.fuelLogDao().getFuelLogsForVehicle(vehicle.id).first()
                val services = db.serviceRecordDao().getServiceRecordsForVehicle(vehicle.id).first()

                // Calculate average daily distance for prediction
                val allOdoLogs = (fuels.map { it.date to it.odometer } + 
                                 services.map { it.date to it.mileage })
                    .sortedBy { it.first }
                
                val avgDailyDist = if (allOdoLogs.size >= 2) {
                    val first = allOdoLogs.first()
                    val last = allOdoLogs.last()
                    val dist = last.second - first.second
                    val days = TimeUnit.MILLISECONDS.toDays(last.first - first.first)
                    if (dist > 0 && days > 0) dist.toDouble() / days else 0.0
                } else 0.0

                for (reminder in reminders.filter { !it.isCompleted }) {
                    var kmRemaining: Int? = null
                    var daysRemaining: Int? = null
                    
                    reminder.targetMileage?.let { target ->
                        kmRemaining = target - vehicle.odometer
                    }
                    
                    reminder.targetDate?.let { targetDate ->
                        daysRemaining = TimeUnit.MILLISECONDS.toDays(targetDate - System.currentTimeMillis()).toInt()
                    }

                    // If we have mileage but no date, try to predict the date
                    if (daysRemaining == null && kmRemaining != null && avgDailyDist > 0) {
                        daysRemaining = (kmRemaining!! / avgDailyDist).toInt()
                    }

                    val severity = when {
                        (kmRemaining != null && kmRemaining!! <= 0) || (daysRemaining != null && daysRemaining!! <= 0) -> AlertSeverity.CRITICAL
                        (kmRemaining != null && kmRemaining!! <= 100) || (daysRemaining != null && daysRemaining!! <= 3) -> AlertSeverity.CRITICAL
                        (kmRemaining != null && kmRemaining!! <= 500) || (daysRemaining != null && daysRemaining!! <= 14) -> AlertSeverity.WARNING
                        else -> null
                    }

                    if (severity != null) {
                        alerts.add(
                            VehicleAlert.ServiceDue(
                                vehicleId = vehicle.id,
                                vehicleName = vehicleName,
                                severity = severity,
                                message = "${reminder.description} is ${if ((kmRemaining ?: 1) <= 0 || (daysRemaining ?: 1) <= 0) "overdue" else "due soon"}",
                                kmRemaining = kmRemaining,
                                daysRemaining = daysRemaining
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return alerts.sortedWith(
            compareByDescending<VehicleAlert> { it.severity }
                .thenBy { 
                    when (it) {
                        is VehicleAlert.DocumentExpiring -> it.daysRemaining
                        is VehicleAlert.ServiceDue -> it.daysRemaining ?: Int.MAX_VALUE
                    }
                }
        )
    }
}
