package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Reminder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class RemindersViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val reminderDao = db.reminderDao()
    private val vehicleDao = db.vehicleDao()
    private val fuelLogDao = db.fuelLogDao()
    private val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(application)

    private val _currentOdometer = MutableStateFlow(0)
    val currentOdometer: StateFlow<Int> = _currentOdometer.asStateFlow()

    private val _estimatedOdometer = MutableStateFlow(0)
    val estimatedOdometer: StateFlow<Int> = _estimatedOdometer.asStateFlow()

    val odometerUnit: StateFlow<String> = userPrefs.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km")

    init {
        loadVehicleData()
        calculateEstimatedOdometer()
    }

    private fun loadVehicleData() {
        if (vehicleId == 0L) return
        viewModelScope.launch {
            vehicleDao.getVehicleById(vehicleId)?.let { vehicle ->
                _currentOdometer.value = vehicle.odometer
            }
        }
    }

    private fun calculateEstimatedOdometer() {
        if (vehicleId == 0L) return
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId) ?: return@launch
            val logs = fuelLogDao.getFuelLogsForVehicle(vehicleId).first()
            
            if (logs.size >= 2) {
                val latestLog = logs.first()
                val oldestLog = logs.last()
                
                val diffMiles = latestLog.odometer - oldestLog.odometer
                val diffTimeDays = (latestLog.date - oldestLog.date) / (1000 * 60 * 60 * 24)
                
                if (diffTimeDays > 0) {
                    val milesPerDay = diffMiles.toDouble() / diffTimeDays
                    val daysSinceLastLog = (System.currentTimeMillis() - latestLog.date) / (1000 * 60 * 60 * 24)
                    val estimatedIncrease = (daysSinceLastLog * milesPerDay).toInt()
                    _estimatedOdometer.value = latestLog.odometer + estimatedIncrease
                } else {
                    _estimatedOdometer.value = vehicle.odometer
                }
            } else {
                _estimatedOdometer.value = vehicle.odometer
            }
        }
    }

    val reminders: StateFlow<List<Reminder>> = (if (vehicleId == 0L) reminderDao.getAllReminders() else reminderDao.getRemindersForVehicle(vehicleId))
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(
        description: String, 
        targetMileage: Int?, 
        targetDate: Long?,
        isRecurring: Boolean = false,
        intervalMileage: Int? = null,
        intervalMonths: Int? = null
    ) {
        viewModelScope.launch {
            val reminder = Reminder(
                vehicleId = vehicleId,
                description = description,
                targetMileage = targetMileage,
                targetDate = targetDate,
                isRecurring = isRecurring,
                intervalMileage = intervalMileage,
                intervalMonths = intervalMonths
            )
            val id = reminderDao.insertReminder(reminder)
            com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
            
            // Schedule notification if date is set
            targetDate?.let { date ->
                com.eliteonetube.glovebox.util.NotificationHelper.scheduleNotification(
                    getApplication(),
                    id,
                    "Maintenance Due",
                    description,
                    date
                )
            }
        }
    }

    fun toggleReminderCompletion(reminder: Reminder) {
        viewModelScope.launch {
            if (reminder.isRecurring && !reminder.isCompleted) {
                // If marking a recurring item as complete, calculate the next one
                val nextMileage = if (reminder.intervalMileage != null) {
                    val baseMileage = _estimatedOdometer.value.coerceAtLeast(reminder.targetMileage ?: 0)
                    baseMileage + reminder.intervalMileage
                } else null

                val nextDate = if (reminder.intervalMonths != null) {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, reminder.intervalMonths)
                    cal.timeInMillis
                } else null

                val updatedReminder = reminder.copy(
                    targetMileage = nextMileage,
                    targetDate = nextDate,
                    lastCompletedMileage = _estimatedOdometer.value,
                    lastCompletedDate = System.currentTimeMillis()
                    // isCompleted stays false because it's renewed
                )
                reminderDao.updateReminder(updatedReminder)
                com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
                
                // Reschedule notification for new date
                nextDate?.let { date ->
                    com.eliteonetube.glovebox.util.NotificationHelper.scheduleNotification(
                        getApplication(),
                        reminder.id,
                        "Maintenance Due",
                        reminder.description,
                        date
                    )
                }
            } else {
                reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
                com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
                // Cancel notification if completed
                if (!reminder.isCompleted) {
                    com.eliteonetube.glovebox.util.NotificationHelper.cancelNotification(
                        getApplication(),
                        reminder.id
                    )
                }
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
            com.eliteonetube.glovebox.util.NotificationHelper.cancelNotification(
                getApplication(),
                reminder.id
            )
        }
    }
}
