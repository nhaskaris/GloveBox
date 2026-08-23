package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.api.VinDecoderApi
import com.eliteonetube.glovebox.data.entity.ProspectVehicle
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

data class ProspectFormState(
    val id: Long = 0,
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val vin: String = "",
    val askedPrice: String = "",
    val currency: String = "USD",
    val sellerNotes: String = "",
    val location: String = "",
    val photoUri: String? = null,
    val isLoading: Boolean = false,
    val errorResId: Int? = null,
    val vinValidationErrorResId: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProspectViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val prospectDao = db.prospectVehicleDao()
    private val vehicleDao = db.vehicleDao()
    private val vehicleCatalogDao = db.vehicleCatalogDao()
    private val userPrefs = com.eliteonetube.glovebox.data.UserPreferencesRepository(application)
    
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val vinApi = Retrofit.Builder()
        .baseUrl("https://vpic.nhtsa.dot.gov/api/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(VinDecoderApi::class.java)

    val allProspects = prospectDao.getAllProspects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(ProspectFormState())
    val formState = _formState.asStateFlow()

    // --- Catalog Search Logic ---
    val makes: StateFlow<List<String>> = vehicleCatalogDao.getMakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _makeQuery = MutableStateFlow("")
    val filteredMakes: StateFlow<List<String>> = _makeQuery
        .combine(makes) { query, list ->
            if (query.isBlank()) list
            else list.filter { it.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMake = MutableStateFlow("")
    val availableModels: StateFlow<List<String>> = _selectedMake
        .flatMapLatest { make ->
            if (make.isBlank()) flowOf(emptyList())
            else vehicleCatalogDao.getModelsForMake(make)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateMakeQuery(query: String) {
        _makeQuery.value = query
    }

    fun onMakeSelected(make: String) {
        _selectedMake.value = make
        _makeQuery.value = make
        _formState.value = _formState.value.copy(make = make)
    }

    fun onMakeChange(v: String) { 
        _formState.value = _formState.value.copy(make = v)
        updateMakeQuery(v)
    }
    fun onModelChange(v: String) { _formState.value = _formState.value.copy(model = v) }
    fun onYearChange(v: String) { _formState.value = _formState.value.copy(year = v) }
    
    fun onVinChange(v: String) { 
        val cleanVin = v.uppercase().filter { it.isLetterOrDigit() }
        val errorResId = when {
            cleanVin.isBlank() -> null
            cleanVin.length != 17 -> com.eliteonetube.glovebox.R.string.vin_error_length
            cleanVin.contains(Regex("[IOQ]")) -> com.eliteonetube.glovebox.R.string.vin_error_chars
            else -> null
        }
        _formState.value = _formState.value.copy(vin = cleanVin, vinValidationErrorResId = errorResId)
    }

    fun onPriceChange(v: String) { _formState.value = _formState.value.copy(askedPrice = v) }
    fun onCurrencyChange(v: String) { _formState.value = _formState.value.copy(currency = v) }
    fun onNotesChange(v: String) { _formState.value = _formState.value.copy(sellerNotes = v) }
    fun onLocationChange(v: String) { _formState.value = _formState.value.copy(location = v) }
    fun onPhotoChange(v: String?) { _formState.value = _formState.value.copy(photoUri = v) }

    fun loadProspect(id: Long) {
        viewModelScope.launch {
            val preferredCurrency = userPrefs.preferredCurrency.first()
            if (id == 0L) {
                _formState.value = ProspectFormState(currency = preferredCurrency)
                return@launch
            }
            prospectDao.getProspectById(id)?.let { p ->
                _formState.value = ProspectFormState(
                    id = p.id,
                    make = p.make,
                    model = p.model,
                    year = p.year.toString(),
                    vin = p.vin ?: "",
                    askedPrice = p.askedPrice?.toString() ?: "",
                    currency = p.currency,
                    sellerNotes = p.sellerNotes,
                    location = p.location,
                    photoUri = p.photoUri
                )
                _selectedMake.value = p.make
                _makeQuery.value = p.make
            }
        }
    }

    fun decodeVin() {
        val vin = _formState.value.vin
        if (vin.length != 17) return
        
        _formState.value = _formState.value.copy(isLoading = true, errorResId = null)
        viewModelScope.launch {
            try {
                val response = vinApi.decodeVin(vin)
                val results = response.Results
                val make = results.find { it.Variable == "Make" }?.Value ?: ""
                val model = results.find { it.Variable == "Model" }?.Value ?: ""
                val year = results.find { it.Variable == "Model Year" }?.Value ?: ""
                
                _formState.value = _formState.value.copy(
                    make = make,
                    model = model,
                    year = year,
                    isLoading = false
                )
                _selectedMake.value = make
                _makeQuery.value = make
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(isLoading = false, errorResId = com.eliteonetube.glovebox.R.string.connection_error)
            }
        }
    }

    fun saveProspect(onResult: () -> Unit) {
        val s = _formState.value
        val prospect = ProspectVehicle(
            id = s.id,
            make = s.make,
            model = s.model,
            year = s.year.toIntOrNull() ?: 0,
            vin = s.vin.takeIf { it.isNotBlank() },
            askedPrice = s.askedPrice.toDoubleOrNull(),
            currency = s.currency,
            sellerNotes = s.sellerNotes,
            location = s.location,
            photoUri = s.photoUri
        )
        viewModelScope.launch {
            if (prospect.id == 0L) prospectDao.insertProspect(prospect)
            else prospectDao.updateProspect(prospect)
            onResult()
        }
    }

    fun deleteProspect(prospect: ProspectVehicle) {
        viewModelScope.launch {
            prospectDao.deleteProspect(prospect)
        }
    }

    fun updateChecklist(prospectId: Long, checkedItems: Set<String>) {
        viewModelScope.launch {
            prospectDao.getProspectById(prospectId)?.let { p ->
                val json = JSONObject()
                checkedItems.forEach { json.put(it, true) }
                prospectDao.updateProspect(p.copy(checklistJson = json.toString()))
            }
        }
    }

    fun promoteToGarage(prospect: ProspectVehicle, onResult: (Long) -> Unit) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                make = prospect.make,
                model = prospect.model,
                year = prospect.year,
                vin = prospect.vin,
                odometer = 0,
                odometerUnit = "mi", // Default to miles
                photoUri = prospect.photoUri
            )
            val newId = vehicleDao.insertVehicle(vehicle)
            
            // Create a specialized service record for the purchase
            val purchaseRecord = com.eliteonetube.glovebox.data.entity.ServiceRecord(
                vehicleId = newId,
                date = System.currentTimeMillis(),
                mileage = 0,
                serviceType = "Purchase Inspection",
                serviceLocation = prospect.location,
                cost = prospect.askedPrice,
                currency = prospect.currency,
                notes = "Promoted from Buying Guide.\n\nSeller Notes: ${prospect.sellerNotes}\n\nInspection Checklist: ${formatChecklist(prospect.checklistJson)}",
                createdAt = System.currentTimeMillis()
            )
            db.serviceRecordDao().insertServiceRecord(purchaseRecord)

            prospectDao.deleteProspect(prospect)
            com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
            onResult(newId)
        }
    }

    private fun formatChecklist(json: String): String {
        return try {
            val obj = JSONObject(json)
            val list = mutableListOf<String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                list.add(keys.next())
            }
            if (list.isEmpty()) "No items checked." else list.joinToString(", ")
        } catch (e: Exception) {
            "Error parsing checklist."
        }
    }
}
