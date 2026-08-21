package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.api.VinDecoderApi
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModel(application: Application, private val vehicleId: Long) : AndroidViewModel(application) {
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()
    private val vehicleCatalogDao = GloveboxDatabase.getDatabase(application).vehicleCatalogDao()
    private val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(application)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val vinApi = Retrofit.Builder()
        .baseUrl("https://vpic.nhtsa.dot.gov/api/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(VinDecoderApi::class.java)

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    val unitSystem: StateFlow<String> = userPrefs.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km")

    // Stream list of unique makes
    val makes: StateFlow<List<String>> = vehicleCatalogDao.getMakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks currently selected make to load corresponding models
    private val _selectedMake = MutableStateFlow("")
    private val _makeQuery = MutableStateFlow("")
    private val _modelQuery = MutableStateFlow("")

    // Dynamically query models whenever _selectedMake changes
    val availableModels: StateFlow<List<String>> = _selectedMake
        .flatMapLatest { make ->
            if (make.isBlank()) flowOf(emptyList())
            else vehicleCatalogDao.getModelsForMake(make)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists moved to ViewModel to avoid UI lag and limited to 50 items
    val filteredMakes: StateFlow<List<String>> = _makeQuery
        .combine(makes) { query, list ->
            if (query.isBlank()) list.take(50)
            else list.filter { it.contains(query, ignoreCase = true) }.take(50)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredModels: StateFlow<List<String>> = _modelQuery
        .combine(availableModels) { query, list ->
            if (query.isBlank()) list.take(50)
            else list.filter { it.contains(query, ignoreCase = true) }.take(50)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _vinDecodingState = MutableStateFlow<VinDecodingState>(VinDecodingState.Idle)
    val vinDecodingState: StateFlow<VinDecodingState> = _vinDecodingState.asStateFlow()

    private val _vinValidationErrorResId = MutableStateFlow<Int?>(null)
    val vinValidationErrorResId: StateFlow<Int?> = _vinValidationErrorResId.asStateFlow()

    init {
        loadVehicle()
    }

    fun updateMakeQuery(query: String) {
        _makeQuery.value = query
    }

    fun updateModelQuery(query: String) {
        _modelQuery.value = query
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            if (vehicleId != 0L) {
                val loaded = vehicleDao.getVehicleById(vehicleId)
                _vehicle.value = loaded
                loaded?.let { 
                    onMakeSelected(it.make)
                    _makeQuery.value = it.make
                    _modelQuery.value = it.model
                }
            } else {
                _vehicle.value = Vehicle(make = "", model = "", year = 2024, odometer = 0)
            }
        }
    }

    fun onMakeSelected(make: String) {
        _selectedMake.value = make
    }

    fun validateVin(v: String): Int? {
        val cleanVin = v.uppercase().filter { it.isLetterOrDigit() }
        val errorResId = when {
            cleanVin.isBlank() -> null
            cleanVin.length != 17 -> com.eliteonetube.glovebox.R.string.vin_error_length
            cleanVin.contains(Regex("[IOQ]")) -> com.eliteonetube.glovebox.R.string.vin_error_chars
            else -> null
        }
        _vinValidationErrorResId.value = errorResId
        return errorResId
    }

    fun decodeVin(vin: String) {
        if (vin.length < 11) return
        
        viewModelScope.launch {
            _vinDecodingState.value = VinDecodingState.Loading
            try {
                val response = vinApi.decodeVin(vin)
                val results = response.Results
                
                val make = results.find { it.Variable == "Make" }?.Value ?: ""
                val model = results.find { it.Variable == "Model" }?.Value ?: ""
                val year = results.find { it.Variable == "Model Year" }?.Value?.toIntOrNull() ?: 2024
                val fuel = results.find { it.Variable == "Fuel Type - Primary" }?.Value ?: ""
                val trim = results.find { it.Variable == "Trim" }?.Value ?: ""

                if (make.isNotBlank()) {
                    _vinDecodingState.value = VinDecodingState.Success(
                        make = make,
                        model = model,
                        year = year,
                        fuelType = fuel,
                        trim = trim
                    )
                } else {
                    _vinDecodingState.value = VinDecodingState.Error(com.eliteonetube.glovebox.R.string.vehicle_not_found)
                }
            } catch (e: Exception) {
                Log.e("VehicleViewModel", "VIN Decoding Error", e)
                _vinDecodingState.value = VinDecodingState.Error(com.eliteonetube.glovebox.R.string.connection_error)
            }
        }
    }

    fun resetVinState() {
        _vinDecodingState.value = VinDecodingState.Idle
    }

    fun saveVehicle(
        make: String,
        model: String,
        year: Int,
        odometer: Int,
        trim: String? = null,
        vin: String? = null,
        nickname: String? = null,
        licensePlate: String? = null,
        color: String? = null,
        fuelType: String? = null,
        odometerUnit: String = "km",
        photoUri: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val currentVehicle = _vehicle.value
            if (currentVehicle != null) {
                val updatedVehicle = currentVehicle.copy(
                    make = make.trim(),
                    model = model.trim(),
                    year = year,
                    odometer = odometer,
                    trim = trim?.trim(),
                    vin = vin?.trim(),
                    nickname = nickname?.trim(),
                    licensePlate = licensePlate?.trim(),
                    color = color?.trim(),
                    fuelType = fuelType?.trim(),
                    odometerUnit = odometerUnit,
                    photoUri = photoUri,
                    lastUpdated = System.currentTimeMillis()
                )
                if (updatedVehicle.id == 0L) {
                    val id = vehicleDao.insertVehicle(updatedVehicle)
                    _vehicle.value = updatedVehicle.copy(id = id)
                } else {
                    vehicleDao.updateVehicle(updatedVehicle)
                    _vehicle.value = updatedVehicle
                }
                onSuccess()
            }
        }
    }
}

sealed class VinDecodingState {
    data object Idle : VinDecodingState()
    data object Loading : VinDecodingState()
    data class Success(val make: String, val model: String, val year: Int, val fuelType: String, val trim: String) : VinDecodingState()
    data class Error(val messageResId: Int) : VinDecodingState()
}
