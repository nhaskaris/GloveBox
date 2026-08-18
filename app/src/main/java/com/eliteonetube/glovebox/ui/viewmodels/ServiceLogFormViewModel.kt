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
                    serviceType = it.serviceType,
                    cost = it.cost.toString(),
                    notes = it.notes
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

    fun onServiceTypeChange(serviceType: String) {
        _uiState.value = _uiState.value.copy(serviceType = serviceType)
    }

    fun onCostChange(cost: String) {
        _uiState.value = _uiState.value.copy(cost = cost)
    }

    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }

    fun saveRecord(vehicleId: Long, onResult: () -> Unit) {
        val state = _uiState.value
        val record = ServiceRecord(
            id = state.recordId,
            vehicleId = vehicleId,
            date = state.date,
            mileage = state.mileage.toIntOrNull() ?: 0,
            serviceType = state.serviceType,
            cost = state.cost.toDoubleOrNull() ?: 0.0,
            notes = state.notes
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
    val serviceType: String = "",
    val cost: String = "",
    val notes: String = ""
)
