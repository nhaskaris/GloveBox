package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServiceHistoryViewModel(application: Application, vehicleId: Long) : AndroidViewModel(application) {
    private val serviceRecordDao = GloveboxDatabase.getDatabase(application).serviceRecordDao()

    val serviceRecords: StateFlow<List<ServiceRecord>> = serviceRecordDao.getServiceRecordsForVehicle(vehicleId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            serviceRecordDao.deleteServiceRecord(record)
        }
    }
}
