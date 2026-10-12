package com.eliteonetube.glovebox.data.alerts

import android.content.Context
import com.eliteonetube.glovebox.R
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.VehicleDocument
import com.eliteonetube.glovebox.util.OdometerStats
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object AlertEngine {

    suspend fun computeAlerts(context: Context): List<VehicleAlert> {
        val alerts = mutableListOf<VehicleAlert>()
        try {
            val db = GloveboxDatabase.getDatabase(context)
            val vehicles = db.vehicleDao().getAllVehicles().first()
            val universalDocs = db.vehicleDocumentDao().getUniversalDocuments().first()

            for (vehicle in vehicles) {
                val vehicleName = vehicle.nickname ?: "${vehicle.make} ${vehicle.model}"
                
                // 1. Process Vehicle-Specific Documents
                val vehicleDocs = db.vehicleDocumentDao().getDocumentsForVehicle(vehicle.id).first()
                alerts += documentAlerts(context, vehicleDocs, vehicle.id, vehicleName)

                // 2. Process Maintenance Reminders (including predictive)
                val reminders = db.reminderDao().getRemindersForVehicle(vehicle.id).first()
                val fuels = db.fuelLogDao().getFuelLogsForVehicle(vehicle.id).first()
                val services = db.serviceRecordDao().getServiceRecordsForVehicle(vehicle.id).first()

                val avgDailyDist = OdometerStats.averageDailyDistance(
                    fuels.map { it.date to it.odometer } + services.map { it.date to it.mileage }
                ) ?: 0.0

                val isMiles = vehicle.odometerUnit == "mi"

                for (reminder in reminders.filter { !it.isCompleted }) {
                    val distanceRemaining: Int? = reminder.targetMileage?.let { it - vehicle.odometer }

                    // Without a due date, predict one from the mileage when we can.
                    val daysRemaining: Int? = reminder.targetDate
                        ?.let { TimeUnit.MILLISECONDS.toDays(it - System.currentTimeMillis()).toInt() }
                        ?: if (distanceRemaining != null && avgDailyDist > 0) {
                            (distanceRemaining / avgDailyDist).toInt()
                        } else null

                    val isOverdue = (distanceRemaining != null && distanceRemaining <= 0) ||
                        (daysRemaining != null && daysRemaining <= 0)
                    val severity = reminderSeverity(distanceRemaining, daysRemaining, isMiles) ?: continue

                    alerts.add(
                        VehicleAlert.ServiceDue(
                            vehicleId = vehicle.id,
                            vehicleName = vehicleName,
                            severity = severity,
                            message = context.getString(
                                if (isOverdue) R.string.alert_service_overdue else R.string.alert_service_due_soon,
                                reminder.description
                            ),
                            kmRemaining = distanceRemaining,
                            daysRemaining = daysRemaining
                        )
                    )
                }
            }

            // Universal documents (not tied to a vehicle) are attributed to the first vehicle
            // so the alert has somewhere to navigate to.
            vehicles.firstOrNull()?.let { first ->
                val name = first.nickname ?: "${first.make} ${first.model}"
                alerts += documentAlerts(context, universalDocs, first.id, name)
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

    private fun documentAlerts(
        context: Context,
        documents: List<VehicleDocument>,
        vehicleId: Long,
        vehicleName: String
    ): List<VehicleAlert> = documents.mapNotNull { doc ->
        val expiry = doc.expiryDate ?: return@mapNotNull null
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(expiry - System.currentTimeMillis()).toInt()
        val severity = documentSeverity(daysRemaining) ?: return@mapNotNull null
        VehicleAlert.DocumentExpiring(
            vehicleId = vehicleId,
            vehicleName = vehicleName,
            severity = severity,
            message = if (daysRemaining < 0) context.getString(R.string.alert_doc_expired, doc.name)
                      else context.getString(R.string.alert_doc_expires_in, doc.name, daysRemaining),
            documentType = doc.category,
            daysRemaining = daysRemaining
        )
    }

    /** Severity for a document that expires in [daysRemaining] days (negative = already expired). */
    internal fun documentSeverity(daysRemaining: Int): AlertSeverity? = when {
        daysRemaining <= 7 -> AlertSeverity.CRITICAL
        daysRemaining <= 30 -> AlertSeverity.WARNING
        else -> null
    }

    /**
     * Severity for a service reminder. Distance thresholds are 100/500 km, or 60/300 mi.
     * Either input may be null when the reminder has no mileage or no (predictable) date.
     */
    internal fun reminderSeverity(distanceRemaining: Int?, daysRemaining: Int?, isMiles: Boolean): AlertSeverity? {
        val criticalDistance = if (isMiles) 60 else 100
        val warningDistance = if (isMiles) 300 else 500
        return when {
            (distanceRemaining != null && distanceRemaining <= criticalDistance) ||
                (daysRemaining != null && daysRemaining <= 3) -> AlertSeverity.CRITICAL
            (distanceRemaining != null && distanceRemaining <= warningDistance) ||
                (daysRemaining != null && daysRemaining <= 14) -> AlertSeverity.WARNING
            else -> null
        }
    }
}
