package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ServiceLogFormViewModel(application: Application) : AndroidViewModel(application) {
    private val serviceRecordDao = GloveboxDatabase.getDatabase(application).serviceRecordDao()

    private val _uiState = MutableStateFlow<ServiceLogFormState>(ServiceLogFormState())
    val uiState: StateFlow<ServiceLogFormState> = _uiState.asStateFlow()

    fun loadRecord(recordId: Long) {
        if (recordId == 0L) return
        viewModelScope.launch {
            val record = serviceRecordDao.getServiceRecordById(recordId)
            record?.let {
                _uiState.value = ServiceLogFormState(
                    recordId = it.id,
                    date = it.date,
                    mileage = it.mileage.toString(),
                    serviceTypes = it.serviceType.split(", ").filter { type -> type.isNotBlank() },
                    cost = it.cost?.toString() ?: "",
                    notes = it.notes,
                    receiptPhotoUri = it.receiptPhotoUri,
                    serviceLocation = it.serviceLocation ?: "",
                    laborHours = it.laborHours?.toString() ?: "",
                    partsUsed = it.partsUsed ?: "",
                    isDiy = it.isDiy,
                    mechanicName = it.mechanicName ?: ""
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
        _uiState.value = _uiState.value.copy(serviceTypes = currentTypes)
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
    val mechanicName: String = ""
)
