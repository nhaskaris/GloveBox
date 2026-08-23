package com.eliteonetube.glovebox.ui.viewmodels.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.UserPreferencesRepository
import com.eliteonetube.glovebox.data.alerts.AlertEngine
import com.eliteonetube.glovebox.data.alerts.VehicleAlert
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.data.entity.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class DashboardActivity {
    abstract val date: Long
    abstract val vehicleId: Long
    abstract val vehicleName: String
    
    data class Fuel(val log: FuelLog, override val vehicleName: String) : DashboardActivity() {
        override val date: Long = log.date
        override val vehicleId: Long = log.vehicleId
    }
    
    data class Service(val record: ServiceRecord, override val vehicleName: String) : DashboardActivity() {
        override val date: Long = record.date
        override val vehicleId: Long = record.vehicleId
    }
}

data class DashboardState(
    val alerts: List<VehicleAlert> = emptyList(),
    val recentActivity: List<DashboardActivity> = emptyList(),
    val activeVehicle: Vehicle? = null,
    val hasVehicles: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val userPrefs = UserPreferencesRepository(application)
    private val vehicleDao = db.vehicleDao()
    private val fuelLogDao = db.fuelLogDao()
    private val serviceRecordDao = db.serviceRecordDao()

    private val _refreshTrigger = MutableStateFlow(0)

    val dashboardState: StateFlow<DashboardState> = combine(
        userPrefs.activeVehicleId,
        vehicleDao.getAllVehicles(),
        _refreshTrigger
    ) { activeId, vehicles, _ ->
        Triple(activeId, vehicles, vehicles.isNotEmpty())
    }.flatMapLatest { (activeId, vehicles, hasVehicles) ->
        if (!hasVehicles) {
            return@flatMapLatest flowOf(DashboardState(hasVehicles = false, isLoading = false))
        }

        val activeVehicle = vehicles.find { it.id == activeId } ?: vehicles.firstOrNull()
        
        // Fetch alerts periodically or on trigger
        val alerts = AlertEngine.computeAlerts(getApplication())
        
        // Fetch recent activity across all vehicles
        val fuelFlow = fuelLogDao.getAllFuelLogs()
        val serviceFlow = serviceRecordDao.getAllServiceRecords()
        
        combine(fuelFlow, serviceFlow) { fuels, services ->
            val activities = mutableListOf<DashboardActivity>()
            
            activities.addAll(fuels.map { log ->
                val vName = vehicles.find { it.id == log.vehicleId }?.let { it.nickname ?: "${it.make} ${it.model}" } ?: "Unknown"
                DashboardActivity.Fuel(log, vName)
            })
            
            activities.addAll(services.map { record ->
                val vName = vehicles.find { it.id == record.vehicleId }?.let { it.nickname ?: "${it.make} ${it.model}" } ?: "Unknown"
                DashboardActivity.Service(record, vName)
            })
            
            DashboardState(
                alerts = alerts,
                recentActivity = activities.sortedByDescending { it.date }.take(5),
                activeVehicle = activeVehicle,
                hasVehicles = true,
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun refresh() {
        _refreshTrigger.value += 1
    }
}
