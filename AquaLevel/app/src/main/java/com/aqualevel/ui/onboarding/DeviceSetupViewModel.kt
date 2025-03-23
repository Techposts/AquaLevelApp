package com.aqualevel.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aqualevel.api.ApiResult
import com.aqualevel.api.model.NetworkSettingsRequest
import com.aqualevel.api.model.WiFiNetwork
import com.aqualevel.data.DeviceRepository
import com.aqualevel.util.DiscoveredDevice
import com.aqualevel.util.MdnsDiscovery
import com.aqualevel.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DeviceSetupViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val networkUtils: NetworkUtils,
    private val mdnsDiscovery: MdnsDiscovery
) : ViewModel() {

    // Current state
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>("")
    val error: LiveData<String> = _error

    // Connection to device AP
    private val _connectedToDeviceAp = MutableLiveData(false)
    val connectedToDeviceAp: LiveData<Boolean> = _connectedToDeviceAp

    private val _currentSsid = MutableLiveData<String?>(null)
    val currentSsid: LiveData<String?> = _currentSsid

    // WiFi setup
    private val _availableNetworks = MutableLiveData<List<WiFiNetwork>>(emptyList())
    val availableNetworks: LiveData<List<WiFiNetwork>> = _availableNetworks

    private val _wifiSetupComplete = MutableLiveData(false)
    val wifiSetupComplete: LiveData<Boolean> = _wifiSetupComplete

    // Device discovery
    private val _deviceDiscovered = MutableLiveData(false)
    val deviceDiscovered: LiveData<Boolean> = _deviceDiscovered

    private val _discoveredDevice = MutableLiveData<DiscoveredDevice?>(null)
    val discoveredDevice: LiveData<DiscoveredDevice?> = _discoveredDevice

    // Save important setup data
    private var deviceHostname: String? = null

    /**
     * Refresh WiFi state to check if connected to AquaLevel device
     */
    fun refreshWifiState() {
        viewModelScope.launch {
            try {
                val currentSsid = networkUtils.getCurrentWifiSsid()
                _currentSsid.value = currentSsid

                val isConnectedToDevice = networkUtils.isConnectedToAquaLevelAP()
                _connectedToDeviceAp.value = isConnectedToDevice

                // If we're connected to the device, set the device IP for API calls
                if (isConnectedToDevice) {
                    deviceRepository.deviceApi.setDeviceAddress("http://192.168.4.1")
                }

                Timber.d("Connected to device AP: $isConnectedToDevice, SSID: $currentSsid")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing WiFi state")
            }
        }
    }

    /**
     * Scan for available WiFi networks
     */
    suspend fun scanNetworks() {
        if (_loading.value == true) return

        _loading.value = true

        try {
            when (val result = deviceRepository.scanNetworks()) {
                is ApiResult.Success -> {
                    _availableNetworks.value = result.data
                    Timber.d("Found ${result.data.size} networks")
                }
                is ApiResult.Error -> {
                    _error.value = "Failed to scan networks: ${result.errorMessage}"
                    Timber.e("Failed to scan networks: ${result.errorMessage}")
                }
                is ApiResult.Loading -> {
                    // This state is handled by _loading
                }
            }
        } catch (e: Exception) {
            _error.value = "Error scanning networks: ${e.message}"
            Timber.e(e, "Error scanning networks")
        } finally {
            _loading.value = false
        }
    }

    /**
     * Configure device WiFi settings
     */
    suspend fun configureDeviceWifi(ssid: String, password: String, deviceName: String) {
        if (_loading.value == true) return

        _loading.value = true

        try {
            // Create request
            val request = NetworkSettingsRequest(
                ssid = ssid,
                password = password,
                deviceName = deviceName
            )

            // Save hostname for later discovery
            deviceHostname = networkUtils.getDeviceHostname(deviceName)

            // Send configuration to device
            when (val result = deviceRepository.configureNetwork(request)) {
                is ApiResult.Success -> {
                    Timber.d("WiFi configuration succeeded")
                    _wifiSetupComplete.value = true

                    // Start looking for device on network
                    startDeviceDiscovery()
                }
                is ApiResult.Error -> {
                    _error.value = "Failed to configure WiFi: ${result.errorMessage}"
                    Timber.e("Failed to configure WiFi: ${result.errorMessage}")
                }
                is ApiResult.Loading -> {
                    // This state is handled by _loading
                }
            }
        } catch (e: Exception) {
            _error.value = "Error configuring WiFi: ${e.message}"
            Timber.e(e, "Error configuring WiFi")
        } finally {
            _loading.value = false
        }
    }

    /**
     * Start device discovery on the local network
     * This should be called after the device has been configured with WiFi settings
     */
    private fun startDeviceDiscovery() {
        viewModelScope.launch {
            try {
                Timber.d("Starting device discovery for $deviceHostname")

                // Wait a few seconds for the device to connect to WiFi
                delay(5000)

                // Try to find device directly by hostname
                val device = mdnsDiscovery.discoverDeviceByHostname(deviceHostname ?: "")

                if (device != null) {
                    handleDiscoveredDevice(device)
                } else {
                    // Start general discovery, maybe we'll find it with a different name
                    Timber.d("Didn't find specific device, starting general discovery")
                    startGeneralDiscovery()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during device discovery")
                _error.value = "Error finding device on network: ${e.message}"
            }
        }
    }

    /**
     * Start general device discovery on the local network
     */
    private fun startGeneralDiscovery() {
        viewModelScope.launch {
            try {
                mdnsDiscovery.discoverDevices()
                    .collect { device ->
                        Timber.d("Discovered device: $device")
                        handleDiscoveredDevice(device)
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error during general device discovery")
            }
        }
    }

    /**
     * Handle a discovered device
     */
    private suspend fun handleDiscoveredDevice(device: DiscoveredDevice) {
        _discoveredDevice.value = device
        _deviceDiscovered.value = true

        // Save the device to our repository
        val deviceId = deviceRepository.saveDiscoveredDevice(device)

        // Set as current device
        deviceRepository.setCurrentDevice(deviceId)
    }

    /**
     * Clear current error message
     */
    fun clearError() {
        _error.value = ""
    }
}