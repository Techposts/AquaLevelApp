package com.aqualevel.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.aqualevel.data.DeviceRepository
import com.aqualevel.data.database.entity.Device
import com.aqualevel.util.DiscoveredDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    // List of saved devices
    val devices: LiveData<List<Device>> = deviceRepository.getAllDevices().asLiveData()

    // Discovered devices not yet saved
    private val _discoveredDevices = MutableLiveData<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: LiveData<List<DiscoveredDevice>> = _discoveredDevices

    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Error state
    private val _error = MutableLiveData<String>("")
    val error: LiveData<String> = _error

    // Device discovery in progress
    private var discoveryActive = false

    /**
     * Start discovering devices on the network
     */
    fun startDeviceDiscovery() {
        if (discoveryActive) return

        discoveryActive = true
        _isLoading.value = true

        val foundDevices = mutableListOf<DiscoveredDevice>()

        viewModelScope.launch {
            try {
                deviceRepository.scanForDevices().collect { device ->
                    Timber.d("Discovered device: $device")

                    // Only add to discovered list if not already saved
                    val deviceId = device.hostname
                    val alreadySaved = deviceRepository.getDeviceById(deviceId)
                        .map { it != null }
                        .asLiveData()
                        .value ?: false

                    if (!alreadySaved && !foundDevices.any { it.hostname == deviceId }) {
                        foundDevices.add(device)
                        _discoveredDevices.value = foundDevices.toList()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error discovering devices")
                _error.value = "Error discovering devices: ${e.message}"
            } finally {
                _isLoading.value = false
                discoveryActive = false
            }
        }
    }

    /**
     * Add all discovered devices to the database
     */
    fun addDiscoveredDevices() {
        val devices = _discoveredDevices.value ?: return

        viewModelScope.launch {
            try {
                for (device in devices) {
                    deviceRepository.saveDiscoveredDevice(device)
                }

                // Clear the discovered devices list
                _discoveredDevices.value = emptyList()
            } catch (e: Exception) {
                Timber.e(e, "Error adding discovered devices")
                _error.value = "Error adding devices: ${e.message}"
            }
        }
    }

    /**
     * Refresh the device list
     */
    fun refreshDevices() {
        _isLoading.value = true

        // Start device discovery
        startDeviceDiscovery()

        // Update existing devices
        viewModelScope.launch {
            try {
                val allDevices = devices.value ?: emptyList()

                for (device in allDevices) {
                    // Check if device is online and update status
                    if (deviceRepository.setCurrentDevice(device.id)) {
                        try {
                            val tankDataResult = deviceRepository.getTankData()
                            if (tankDataResult is com.aqualevel.api.ApiResult.Success) {
                                // Data fetched successfully, device is online
                                // The repository will automatically update the device status
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Error refreshing device ${device.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing devices")
                _error.value = "Error refreshing devices: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Toggle favorite status for a device
     */
    suspend fun toggleFavorite(deviceId: String) {
        try {
            val device = deviceRepository.getDeviceById(deviceId)
                .asLiveData()
                .value

            device?.let {
                deviceRepository.setDeviceFavorite(deviceId, !it.isFavorite)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error toggling favorite for device $deviceId")
            _error.value = "Error updating device: ${e.message}"
        }
    }

    /**
     * Try to reconnect to the last used device
     */
    suspend fun reconnectToLastDevice() {
        try {
            deviceRepository.reconnectToLastDevice()
        } catch (e: Exception) {
            Timber.e(e, "Error reconnecting to last device")
        }
    }

    /**
     * Clear the current error message
     */
    fun clearError() {
        _error.value = ""
    }
}