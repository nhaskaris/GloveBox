package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.UserPreferencesRepository
import com.eliteonetube.glovebox.data.entity.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehicleListViewModel(application: Application) : AndroidViewModel(application) {
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()
    private val userPreferencesRepository = UserPreferencesRepository(application)

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeVehicleId: StateFlow<Long?> = userPreferencesRepository.activeVehicleId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setActiveVehicle(vehicleId: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setActiveVehicleId(vehicleId)
            com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            if (activeVehicleId.value == vehicle.id) {
                // Clear active vehicle if it's the one being deleted
                userPreferencesRepository.setActiveVehicleId(0L)
            }
            vehicleDao.deleteVehicle(vehicle)
            com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
        }
    }
}
