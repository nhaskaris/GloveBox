package com.eliteonetube.glovebox.data.alerts

sealed class VehicleAlert(
    open val vehicleId: Long,
    open val vehicleName: String,
    open val severity: AlertSeverity,
    open val message: String
) {
    data class DocumentExpiring(
        override val vehicleId: Long,
        override val vehicleName: String,
        override val severity: AlertSeverity,
        override val message: String,
        val documentType: String, // e.g. "Insurance", "KTEO"
        val daysRemaining: Int
    ) : VehicleAlert(vehicleId, vehicleName, severity, message)

    data class ServiceDue(
        override val vehicleId: Long,
        override val vehicleName: String,
        override val severity: AlertSeverity,
        override val message: String,
        val kmRemaining: Int?,
        val daysRemaining: Int?
    ) : VehicleAlert(vehicleId, vehicleName, severity, message)
}

enum class AlertSeverity { CRITICAL, WARNING, INFO }
