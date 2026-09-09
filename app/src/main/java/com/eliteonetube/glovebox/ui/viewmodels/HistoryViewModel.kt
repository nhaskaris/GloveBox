package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.util.PdfExportUtility
import com.eliteonetube.glovebox.util.CsvUtility
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val db = GloveboxDatabase.getDatabase(application)
    private val serviceRecordDao = db.serviceRecordDao()
    private val fuelLogDao = db.fuelLogDao()
    private val vehicleDao = db.vehicleDao()
    private val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(application)

    val preferredCurrency: StateFlow<String> = userPrefs.preferredCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    private val serviceRecords = serviceRecordDao.getServiceRecordsForVehicle(vehicleId)
    private val fuelLogs = fuelLogDao.getFuelLogsForVehicle(vehicleId)

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    val historyItems: StateFlow<List<HistoryItem>> = combine(serviceRecords, fuelLogs, _filter) { services, fuels, currentFilter ->
        val items = mutableListOf<HistoryItem>()
        if (currentFilter == HistoryFilter.ALL || currentFilter == HistoryFilter.SERVICE) {
            items.addAll(services.map { HistoryItem.Service(it) })
        }
        if (currentFilter == HistoryFilter.ALL || currentFilter == HistoryFilter.FUEL) {
            items.addAll(fuels.map { HistoryItem.Fuel(it) })
        }
        items.sortByDescending { it.date }
        items
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _vehicle = kotlinx.coroutines.flow.MutableStateFlow<com.eliteonetube.glovebox.data.entity.Vehicle?>(null)
    val vehicle: StateFlow<com.eliteonetube.glovebox.data.entity.Vehicle?> = _vehicle

    private val _csvOperationStatus = MutableStateFlow<String?>(null)
    val csvOperationStatus = _csvOperationStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _vehicle.value = vehicleDao.getVehicleById(vehicleId)
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
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

    fun clearCsvStatus() {
        _csvOperationStatus.value = null
    }

    fun exportToCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val services = serviceRecordDao.getServiceRecordsForVehicle(vehicleId).first()
                val csv = CsvUtility.exportServiceRecords(services)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { 
                    it.write(csv.toByteArray())
                }
                _csvOperationStatus.value = getApplication<Application>().getString(com.eliteonetube.glovebox.R.string.export_success)
            } catch (e: Exception) {
                _csvOperationStatus.value = getApplication<Application>().getString(com.eliteonetube.glovebox.R.string.export_failed, e.message)
            }
        }
    }

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val csv = getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (csv != null) {
                    val imported = CsvUtility.parseServiceRecords(csv, vehicleId)
                    if (imported.isEmpty()) {
                        _csvOperationStatus.value = getApplication<Application>().getString(com.eliteonetube.glovebox.R.string.import_no_records)
                        return@launch
                    }
                    imported.forEach { serviceRecordDao.insertServiceRecord(it) }
                    _csvOperationStatus.value = getApplication<Application>().getString(com.eliteonetube.glovebox.R.string.import_success, imported.size)
                } else {
                    _csvOperationStatus.value = getApplication<Application>().getString(com.eliteonetube.glovebox.R.string.import_read_error)
                }
            } catch (e: Exception) {
                _csvOperationStatus.value = getApplication<Application>().getString(com.eliteonetube.glovebox.R.string.import_failed, e.message)
            }
        }
    }

    fun exportHistoryToPdf(
        includeCosts: Boolean,
        includeShop: Boolean,
        includeMechanic: Boolean,
        includeFuel: Boolean,
        includeSummary: Boolean,
        onResult: (File?) -> Unit
    ) {
        val currentVehicle = vehicle.value
        // For export, we might want to fetch all regardless of current filter
        // or just export what's visible. Usually, the dialog options decide.
        if (currentVehicle != null) {
            viewModelScope.launch {
                val services = serviceRecordDao.getServiceRecordsForVehicle(vehicleId).first()
                val fuels = fuelLogDao.getFuelLogsForVehicle(vehicleId).first()
                val prefCurrency = preferredCurrency.value
                
                val file = withContext(Dispatchers.IO) {
                    try {
                        PdfExportUtility.generateFullVehicleHistoryPdf(
                            getApplication(),
                            currentVehicle,
                            services,
                            fuels,
                            includeCosts = includeCosts,
                            includeShop = includeShop,
                            includeMechanic = includeMechanic,
                            includeFuel = includeFuel,
                            includeSummary = includeSummary,
                            preferredCurrency = prefCurrency
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

enum class HistoryFilter {
    ALL, SERVICE, FUEL
}
