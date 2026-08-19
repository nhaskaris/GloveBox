package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.data.entity.VehicleDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentFormViewModel(application: Application) : AndroidViewModel(application) {
    private val documentDao = GloveboxDatabase.getDatabase(application).vehicleDocumentDao()
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()

    private val _uiState = MutableStateFlow(DocumentFormState())
    val uiState: StateFlow<DocumentFormState> = _uiState.asStateFlow()

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onCategoryChange(value: String) { _uiState.value = _uiState.value.copy(category = value) }
    fun onPhotoChange(uri: String?) { _uiState.value = _uiState.value.copy(photoUri = uri) }
    fun onExpiryDateChange(date: Long?) { _uiState.value = _uiState.value.copy(expiryDate = date) }
    fun onUniversalToggle(isUniversal: Boolean) { _uiState.value = _uiState.value.copy(isUniversal = isUniversal) }
    fun onVehicleSelect(vehicleId: Long?) { _uiState.value = _uiState.value.copy(linkedVehicleId = vehicleId) }

    fun initialize(vehicleId: Long) {
        if (vehicleId != 0L) {
            _uiState.value = _uiState.value.copy(
                isUniversal = false,
                linkedVehicleId = vehicleId
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isUniversal = true,
                linkedVehicleId = null
            )
        }
    }

    fun saveDocument(onResult: () -> Unit) {
        val state = _uiState.value
        if (state.photoUri == null) return

        val document = VehicleDocument(
            vehicleId = if (state.isUniversal) null else state.linkedVehicleId,
            name = state.name.ifBlank { state.category },
            category = state.category,
            photoUri = state.photoUri,
            expiryDate = state.expiryDate
        )
        viewModelScope.launch {
            val id = documentDao.insertDocument(document)
            
            // Schedule notification if expiry date is set
            state.expiryDate?.let { expiry ->
                val context = getApplication<Application>()
                val thirtyDaysBefore = expiry - (30L * 24 * 60 * 60 * 1000)
                val sevenDaysBefore = expiry - (7L * 24 * 60 * 60 * 1000)
                
                com.eliteonetube.glovebox.util.NotificationHelper.scheduleNotification(
                    context, id, "Document Expiry", "${document.name} expires in 30 days", thirtyDaysBefore
                )
                com.eliteonetube.glovebox.util.NotificationHelper.scheduleNotification(
                    context, id + 1000000, "Document Expiry", "${document.name} expires in 7 days", sevenDaysBefore
                )
            }
            
            onResult()
        }
    }
}

data class DocumentFormState(
    val name: String = "",
    val category: String = "Insurance",
    val photoUri: String? = null,
    val expiryDate: Long? = null,
    val isUniversal: Boolean = false,
    val linkedVehicleId: Long? = null
)
