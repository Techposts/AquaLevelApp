package com.aqualevel.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aqualevel.api.ApiResult
import com.aqualevel.api.model.TankData
import com.aqualevel.data.DeviceRepository
import com.aqualevel.data.database.entity.Device
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    // Device ID
    private var deviceId: String? = null

    // Device data
    private val _device = MutableLiveData<Device>()
    val device: LiveData<Device> = _device

    // Tank data
    private val _tankData = MutableLiveData<TankData>()
    val tankData: LiveData<TankData> = _tankData

    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String>("")
    val error: LiveData<String> = _error

    // Last refresh time
    private val _lastRefreshed = MutableLiveData<Long>()
    val lastRefreshed: LiveData<Long> = _lastRefreshed

    // Connection status
    private val _isConnected = MutableLiveData(false)
    val isConnected: LiveData<Boolean> = _isConnected

    /**
     * Initialize with device ID
     */
    fun initialize(deviceId: String) {
        this.deviceId = deviceId

        // Observe device from database
        viewModelScope.launch {
            deviceRepository.getDeviceById(deviceId)
                .collect { device ->
                    device?.let {
                        _device.value = it
                    }
                }
        }

        // Set this as the current device and refresh data
        viewModelScope.launch {
            val success = deviceRepository.setCurrentDevice(deviceId)
            if (success) {
                refreshDeviceData()
            } else {
                _error.value = "Failed to connect to device"
            }
        }
    }

    /**
     * Refresh device data
     */
    fun refreshDeviceData() {
        if (_isLoading.value == true) return

        deviceId?.let { id ->
            _isLoading.value = true

            viewModelScope.launch {
                try {
                    // Ensure we're using the correct device
                    deviceRepository.setCurrentDevice(id)

                    // Get tank data
                    when (val result = deviceRepository.getTankData()) {
                        is ApiResult.Success -> {
                            _tankData.value = result.data
                            _isConnected.value = true
                            _lastRefreshed.value = System.currentTimeMillis()
                        }
                        is ApiResult.Error -> {
                            Timber.e("Error getting tank data: ${result.errorMessage}")
                            _error.value = result.errorMessage
                            _isConnected.value = false
                        }
                        is ApiResult.Loading -> {
                            // Loading handled by _isLoading
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error refreshing device data")
                    _error.value = "Error refreshing data: ${e.message}"
                    _isConnected.value = false
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Calibrate empty tank
     */
    fun calibrateEmpty() {
        if (_isLoading.value == true) return

        _isLoading.value = true

        viewModelScope.launch {
            try {
                when (val result = deviceRepository.calibrateEmpty()) {
                    is ApiResult.Success -> {
                        _error.value = result.data // Actually a success message
                        refreshDeviceData()
                    }
                    is ApiResult.Error -> {
                        Timber.e("Error calibrating empty: ${result.errorMessage}")
                        _error.value = result.errorMessage
                    }
                    is ApiResult.Loading -> {
                        // Loading handled by _isLoading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error calibrating empty")
                _error.value = "Error calibrating: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Calibrate full tank
     */
    fun calibrateFull() {
        if (_isLoading.value == true) return

        _isLoading.value = true

        viewModelScope.launch {
            try {
                when (val result = deviceRepository.calibrateFull()) {
                    is ApiResult.Success -> {
                        _error.value = result.data // Actually a success message
                        refreshDeviceData()
                    }
                    is ApiResult.Error -> {
                        Timber.e("Error calibrating full: ${result.errorMessage}")
                        _error.value = result.errorMessage
                    }
                    is ApiResult.Loading -> {
                        // Loading handled by _isLoading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error calibrating full")
                _error.value = "Error calibrating: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Rename device
     */
    fun renameDevice(newName: String) {
        deviceId?.let { id ->
            viewModelScope.launch {
                try {
                    deviceRepository.updateDeviceName(id, newName)
                } catch (e: Exception) {
                    Timber.e(e, "Error renaming device")
                    _error.value = "Error renaming device: ${e.message}"
                }
            }
        }
    }

    /**
     * Delete device
     */
    fun deleteDevice() {
        deviceId?.let { id ->
            viewModelScope.launch {
                try {
                    deviceRepository.deleteDevice(id)
                } catch (e: Exception) {
                    Timber.e(e, "Error deleting device")
                    _error.value = "Error deleting device: ${e.message}"
                }
            }
        }
    }

    /**
     * Clear the current error message
     */
    fun clearError() {
        _error.value = ""
    }
}