package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Reminder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModel(application: Application) : AndroidViewModel(application) {
    private val reminderDao = GloveboxDatabase.getDatabase(application).reminderDao()
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()

    private val vehicleId = MutableStateFlow(0L)
    private val _currentOdometer = MutableStateFlow(0)
    val currentOdometer: StateFlow<Int> = _currentOdometer.asStateFlow()

    init {
        viewModelScope.launch {
            // For now, we assume there's only one vehicle profile
            vehicleDao.getAllVehicles().collect { vehicles ->
                if (vehicles.isNotEmpty()) {
                    val vehicle = vehicles.first()
                    vehicleId.value = vehicle.id
                    _currentOdometer.value = vehicle.odometer
                }
            }
        }
    }

    val reminders: StateFlow<List<Reminder>> = vehicleId.flatMapLatest { id ->
        reminderDao.getRemindersForVehicle(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addReminder(description: String, targetMileage: Int?, targetDate: Long?) {
        viewModelScope.launch {
            val reminder = Reminder(
                vehicleId = vehicleId.value,
                description = description,
                targetMileage = targetMileage,
                targetDate = targetDate
            )
            reminderDao.insertReminder(reminder)
        }
    }

    fun toggleReminderCompletion(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderDao.deleteReminder(reminder)
        }
    }
}
