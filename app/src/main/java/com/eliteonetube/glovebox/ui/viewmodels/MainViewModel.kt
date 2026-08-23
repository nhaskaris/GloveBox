package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.ThemePreference
import com.eliteonetube.glovebox.data.UserPreferencesRepository
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.data.backup.LocalBackupManager
import com.eliteonetube.glovebox.data.backup.BackupResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()
    private val backupManager = LocalBackupManager(application)

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus = _backupStatus.asStateFlow()

    private val _needsRecreate = MutableStateFlow(false)
    val needsRecreate = _needsRecreate.asStateFlow()

    val themePreference: StateFlow<ThemePreference> = userPreferencesRepository.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreference.SYSTEM)

    val appLanguage: StateFlow<String?> = userPreferencesRepository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unitSystem: StateFlow<String> = userPreferencesRepository.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "km")

    val activeVehicleId: StateFlow<Long?> = userPreferencesRepository.activeVehicleId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val vehicles: StateFlow<List<Vehicle>> = vehicleDao.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnboardingCompleted: StateFlow<Boolean?> = userPreferencesRepository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isVinFeatureEnabled: StateFlow<Boolean> = userPreferencesRepository.isVinFeatureEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val userCountry: StateFlow<String> = userPreferencesRepository.userCountry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Global")

    val preferredCurrency: StateFlow<String> = userPreferencesRepository.preferredCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val lastBackupTime: StateFlow<Long?> = userPreferencesRepository.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            userPreferencesRepository.setThemePreference(theme)
        }
    }

    fun setAppLanguage(languageCode: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setAppLanguage(languageCode)
        }
    }

    fun setUnitSystem(unit: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUnitSystem(unit)
        }
    }

    fun setActiveVehicleId(vehicleId: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setActiveVehicleId(vehicleId)
            com.eliteonetube.glovebox.util.WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun setVinFeatureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setVinFeatureEnabled(enabled)
        }
    }

    fun setUserCountry(countryCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setUserCountry(countryCode)
        }
    }

    fun setPreferredCurrency(currencyCode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferredCurrency(currencyCode)
        }
    }


    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = "Exporting backup..."
            val result = backupManager.exportDatabase(uri)
            handleBackupResult(result)
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _backupStatus.value = "Importing backup..."
            val result = backupManager.importDatabase(uri)
            handleBackupResult(result)
            if (result is BackupResult.Success) {
                _backupStatus.value = "Import successful! Refreshing..."
                _needsRecreate.value = true
            }
        }
    }

    fun onRecreated() {
        _needsRecreate.value = false
    }

    fun handleBackupResult(result: BackupResult) {
        viewModelScope.launch {
            when (result) {
                is BackupResult.Success -> {
                    userPreferencesRepository.setLastBackupTime(System.currentTimeMillis())
                    _backupStatus.value = "Operation successful!"
                }
                is BackupResult.Error -> {
                    _backupStatus.value = "Failed: ${result.message}"
                }
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}