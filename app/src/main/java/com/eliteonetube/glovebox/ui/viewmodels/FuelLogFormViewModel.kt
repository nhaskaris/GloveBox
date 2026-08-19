package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.FuelLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FuelLogFormViewModel(application: Application) : AndroidViewModel(application) {
    private val fuelLogDao = GloveboxDatabase.getDatabase(application).fuelLogDao()

    private val _uiState = MutableStateFlow(FuelLogFormState())
    val uiState: StateFlow<FuelLogFormState> = _uiState.asStateFlow()

    fun loadLog(logId: Long) {
        if (logId == 0L) return
        viewModelScope.launch {
            fuelLogDao.getFuelLogById(logId)?.let { log ->
                _uiState.value = FuelLogFormState(
                    logId = log.id,
                    date = log.date,
                    odometer = log.odometer.toString(),
                    amount = log.amount.toString(),
                    totalCost = log.totalCost.toString(),
                    location = log.location ?: "",
                    isFullTank = log.isFullTank
                )
            }
        }
    }

    fun onDateChange(date: Long) { _uiState.value = _uiState.value.copy(date = date) }
    fun onOdometerChange(value: String) { _uiState.value = _uiState.value.copy(odometer = value) }
    fun onAmountChange(value: String) { _uiState.value = _uiState.value.copy(amount = value) }
    fun onTotalCostChange(value: String) { _uiState.value = _uiState.value.copy(totalCost = value) }
    fun onLocationChange(value: String) { _uiState.value = _uiState.value.copy(location = value) }
    fun onFullTankToggle(value: Boolean) { _uiState.value = _uiState.value.copy(isFullTank = value) }

    fun saveLog(vehicleId: Long, onResult: () -> Unit) {
        val state = _uiState.value
        val log = FuelLog(
            id = state.logId,
            vehicleId = vehicleId,
            date = state.date,
            odometer = state.odometer.toIntOrNull() ?: 0,
            amount = state.amount.toDoubleOrNull() ?: 0.0,
            totalCost = state.totalCost.toDoubleOrNull() ?: 0.0,
            location = state.location.takeIf { it.isNotBlank() },
            isFullTank = state.isFullTank
        )
        viewModelScope.launch {
            if (log.id == 0L) fuelLogDao.insertFuelLog(log)
            else fuelLogDao.updateFuelLog(log)
            onResult()
        }
    }
}

data class FuelLogFormState(
    val logId: Long = 0,
    val date: Long = System.currentTimeMillis(),
    val odometer: String = "",
    val amount: String = "",
    val totalCost: String = "",
    val location: String = "",
    val isFullTank: Boolean = true
)
