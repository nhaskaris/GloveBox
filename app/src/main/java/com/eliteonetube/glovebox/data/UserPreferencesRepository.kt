package com.eliteonetube.glovebox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemePreference {
    LIGHT, DARK, SYSTEM
}

class UserPreferencesRepository(private val context: Context) {
    private val dataStore = context.dataStore

    object PreferencesKeys {
        val ACTIVE_VEHICLE_ID = longPreferencesKey("active_vehicle_id")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val IS_VIN_FEATURE_ENABLED = booleanPreferencesKey("is_vin_feature_enabled")
        val USER_COUNTRY = stringPreferencesKey("user_country")
        val PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    }

    val activeVehicleId: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_VEHICLE_ID]
        }

    val themePreference: Flow<ThemePreference> = dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_PREFERENCE] ?: ThemePreference.SYSTEM.name
            ThemePreference.valueOf(themeName)
        }

    val appLanguage: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE]
        }

    val unitSystem: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.UNIT_SYSTEM] ?: "km"
        }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        }

    val isVinFeatureEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_VIN_FEATURE_ENABLED] ?: true
        }

    val userCountry: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_COUNTRY] ?: "Global"
        }

    val preferredCurrency: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PREFERRED_CURRENCY] ?: "USD"
        }


    val lastBackupTime: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIME]
        }

    suspend fun setActiveVehicleId(vehicleId: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_VEHICLE_ID] = vehicleId
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_PREFERENCE] = theme.name
        }
    }

    suspend fun setAppLanguage(languageCode: String?) {
        dataStore.edit { preferences ->
            if (languageCode == null) {
                preferences.remove(PreferencesKeys.APP_LANGUAGE)
            } else {
                preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
            }
        }
    }

    suspend fun setUnitSystem(unit: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNIT_SYSTEM] = unit
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setVinFeatureEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_VIN_FEATURE_ENABLED] = enabled
        }
    }

    suspend fun setUserCountry(countryCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_COUNTRY] = countryCode
        }
    }

    suspend fun setPreferredCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_CURRENCY] = currencyCode
        }
    }


    suspend fun setLastBackupTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIME] = timestamp
        }
    }
}
