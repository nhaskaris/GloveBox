package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.FuelLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FuelLogFormViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val fuelLogDao = db.fuelLogDao()
    private val vehicleDao = db.vehicleDao()
    private val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(application)

    private val _uiState = MutableStateFlow(FuelLogFormState())
    val uiState: StateFlow<FuelLogFormState> = _uiState.asStateFlow()

    fun loadData(vehicleId: Long, logId: Long) {
        viewModelScope.launch {
            val vehicle = vehicleDao.getVehicleById(vehicleId)
            val unit = vehicle?.odometerUnit ?: "km"
            val preferredCurrency = userPrefs.preferredCurrency.first()
            
            if (logId != 0L) {
                fuelLogDao.getFuelLogById(logId)?.let { log ->
                    _uiState.value = FuelLogFormState(
                        logId = log.id,
                        date = log.date,
                        odometer = log.odometer.toString(),
                        amount = log.amount.toString(),
                        totalCost = log.totalCost.toString(),
                        currency = log.currency,
                        location = log.location ?: "",
                        unit = unit
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    unit = unit,
                    currency = preferredCurrency
                )
            }
        }
    }

    fun onDateChange(date: Long) { _uiState.value = _uiState.value.copy(date = date) }
    fun onOdometerChange(value: String) { _uiState.value = _uiState.value.copy(odometer = value) }
    fun onAmountChange(value: String) { _uiState.value = _uiState.value.copy(amount = value) }
    fun onTotalCostChange(value: String) { _uiState.value = _uiState.value.copy(totalCost = value) }
    fun onCurrencyChange(value: String) { _uiState.value = _uiState.value.copy(currency = value) }
    fun onLocationChange(value: String) { _uiState.value = _uiState.value.copy(location = value) }

    fun saveLog(vehicleId: Long, onResult: () -> Unit) {
        val state = _uiState.value
        val log = FuelLog(
            id = state.logId,
            vehicleId = vehicleId,
            date = state.date,
            odometer = state.odometer.toIntOrNull() ?: 0,
            amount = state.amount.toDoubleOrNull() ?: 0.0,
            totalCost = state.totalCost.toDoubleOrNull() ?: 0.0,
            currency = state.currency,
            location = state.location.takeIf { it.isNotBlank() }
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
    val currency: String = "USD",
    val location: String = "",
    val unit: String = "km"
)
