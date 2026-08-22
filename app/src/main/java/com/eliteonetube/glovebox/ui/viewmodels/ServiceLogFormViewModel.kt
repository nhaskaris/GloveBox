package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Reminder
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class ServiceLogFormViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val serviceRecordDao = db.serviceRecordDao()
    private val vehicleDao = db.vehicleDao()
    private val reminderDao = db.reminderDao()

    private val _uiState = MutableStateFlow<ServiceLogFormState>(ServiceLogFormState())
    val uiState: StateFlow<ServiceLogFormState> = _uiState.asStateFlow()

    fun loadData(vehicleId: Long, recordId: Long, prefilledType: String? = null) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId)
            val unit = vehicle?.odometerUnit ?: "km"

            if (recordId != 0L) {
                serviceRecordDao.getServiceRecordById(recordId)?.let { record ->
                    _uiState.value = ServiceLogFormState(
                        recordId = record.id,
                        date = record.date,
                        mileage = record.mileage.toString(),
                        serviceTypes = record.serviceType.split(", ").filter { it.isNotBlank() },
                        cost = record.cost?.toString() ?: "",
                        notes = record.notes,
                        receiptPhotoUri = record.receiptPhotoUri,
                        serviceLocation = record.serviceLocation ?: "",
                        laborHours = record.laborHours?.toString() ?: "",
                        partsUsed = record.partsUsed ?: "",
                        isDiy = record.isDiy,
                        mechanicName = record.mechanicName ?: "",
                        unit = unit
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    unit = unit,
                    serviceTypes = if (prefilledType != null) listOf(prefilledType) else emptyList()
                )
            }
        }
    }

    fun onDateChange(date: Long) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onMileageChange(mileage: String) {
        _uiState.value = _uiState.value.copy(mileage = mileage)
    }

    fun onServiceTypeToggle(type: String) {
        val currentTypes = _uiState.value.serviceTypes.toMutableList()
        if (currentTypes.contains(type)) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }
        
        // Auto-suggest logic for standard intervals
        var autoSchedule = _uiState.value.isSchedulingNext
        var intervalMileage = _uiState.value.nextIntervalMileage
        var intervalMonths = _uiState.value.nextIntervalMonths
        val unit = _uiState.value.unit

        if (currentTypes.isNotEmpty()) {
            val latest = currentTypes.last().lowercase()
            when {
                latest.contains("oil") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "5000" else "8000"
                    intervalMonths = "6"
                }
                latest.contains("tire") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "6000" else "10000"
                    intervalMonths = "6"
                }
                latest.contains("brake") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "20000" else "30000"
                    intervalMonths = "12"
                }
                latest.contains("filter") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "10000" else "15000"
                    intervalMonths = "12"
                }
                latest.contains("spark") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "60000" else "100000"
                    intervalMonths = "48"
                }
                latest.contains("transmission") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "50000" else "80000"
                    intervalMonths = "36"
                }
                latest.contains("belt") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "80000" else "120000"
                    intervalMonths = "60"
                }
                latest.contains("coolant") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "30000" else "50000"
                    intervalMonths = "24"
                }
                latest.contains("alignment") -> {
                    autoSchedule = true
                    intervalMileage = if (unit == "mi") "10000" else "15000"
                    intervalMonths = "12"
                }
                latest.contains("wiper") -> {
                    autoSchedule = true
                    intervalMonths = "6"
                }
                latest.contains("battery") -> {
                    autoSchedule = true
                    intervalMonths = "24"
                }
                latest.contains("inspection") -> {
                    autoSchedule = true
                    intervalMonths = "12"
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            serviceTypes = currentTypes,
            isSchedulingNext = autoSchedule,
            nextIntervalMileage = intervalMileage,
            nextIntervalMonths = intervalMonths
        )
    }

    fun onCostChange(cost: String) {
        _uiState.value = _uiState.value.copy(cost = cost)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun onPhotoChange(uri: String?) {
        _uiState.value = _uiState.value.copy(receiptPhotoUri = uri)
    }

    fun onLocationChange(location: String) {
        _uiState.value = _uiState.value.copy(serviceLocation = location)
    }

    fun onLaborHoursChange(hours: String) {
        _uiState.value = _uiState.value.copy(laborHours = hours)
    }

    fun onPartsUsedChange(parts: String) {
        _uiState.value = _uiState.value.copy(partsUsed = parts)
    }

    fun onDiyToggle(isDiy: Boolean) {
        _uiState.value = _uiState.value.copy(isDiy = isDiy)
    }

    fun onMechanicNameChange(name: String) {
        _uiState.value = _uiState.value.copy(mechanicName = name)
    }

    fun onAutoScheduleToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isSchedulingNext = enabled)
    }

    fun onNextIntervalMileageChange(value: String) {
        _uiState.value = _uiState.value.copy(nextIntervalMileage = value)
    }

    fun onNextIntervalMonthsChange(value: String) {
        _uiState.value = _uiState.value.copy(nextIntervalMonths = value)
    }

    fun saveRecord(vehicleId: Long, onResult: () -> Unit) {
        val state = _uiState.value
        val record = ServiceRecord(
            id = state.recordId,
            vehicleId = vehicleId,
            date = state.date,
            mileage = state.mileage.toIntOrNull() ?: 0,
            serviceType = state.serviceTypes.joinToString(", "),
            cost = state.cost.toDoubleOrNull(),
            notes = state.notes,
            receiptPhotoUri = state.receiptPhotoUri,
            serviceLocation = if (state.isDiy) "DIY" else state.serviceLocation.takeIf { it.isNotBlank() },
            laborHours = state.laborHours.toDoubleOrNull(),
            partsUsed = state.partsUsed.takeIf { it.isNotBlank() },
            isDiy = state.isDiy,
            mechanicName = state.mechanicName.takeIf { it.isNotBlank() },
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (record.id == 0L) {
                serviceRecordDao.insertServiceRecord(record)
                
                // Smart Auto-Reschedule logic
                if (state.isSchedulingNext && state.serviceTypes.isNotEmpty()) {
                    val serviceName = state.serviceTypes.first()
                    val currentMileage = record.mileage
                    val intervalMileage = state.nextIntervalMileage.toIntOrNull()
                    val intervalMonths = state.nextIntervalMonths.toIntOrNull()
                    
                    // Look for an existing recurring reminder for this service
                    val existingReminders = reminderDao.getRemindersForVehicle(vehicleId).first()
                    val existingRecurring = existingReminders.find { 
                        it.description.contains(serviceName, ignoreCase = true) && it.isRecurring 
                    }

                    val targetMileage = intervalMileage?.let { currentMileage + it }
                    val targetDate = intervalMonths?.let {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = record.date
                        cal.add(Calendar.MONTH, it)
                        cal.timeInMillis
                    }
                    
                    if (existingRecurring != null) {
                        // Update existing recurring reminder
                        reminderDao.updateReminder(existingRecurring.copy(
                            targetMileage = targetMileage ?: existingRecurring.targetMileage,
                            targetDate = targetDate ?: existingRecurring.targetDate,
                            intervalMileage = intervalMileage ?: existingRecurring.intervalMileage,
                            intervalMonths = intervalMonths ?: existingRecurring.intervalMonths,
                            lastCompletedMileage = currentMileage,
                            lastCompletedDate = record.date,
                            isCompleted = false
                        ))
                    } else if (targetMileage != null || targetDate != null) {
                        // Create new recurring reminder
                        val reminder = Reminder(
                            vehicleId = vehicleId,
                            description = serviceName,
                            targetMileage = targetMileage,
                            targetDate = targetDate,
                            isRecurring = true,
                            intervalMileage = intervalMileage,
                            intervalMonths = intervalMonths,
                            lastCompletedMileage = currentMileage,
                            lastCompletedDate = record.date
                        )
                        reminderDao.insertReminder(reminder)
                    }
                }
            } else {
                serviceRecordDao.updateServiceRecord(record)
            }
            onResult()
        }
    }
}

data class ServiceLogFormState(
    val recordId: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val mileage: String = "",
    val serviceTypes: List<String> = emptyList(),
    val cost: String = "",
    val notes: String = "",
    val receiptPhotoUri: String? = null,
    val serviceLocation: String = "",
    val laborHours: String = "",
    val partsUsed: String = "",
    val isDiy: Boolean = false,
    val mechanicName: String = "",
    val unit: String = "km",
    val isSchedulingNext: Boolean = false,
    val nextIntervalMileage: String = "10000",
    val nextIntervalMonths: String = "6"
)
