package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.data.entity.VehicleDocument
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

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

    fun scanDocument() {
        val uriStr = _uiState.value.photoUri ?: return
        val uri = Uri.parse(uriStr)
        val context = getApplication<Application>()
        
        _uiState.value = _uiState.value.copy(isOcrLoading = true)
        
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { 
                        BitmapFactory.decodeStream(it)
                    }
                } ?: return@launch

                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val fullText = visionText.text
                        val expiryDate = extractExpiryDate(fullText)
                        
                        _uiState.value = _uiState.value.copy(
                            isOcrLoading = false,
                            expiryDate = expiryDate ?: _uiState.value.expiryDate,
                            ocrResult = if (expiryDate != null) "Found expiration date!" else "No date found"
                        )
                    }
                    .addOnFailureListener {
                        _uiState.value = _uiState.value.copy(isOcrLoading = false, ocrResult = "OCR Failed")
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isOcrLoading = false, ocrResult = "Error: ${e.message}")
            }
        }
    }

    private fun extractExpiryDate(text: String): Long? {
        // Common patterns for expiration
        val keywords = listOf("EXP", "EXPIRES", "EXPIRATION", "VALID UNTIL", "THRU")
        val lines = text.split("\n")
        
        // Date formats to try
        val dateFormats = listOf(
            "MM/dd/yyyy", "MM-dd-yyyy", "MM/dd/yy", "yyyy-MM-dd", "dd MMM yyyy"
        )

        for (keyword in keywords) {
            val index = text.uppercase().indexOf(keyword)
            if (index != -1) {
                // Look at text immediately following the keyword
                val sub = text.substring(index, (index + 40).coerceAtMost(text.length))
                val date = findDateInString(sub, dateFormats)
                if (date != null) return date
            }
        }
        
        // Fallback: search entire text for anything that looks like a date
        return findDateInString(text, dateFormats)
    }

    private fun findDateInString(input: String, formats: List<String>): Long? {
        val datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})|(\\d{4}-\\d{2}-\\d{2})")
        val matcher = datePattern.matcher(input)
        
        while (matcher.find()) {
            val dateStr = matcher.group()
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    sdf.isLenient = false
                    val date = sdf.parse(dateStr)
                    if (date != null && date.after(Date())) { // Only accept future dates
                        return date.time
                    }
                } catch (e: Exception) {}
            }
        }
        return null
    }

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
    val linkedVehicleId: Long? = null,
    val isOcrLoading: Boolean = false,
    val ocrResult: String? = null
)
