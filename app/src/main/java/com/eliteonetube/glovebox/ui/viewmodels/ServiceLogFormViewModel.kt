package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Reminder
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.util.NotificationHelper
import com.eliteonetube.glovebox.util.OdometerCheck
import com.eliteonetube.glovebox.util.ReminderMatcher
import com.eliteonetube.glovebox.util.OdometerGuard
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
    private val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(application)

    /** Open reminders of the vehicle being edited, used to offer closing the ones a service fulfils. */
    private var openReminders: List<Reminder> = emptyList()

    private fun ServiceLogFormState.withMatchingReminders() = copy(
        matchingReminders = openReminders
            .filter { r -> serviceTypes.any { ReminderMatcher.matches(r.description, it) } }
            .map { it.description }
    )

    private val _uiState = MutableStateFlow<ServiceLogFormState>(ServiceLogFormState())
    val uiState: StateFlow<ServiceLogFormState> = _uiState.asStateFlow()

    fun loadData(vehicleId: Long, recordId: Long, prefilledType: String? = null, scanUri: String? = null) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId)
            val unit = vehicle?.odometerUnit ?: "km"
            val preferredCurrency = userPrefs.preferredCurrency.first()

            if (recordId != 0L) {
                // ... (existing loading logic)
                serviceRecordDao.getServiceRecordById(recordId)?.let { record ->
                    _uiState.value = ServiceLogFormState(
                        recordId = record.id,
                        date = record.date,
                        mileage = record.mileage.toString(),
                        serviceTypes = record.serviceType.split(", ").filter { it.isNotBlank() },
                        cost = record.cost?.toString() ?: "",
                        currency = record.currency,
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
                openReminders = reminderDao.getRemindersForVehicle(vehicleId).first().filter { !it.isCompleted }
                _uiState.value = _uiState.value.copy(
                    unit = unit,
                    currency = preferredCurrency,
                    serviceTypes = if (prefilledType != null) listOf(prefilledType) else emptyList()
                ).withMatchingReminders()
                
                // Trigger scanning if scanUri is provided
                scanUri?.let { uriStr ->
                    scanInvoice(android.net.Uri.parse(uriStr), vehicleId)
                }
            }
        }
    }

    private fun scanInvoice(uri: android.net.Uri, vehicleId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, receiptPhotoUri = uri.toString())
            val results = com.eliteonetube.glovebox.util.InvoiceScanner.scanUri(getApplication(), uri)
            
            if (results.isNotEmpty()) {
                val first = results.first()
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    pendingScans = results,
                    currentScanIndex = 0,
                    totalScans = results.size,
                    date = first.date ?: _uiState.value.date,
                    cost = first.totalCost?.toString() ?: "",
                    mileage = first.mileage?.toString() ?: "",
                    serviceTypes = if (first.detectedServiceType != null) listOf(first.detectedServiceType) else emptyList()
                ).withMatchingReminders()
            } else {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    notes = getApplication<android.app.Application>().getString(com.eliteonetube.glovebox.R.string.scan_no_records)
                )
            }
        }
    }

    fun skipCurrentScan(onComplete: () -> Unit) {
        loadNextScan(onComplete)
    }

    private fun loadNextScan(onComplete: () -> Unit) {
        val nextIndex = _uiState.value.currentScanIndex + 1
        if (nextIndex < _uiState.value.totalScans) {
            val nextScan = _uiState.value.pendingScans[nextIndex]
            _uiState.value = _uiState.value.copy(
                currentScanIndex = nextIndex,
                date = nextScan.date ?: System.currentTimeMillis(),
                cost = nextScan.totalCost?.toString() ?: "",
                mileage = nextScan.mileage?.toString() ?: "",
                serviceTypes = if (nextScan.detectedServiceType != null) listOf(nextScan.detectedServiceType) else emptyList(),
                notes = "",
                laborHours = "",
                partsUsed = ""
            ).withMatchingReminders()
        } else {
            // No more scans
            _uiState.value = _uiState.value.copy(pendingScans = emptyList(), totalScans = 0)
            onComplete()
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
        ).withMatchingReminders()
    }

    fun onCloseMatchingRemindersToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(closeMatchingReminders = enabled)
    }

    fun onCostChange(cost: String) {
        _uiState.value = _uiState.value.copy(cost = cost)
    }

    fun onCurrencyChange(value: String) {
        _uiState.value = _uiState.value.copy(currency = value)
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

    /**
     * Closes open reminders that this service fulfils. One-off reminders are marked done; recurring
     * ones are renewed from their own interval. Recurring reminders for the first service type are
     * left to the "schedule next" logic in [saveRecord], which uses the intervals the user entered.
     */
    private suspend fun completeMatchingReminders(vehicleId: Long, state: ServiceLogFormState, record: ServiceRecord) {
        val app = getApplication<android.app.Application>()
        val matches = reminderDao.getRemindersForVehicle(vehicleId).first()
            .filter { !it.isCompleted && state.serviceTypes.any { type -> ReminderMatcher.matches(it.description, type) } }

        for (reminder in matches) {
            val handledBySchedulingNext = reminder.isRecurring && state.isSchedulingNext &&
                state.serviceTypes.firstOrNull()?.let { ReminderMatcher.matches(reminder.description, it) } == true
            if (handledBySchedulingNext) continue

            val nextMileage = reminder.intervalMileage?.takeIf { record.mileage > 0 }?.let { record.mileage + it }
            val nextDate = reminder.intervalMonths?.let { months ->
                Calendar.getInstance().apply { timeInMillis = record.date; add(Calendar.MONTH, months) }.timeInMillis
            }
            val renews = reminder.isRecurring && (nextMileage != null || nextDate != null)

            reminderDao.updateReminder(
                reminder.copy(
                    isCompleted = !renews,
                    targetMileage = if (renews) nextMileage else reminder.targetMileage,
                    targetDate = if (renews) nextDate else reminder.targetDate,
                    lastCompletedMileage = record.mileage.takeIf { it > 0 } ?: reminder.lastCompletedMileage,
                    lastCompletedDate = record.date
                )
            )

            NotificationHelper.cancelNotification(app, reminder.id)
            NotificationHelper.cancelNotification(app, NotificationHelper.predictiveReminderId(reminder.id))
            if (renews && nextDate != null) {
                NotificationHelper.scheduleNotification(
                    app,
                    reminder.id,
                    app.getString(com.eliteonetube.glovebox.R.string.notif_maintenance_due_title),
                    reminder.description,
                    nextDate,
                    vehicleId = reminder.vehicleId
                )
            }
        }
    }

    fun dismissOdometerWarning() {
        _uiState.value = _uiState.value.copy(odometerWarning = null)
    }

    /** Saves the record; unless [force] is set, an odometer reading that looks wrong is flagged first. */
    fun saveRecord(vehicleId: Long, onResult: () -> Unit, force: Boolean = false) {
        val state = _uiState.value
        val record = ServiceRecord(
            id = state.recordId,
            vehicleId = vehicleId,
            date = state.date,
            mileage = state.mileage.toIntOrNull() ?: 0,
            serviceType = state.serviceTypes.joinToString(", "),
            cost = state.cost.toDoubleOrNull(),
            currency = state.currency,
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
            if (!force) {
                val check = OdometerGuard.check(getApplication(), vehicleId, record.mileage, record.date, excludeServiceRecordId = record.id)
                if (check != OdometerCheck.Ok) {
                    _uiState.value = _uiState.value.copy(odometerWarning = check)
                    return@launch
                }
            }
            _uiState.value = _uiState.value.copy(odometerWarning = null)

            if (record.id == 0L) {
                serviceRecordDao.insertServiceRecord(record)

                if (state.closeMatchingReminders) completeMatchingReminders(vehicleId, state, record)

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

            // Re-calculate and schedule predictive alerts
            com.eliteonetube.glovebox.util.MaintenancePredictor.schedulePredictiveAlerts(
                getApplication(),
                vehicleId
            )

            com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())

            // If we have more items to review, load the next one
            if (state.currentScanIndex < state.totalScans - 1) {
                loadNextScan(onResult)
            } else {
                onResult()
            }
        }
    }
}

data class ServiceLogFormState(
    val recordId: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val mileage: String = "",
    val serviceTypes: List<String> = emptyList(),
    val cost: String = "",
    val currency: String = "USD",
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
    val nextIntervalMonths: String = "6",
    val isScanning: Boolean = false,
    val pendingScans: List<com.eliteonetube.glovebox.util.ScannedInvoice> = emptyList(),
    val currentScanIndex: Int = 0,
    val totalScans: Int = 0,
    val odometerWarning: OdometerCheck? = null,
    /** Descriptions of open reminders that the selected service types would fulfil. */
    val matchingReminders: List<String> = emptyList(),
    val closeMatchingReminders: Boolean = true
)
