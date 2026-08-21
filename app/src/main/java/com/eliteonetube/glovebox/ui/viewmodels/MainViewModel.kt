package com.eliteonetube.glovebox.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eliteonetube.glovebox.data.ThemePreference
import com.eliteonetube.glovebox.data.UserPreferencesRepository
import com.eliteonetube.glovebox.data.GloveboxDatabase
import com.eliteonetube.glovebox.data.entity.Vehicle
import com.eliteonetube.glovebox.data.backup.GoogleDriveBackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userPreferencesRepository = UserPreferencesRepository(application)
    private val vehicleDao = GloveboxDatabase.getDatabase(application).vehicleDao()
    private val backupManager = GoogleDriveBackupManager(application)

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus = _backupStatus.asStateFlow()

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

    val isDriveBackupEnabled: StateFlow<Boolean> = userPreferencesRepository.isDriveBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    fun setDriveBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDriveBackupEnabled(enabled)
        }
    }

    fun getBackupSignInIntent() = backupManager.getSignInIntent()

    fun performBackup() {
        viewModelScope.launch {
            _backupStatus.value = "Backing up..."
            val success = backupManager.performBackup()
            if (success) {
                userPreferencesRepository.setLastBackupTime(System.currentTimeMillis())
                _backupStatus.value = "Backup successful!"
            } else {
                _backupStatus.value = "Backup failed. Check sign-in."
            }
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }
}
