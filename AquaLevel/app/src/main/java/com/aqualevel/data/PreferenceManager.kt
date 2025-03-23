package com.aqualevel.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preference keys for app settings
 */
private object PreferenceKeys {
    val LAST_USED_DEVICE = stringPreferencesKey("last_used_device")
    val FIRST_TIME_LAUNCH = booleanPreferencesKey("first_time_launch")
    val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aqualevel_preferences")

/**
 * Manager for app preferences using DataStore
 */
@Singleton
class PreferenceManager @Inject constructor(private val context: Context) {

    private val dataStore = context.dataStore

    /**
     * Check if this is the first time the app has been launched
     */
    suspend fun isFirstTimeLaunch(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[PreferenceKeys.FIRST_TIME_LAUNCH] ?: true
        } catch (e: Exception) {
            Timber.e(e, "Error reading first time launch preference")
            true
        }
    }

    /**
     * Set first time launch status
     */
    suspend fun setFirstTimeLaunch(isFirstTime: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.FIRST_TIME_LAUNCH] = isFirstTime
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving first time launch preference")
        }
    }

    /**
     * Get the ID of the last used device
     */
    suspend fun getLastUsedDevice(): String? {
        return try {
            val preferences = dataStore.data.first()
            preferences[PreferenceKeys.LAST_USED_DEVICE]
        } catch (e: Exception) {
            Timber.e(e, "Error reading last used device")
            null
        }
    }

    /**
     * Set the ID of the last used device
     */
    suspend fun setLastUsedDevice(deviceId: String) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.LAST_USED_DEVICE] = deviceId
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving last used device")
        }
    }

    /**
     * Get refresh interval for tank data (in seconds)
     */
    val refreshInterval: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.REFRESH_INTERVAL] ?: 60 // Default: 1 minute
        }

    /**
     * Set refresh interval for tank data (in seconds)
     */
    suspend fun setRefreshInterval(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.REFRESH_INTERVAL] = seconds
        }
    }

    /**
     * Check if notifications are enabled
     */
    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true // Default: enabled
        }

    /**
     * Set notification preference
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Get dark mode setting
     * Values: "system", "light", "dark"
     */
    val darkModeSetting: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferenceKeys.DARK_MODE] ?: "system" // Default: follow system
        }

    /**
     * Set dark mode setting
     */
    suspend fun setDarkMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_MODE] = mode
        }
    }

    /**
     * Check if onboarding has been completed
     */
    suspend fun isOnboardingCompleted(): Boolean {
        return try {
            val preferences = dataStore.data.first()
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] ?: false
        } catch (e: Exception) {
            Timber.e(e, "Error reading onboarding status")
            false
        }
    }

    /**
     * Set onboarding completion status
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}