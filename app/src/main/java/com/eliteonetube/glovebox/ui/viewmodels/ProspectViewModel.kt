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
    val sellerNotes: String = "",
    val location: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProspectViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GloveboxDatabase.getDatabase(application)
    private val prospectDao = db.prospectVehicleDao()
    private val vehicleDao = db.vehicleDao()
    
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

    fun onMakeChange(v: String) { _formState.value = _formState.value.copy(make = v) }
    fun onModelChange(v: String) { _formState.value = _formState.value.copy(model = v) }
    fun onYearChange(v: String) { _formState.value = _formState.value.copy(year = v) }
    fun onVinChange(v: String) { _formState.value = _formState.value.copy(vin = v) }
    fun onPriceChange(v: String) { _formState.value = _formState.value.copy(askedPrice = v) }
    fun onNotesChange(v: String) { _formState.value = _formState.value.copy(sellerNotes = v) }
    fun onLocationChange(v: String) { _formState.value = _formState.value.copy(location = v) }

    fun loadProspect(id: Long) {
        if (id == 0L) {
            _formState.value = ProspectFormState()
            return
        }
        viewModelScope.launch {
            prospectDao.getProspectById(id)?.let { p ->
                _formState.value = ProspectFormState(
                    id = p.id,
                    make = p.make,
                    model = p.model,
                    year = p.year.toString(),
                    vin = p.vin ?: "",
                    askedPrice = p.askedPrice?.toString() ?: "",
                    sellerNotes = p.sellerNotes,
                    location = p.location
                )
            }
        }
    }

    fun decodeVin() {
        val vin = _formState.value.vin
        if (vin.length != 17) return
        
        _formState.value = _formState.value.copy(isLoading = true, error = null)
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
            } catch (e: Exception) {
                _formState.value = _formState.value.copy(isLoading = false, error = "Connection Error")
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
            sellerNotes = s.sellerNotes,
            location = s.location
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
                odometerUnit = "km"
            )
            val newId = vehicleDao.insertVehicle(vehicle)
            prospectDao.deleteProspect(prospect)
            onResult(newId)
        }
    }
}
