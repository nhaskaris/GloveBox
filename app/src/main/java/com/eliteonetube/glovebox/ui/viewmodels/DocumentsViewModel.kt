package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.VehicleDocument
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DocumentsViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val documentDao = GloveboxDatabase.getDatabase(application).vehicleDocumentDao()

    val documents: StateFlow<List<VehicleDocument>> = documentDao.getDocumentsForVehicle(vehicleId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDocument(document: VehicleDocument) {
        viewModelScope.launch {
            documentDao.deleteDocument(document)
        }
    }
}
