package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.util.PdfExportUtility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class HistoryItem {
    abstract val date: Long
    abstract val sortId: Long // Combined key for list items

    data class Service(val record: ServiceRecord) : HistoryItem() {
        override val date: Long = record.date
        override val sortId: Long = record.id
    }

    data class Fuel(val log: FuelLog) : HistoryItem() {
        override val date: Long = log.date
        override val sortId: Long = log.id
    }
}

class HistoryViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val serviceRecordDao = GloveboxDatabase.getDatabase(application).serviceRecordDao()
    private val fuelLogDao = GloveboxDatabase.getDatabase(application).fuelLogDao()
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()

    private val serviceRecords = serviceRecordDao.getServiceRecordsForVehicle(vehicleId)
    private val fuelLogs = fuelLogDao.getFuelLogsForVehicle(vehicleId)

    val historyItems: StateFlow<List<HistoryItem>> = combine(serviceRecords, fuelLogs) { services, fuels ->
        val items = mutableListOf<HistoryItem>()
        items.addAll(services.map { HistoryItem.Service(it) })
        items.addAll(fuels.map { HistoryItem.Fuel(it) })
        items.sortByDescending { it.date }
        items
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _vehicle = kotlinx.coroutines.flow.MutableStateFlow<com.eliteonetube.glovebox.data.entity.Vehicle?>(null)
    val vehicle: StateFlow<com.eliteonetube.glovebox.data.entity.Vehicle?> = _vehicle

    init {
        viewModelScope.launch {
            _vehicle.value = vehicleDao.getVehicleById(vehicleId)
        }
    }

    fun deleteServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            serviceRecordDao.deleteServiceRecord(record)
        }
    }

    fun deleteFuelLog(log: FuelLog) {
        viewModelScope.launch {
            fuelLogDao.deleteFuelLog(log)
        }
    }

    fun exportHistoryToPdf(onResult: (File?) -> Unit) {
        val currentVehicle = vehicle.value
        val items = historyItems.value
        if (currentVehicle != null && items.isNotEmpty()) {
            viewModelScope.launch {
                val file = withContext(Dispatchers.IO) {
                    try {
                        // PDF currently only supports service records, we can keep it like that or update utility
                        val services = items.filterIsInstance<HistoryItem.Service>().map { it.record }
                        PdfExportUtility.generateVehicleServiceHistoryPdf(
                            getApplication(),
                            currentVehicle,
                            services
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                onResult(file)
            }
        } else {
            onResult(null)
        }
    }
}
