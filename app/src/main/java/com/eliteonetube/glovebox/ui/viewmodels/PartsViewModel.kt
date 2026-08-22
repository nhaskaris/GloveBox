package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.VehiclePart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PartsViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val vehiclePartDao = GloveboxDatabase.getDatabase(application).vehiclePartDao()
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()

    val vehicle = flow {
        if (vehicleId != 0L) {
            emit(vehicleDao.getVehicleById(vehicleId))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val parts: StateFlow<List<VehiclePart>> = if (vehicleId != 0L) {
        vehiclePartDao.getPartsForVehicle(vehicleId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun addPart(name: String, partNumber: String, brand: String? = null, notes: String? = null, photoUri: String? = null) {
        if (vehicleId == 0L) return
        viewModelScope.launch {
            vehiclePartDao.insertPart(
                VehiclePart(
                    vehicleId = vehicleId,
                    name = name,
                    partNumber = partNumber,
                    brand = brand,
                    notes = notes,
                    photoUri = photoUri
                )
            )
        }
    }

    fun updatePart(part: VehiclePart) {
        viewModelScope.launch {
            vehiclePartDao.updatePart(part.copy(lastUpdated = System.currentTimeMillis()))
        }
    }

    fun deletePart(part: VehiclePart) {
        viewModelScope.launch {
            vehiclePartDao.deletePart(part)
        }
    }
}
