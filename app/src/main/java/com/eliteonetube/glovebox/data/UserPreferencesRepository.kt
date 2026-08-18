package com.eliteonetube.glovebox.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
}
