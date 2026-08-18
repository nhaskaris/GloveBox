package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()
    private val vehicleCatalogDao = GloveboxDatabase.getDatabase(application).vehicleCatalogDao()

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    // Stream list of unique makes
    val makes: StateFlow<List<String>> = vehicleCatalogDao.getMakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks currently selected make to load corresponding models
    private val _selectedMake = MutableStateFlow("")

    // Dynamically query models whenever _selectedMake changes
    val availableModels: StateFlow<List<String>> = _selectedMake
        .flatMapLatest { make ->
            if (make.isBlank()) flowOf(emptyList())
            else vehicleCatalogDao.getModelsForMake(make)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadVehicle()
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            if (vehicleId != 0L) {
                val loaded = vehicleDao.getVehicleById(vehicleId)
                _vehicle.value = loaded
                loaded?.let { onMakeSelected(it.make) }
            } else {
                _vehicle.value = Vehicle(make = "", model = "", year = 2024, odometer = 0)
            }
        }
    }

    fun onMakeSelected(make: String) {
        _selectedMake.value = make
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