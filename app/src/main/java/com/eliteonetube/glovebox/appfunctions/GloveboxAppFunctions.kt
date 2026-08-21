package com.eliteonetube.glovebox.appfunctions

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.UserPreferencesRepository
import com.eliteonetube.glovebox.data.entity.FuelLog
import kotlinx.coroutines.flow.first

/**
 * Exposes Glovebox functionality to the system and AI agents.
 */
class GloveboxAppFunctions {

    /**
     * Logs a fuel entry for a specific vehicle.
     *
     * @param context The context in which the AppFunction is executed.
     * @param vehicleId The ID of the vehicle to log fuel for.
     * @param amount The amount of fuel added.
     * @param totalCost The total cost of the fuel.
     * @param odometer The odometer reading at the time of fueling.
     * @return The ID of the newly created fuel log.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun logFuel(
        context: AppFunctionContext,
        vehicleId: Long,
        amount: Double,
        totalCost: Double,
        odometer: Int
    ): Long {
        val database = GloveboxDatabase.getDatabase(context.context)
        val fuelLog = FuelLog(
            vehicleId = vehicleId,
            amount = amount,
            totalCost = totalCost,
            odometer = odometer,
            date = System.currentTimeMillis()
        )
        return database.fuelLogDao().insertFuelLog(fuelLog)
    }

    /**
     * Checks for pending reminders for the currently active vehicle.
     *
     * @param context The context in which the AppFunction is executed.
     * @return A list of reminder information.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkReminders(
        context: AppFunctionContext
    ): List<ReminderInfo> {
        val database = GloveboxDatabase.getDatabase(context.context)
        val userPrefs = UserPreferencesRepository(context.context)
        val activeVehicleId = userPrefs.activeVehicleId.first() ?: return emptyList()
        
        val reminders = database.reminderDao().getRemindersForVehicle(activeVehicleId).first()
        return reminders.map { 
            ReminderInfo(
                id = it.id,
                title = it.description,
                dueDate = it.targetDate,
                dueMileage = it.targetMileage
            )
        }
    }
}

/**
 * Data class representing a simplified reminder for AppFunctions.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ReminderInfo(
    /** The reminder's unique identifier. */
    val id: Long,
    /** The title or description of the reminder. */
    val title: String,
    /** The due date in milliseconds, if applicable. */
    val dueDate: Long?,
    /** The due mileage, if applicable. */
    val dueMileage: Int?
)
