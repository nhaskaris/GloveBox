package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import com.eliteonetube.glovebox.data.entity.Vehicle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class EfficiencyPoint(val date: Long, val value: Double)
data class CategorySpending(val category: String, val amount: Double)
data class MonthlySpending(val month: String, val amount: Double)

data class InsightsState(
    val efficiencyHistory: List<EfficiencyPoint> = emptyList(),
    val spendingByCategory: List<CategorySpending> = emptyList(),
    val monthlySpending: List<MonthlySpending> = emptyList(),
    val totalCost: Double = 0.0,
    val averageEfficiency: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(application: Application, initialVehicleId: Long) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val fuelLogDao = db.fuelLogDao()
    private val serviceRecordDao = db.serviceRecordDao()
    private val vehicleDao = db.vehicleDao()

    private val _selectedVehicleId = MutableStateFlow(initialVehicleId)
    val selectedVehicleId: StateFlow<Long> = _selectedVehicleId.asStateFlow()

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicleId.flatMapLatest { id ->
        if (id == 0L) flowOf<Vehicle?>(null)
        else flow { emit(vehicleDao.getVehicleById(id)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val uiState: StateFlow<InsightsState> = _selectedVehicleId.flatMapLatest { id ->
        val fuelsFlow = if (id == 0L) fuelLogDao.getAllFuelLogs() else fuelLogDao.getFuelLogsForVehicle(id)
        val servicesFlow = if (id == 0L) serviceRecordDao.getAllServiceRecords() else serviceRecordDao.getServiceRecordsForVehicle(id)
        
        combine(fuelsFlow, servicesFlow) { fuels, services ->
            calculateInsights(fuels, services)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsState())

    fun selectVehicle(id: Long) {
        _selectedVehicleId.value = id
    }

    private fun calculateInsights(fuels: List<FuelLog>, services: List<ServiceRecord>): InsightsState {
        if (fuels.isEmpty() && services.isEmpty()) return InsightsState()

        // 1. Efficiency History (L/100km)
        val efficiencyPoints = mutableListOf<EfficiencyPoint>()
        
        // Group by vehicle to calculate efficiency between refills of the same car
        val fuelsByVehicle = fuels.groupBy { it.vehicleId }
        
        fuelsByVehicle.forEach { (_, vehicleFuels) ->
            val sortedFuels = vehicleFuels.filter { it.isFullTank }.sortedBy { it.date }
            for (i in 0 until sortedFuels.size - 1) {
                val current = sortedFuels[i]
                val next = sortedFuels[i + 1]
                val distance = next.odometer - current.odometer
                if (distance > 0) {
                    val l100km = (next.amount / distance) * 100
                    efficiencyPoints.add(EfficiencyPoint(next.date, l100km))
                }
            }
        }
        efficiencyPoints.sortBy { it.date }

        // 2. Spending by Category
        val fuelTotal = fuels.sumOf { it.totalCost }
        val serviceTotal = services.sumOf { it.cost ?: 0.0 }
        
        val categorySpending = listOf(
            CategorySpending("Fuel", fuelTotal),
            CategorySpending("Service", serviceTotal)
        ).filter { it.amount > 0 }

        // 3. Monthly Spending
        val allCosts = mutableListOf<Pair<Long, Double>>()
        fuels.forEach { allCosts.add(it.date to it.totalCost) }
        services.forEach { allCosts.add(it.date to (it.cost ?: 0.0)) }

        val monthlyMap = mutableMapOf<YearMonth, Double>()
        allCosts.forEach { (date, cost) ->
            val month = YearMonth.from(Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()))
            monthlyMap[month] = monthlyMap.getOrDefault(month, 0.0) + cost
        }
        
        val monthlySpending = monthlyMap.entries
            .sortedBy { it.key }
            .takeLast(6) // Last 6 months
            .map { MonthlySpending(it.key.month.name.take(3), it.value) }

        return InsightsState(
            efficiencyHistory = efficiencyPoints,
            spendingByCategory = categorySpending,
            monthlySpending = monthlySpending,
            totalCost = fuelTotal + serviceTotal,
            averageEfficiency = if (efficiencyPoints.isNotEmpty()) efficiencyPoints.map { it.value }.average() else 0.0
        )
    }
}
