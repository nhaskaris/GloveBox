package com.eliteonetube.glovebox.util

import android.content.Context
import com.eliteonetube.glovebox.data.GloveboxDatabase
import kotlinx.coroutines.flow.first

/** Checks a new odometer reading against everything already logged for the vehicle. */
object OdometerGuard {
    suspend fun check(
        context: Context,
        vehicleId: Long,
        reading: Int,
        date: Long,
        excludeFuelLogId: Long = 0L,
        excludeServiceRecordId: Long = 0L
    ): OdometerCheck {
        // A blank/zero reading means "not entered", so there is nothing to validate.
        if (reading <= 0) return OdometerCheck.Ok
        val db = GloveboxDatabase.getDatabase(context)
        val fuels = db.fuelLogDao().getFuelLogsForVehicle(vehicleId).first()
            .filter { it.id != excludeFuelLogId }
            .map { it.date to it.odometer }
        val services = db.serviceRecordDao().getServiceRecordsForVehicle(vehicleId).first()
            .filter { it.id != excludeServiceRecordId }
            .map { it.date to it.mileage }
        return OdometerValidator.check(reading, date, fuels + services)
    }
}
