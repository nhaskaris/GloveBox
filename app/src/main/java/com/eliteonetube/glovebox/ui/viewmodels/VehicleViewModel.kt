package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    init {
        loadVehicle()
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            if (vehicleId != 0L) {
                _vehicle.value = vehicleDao.getVehicleById(vehicleId)
            } else {
                _vehicle.value = Vehicle(make = "", model = "", year = 2024, odometer = 0)
            }
        }
    }

    fun saveVehicle(make: String, model: String, year: Int, odometer: Int) {
        viewModelScope.launch {
            val currentVehicle = _vehicle.value
            if (currentVehicle != null) {
                val updatedVehicle = currentVehicle.copy(
                    make = make,
                    model = model,
                    year = year,
                    odometer = odometer
                )
                if (updatedVehicle.id == 0L) {
                    val id = vehicleDao.insertVehicle(updatedVehicle)
                    _vehicle.value = updatedVehicle.copy(id = id)
                } else {
                    vehicleDao.updateVehicle(updatedVehicle)
                    _vehicle.value = updatedVehicle
                }
            }
        }
    }
}
