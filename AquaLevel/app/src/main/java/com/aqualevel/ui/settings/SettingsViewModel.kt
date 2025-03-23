package com.aqualevel.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqualevel.api.ApiResult
import com.aqualevel.api.model.AlertSettingsRequest
import com.aqualevel.api.model.DeviceSettings
import com.aqualevel.api.model.SensorSettingsRequest
import com.aqualevel.api.model.TankSettingsRequest
import com.aqualevel.data.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    // Device ID
    private var deviceId: String? = null

    // Device settings
    private val _deviceSettings = MutableLiveData<DeviceSettings>()
    val deviceSettings: LiveData<DeviceSettings> = _deviceSettings

    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Save result
    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    // Error state
    private val _error = MutableLiveData<String>("")
    val error: LiveData<String> = _error

    /**
     * Initialize with device ID
     */
    fun initialize(deviceId: String) {
        this.deviceId = deviceId

        // Set this as the current device and load settings
        viewModelScope.launch {
            val success = deviceRepository.setCurrentDevice(deviceId)
            if (success) {
                loadDeviceSettings()
            } else {
                _error.value = "Failed to connect to device"
            }
        }
    }

    /**
     * Load device settings
     */
    private fun loadDeviceSettings() {
        if (_isLoading.value == true) return

        _isLoading.value = true

        viewModelScope.launch {
            try {
                when (val result = deviceRepository.getDeviceSettings()) {
                    is ApiResult.Success -> {
                        _deviceSettings.value = result.data
                    }
                    is ApiResult.Error -> {
                        Timber.e("Error getting device settings: ${result.errorMessage}")
                        _error.value = result.errorMessage
                    }
                    is ApiResult.Loading -> {
                        // Loading handled by _isLoading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading device settings")
                _error.value = "Error loading settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save tank settings
     */
    suspend fun saveTankSettings(settings: TankSettingsRequest) {
        if (_isLoading.value == true) return

        _isLoading.value = true

        try {
            when (val result = deviceRepository.updateTankSettings(settings)) {
                is ApiResult.Success -> {
                    _saveResult.value = true
                    loadDeviceSettings() // Reload settings
                }
                is ApiResult.Error -> {
                    Timber.e("Error saving tank settings: ${result.errorMessage}")
                    _error.value = result.errorMessage
                    _saveResult.value = false
                }
                is ApiResult.Loading -> {
                    // Loading handled by _isLoading
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving tank settings")
            _error.value = "Error saving settings: ${e.message}"
            _saveResult.value = false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Save sensor settings
     */
    suspend fun saveSensorSettings(settings: SensorSettingsRequest) {
        if (_isLoading.value == true) return

        _isLoading.value = true

        try {
            when (val result = deviceRepository.updateSensorSettings(settings)) {
                is ApiResult.Success -> {
                    _saveResult.value = true
                    loadDeviceSettings() // Reload settings
                }
                is ApiResult.Error -> {
                    Timber.e("Error saving sensor settings: ${result.errorMessage}")
                    _error.value = result.errorMessage
                    _saveResult.value = false
                }
                is ApiResult.Loading -> {
                    // Loading handled by _isLoading
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving sensor settings")
            _error.value = "Error saving settings: ${e.message}"
            _saveResult.value = false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Save alert settings
     */
    suspend fun saveAlertSettings(settings: AlertSettingsRequest) {
        if (_isLoading.value == true) return

        _isLoading.value = true

        try {
            when (val result = deviceRepository.updateAlertSettings(settings)) {
                is ApiResult.Success -> {
                    _saveResult.value = true
                    loadDeviceSettings() // Reload settings
                }
                is ApiResult.Error -> {
                    Timber.e("Error saving alert settings: ${result.errorMessage}")
                    _error.value = result.errorMessage
                    _saveResult.value = false
                }
                is ApiResult.Loading -> {
                    // Loading handled by _isLoading
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving alert settings")
            _error.value = "Error saving settings: ${e.message}"
            _saveResult.value = false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clear the current error message
     */
    fun clearError() {
        _error.value = ""
    }
}