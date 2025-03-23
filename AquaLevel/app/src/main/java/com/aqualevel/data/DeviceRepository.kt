package com.aqualevel.data

import android.content.Context
import com.aqualevel.api.ApiResult
import com.aqualevel.api.DeviceApi
import com.aqualevel.api.model.*
import com.aqualevel.data.database.dao.DeviceDao
import com.aqualevel.data.database.dao.WaterLevelHistoryDao
import com.aqualevel.data.database.entity.Device
import com.aqualevel.data.database.entity.WaterLevelHistory
import com.aqualevel.util.DiscoveredDevice
import com.aqualevel.util.MdnsDiscovery
import com.aqualevel.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that manages all interaction with AquaLevel devices
 */
@Singleton
class DeviceRepository @Inject constructor(
    private val context: Context,
    private val deviceDao: DeviceDao,
    private val waterLevelHistoryDao: WaterLevelHistoryDao,
    private val deviceApi: DeviceApi,
    private val mdnsDiscovery: MdnsDiscovery,
    private val networkUtils: NetworkUtils,
    private val preferenceManager: PreferenceManager
) {
    // Current device being accessed
    private var currentDeviceId: String? = null

    /**
     * Get all saved devices
     */
    fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()

    /**
     * Get favorite devices
     */
    fun getFavoriteDevices(): Flow<List<Device>> = deviceDao.getFavoriteDevices()

    /**
     * Get device by ID
     */
    fun getDeviceById(deviceId: String): Flow<Device?> = deviceDao.getDeviceById(deviceId)

    /**
     * Save a newly discovered device
     */
    suspend fun saveDiscoveredDevice(
        discoveredDevice: DiscoveredDevice,
        customName: String? = null
    ): String {
        val deviceId = discoveredDevice.hostname
        val name = customName ?: discoveredDevice.serviceName.removeSuffix(".local")

        withContext(Dispatchers.IO) {
            // Check if device already exists
            if (!deviceDao.deviceExists(deviceId)) {
                // Create new device entity
                val device = Device(
                    id = deviceId,
                    name = name,
                    ipAddress = discoveredDevice.ipAddress,
                    lastSeen = Date()
                )
                deviceDao.insertDevice(device)
                Timber.d("Saved new device: $deviceId")
            } else {
                // Update existing device
                val existingDevice = deviceDao.getDeviceByIdSuspend(deviceId)
                existingDevice?.let {
                    val updatedDevice = it.copy(
                        ipAddress = discoveredDevice.ipAddress,
                        lastSeen = Date(),
                        name = customName ?: it.name
                    )
                    deviceDao.updateDevice(updatedDevice)
                    Timber.d("Updated existing device: $deviceId")
                }
            }
        }

        return deviceId
    }

    /**
     * Update device name
     */
    suspend fun updateDeviceName(deviceId: String, name: String) {
        deviceDao.updateDeviceName(deviceId, name)
    }

    /**
     * Set device as favorite
     */
    suspend fun setDeviceFavorite(deviceId: String, isFavorite: Boolean) {
        deviceDao.updateFavoriteStatus(deviceId, isFavorite)
    }

    /**
     * Delete device
     */
    suspend fun deleteDevice(deviceId: String) {
        val device = deviceDao.getDeviceByIdSuspend(deviceId)
        device?.let {
            deviceDao.deleteDevice(it)
        }
    }

    /**
     * Set current device for API calls
     */
    suspend fun setCurrentDevice(deviceId: String): Boolean {
        val device = deviceDao.getDeviceByIdSuspend(deviceId) ?: return false

        // Set device address for API calls
        deviceApi.setDeviceAddress(device.ipAddress)
        currentDeviceId = deviceId

        // Save as last used device
        preferenceManager.setLastUsedDevice(deviceId)

        return true
    }

    /**
     * Get current device ID
     */
    fun getCurrentDeviceId(): String? = currentDeviceId

    /**
     * Get tank data for current device
     */
    suspend fun getTankData(): ApiResult<TankData> {
        val result = deviceApi.getTankData()

        // If successful, save the data for historical records
        if (result is ApiResult.Success && currentDeviceId != null) {
            val tankData = result.data

            // Update device info in database
            deviceDao.updateDeviceStatus(
                currentDeviceId!!,
                tankData.percentage,
                tankData.volume,
                Date().time
            )

            // Add to history (if appropriate)
            addHistoricalReading(tankData)
        }

        return result
    }

    /**
     * Add tank data to historical records
     * Only adds data periodically to avoid filling the database
     */
    private suspend fun addHistoricalReading(tankData: TankData) {
        if (currentDeviceId == null) return

        // Get latest reading to check if we should add a new one
        val latestReading = waterLevelHistoryDao.getLatestReading(currentDeviceId!!)
        val now = Date()

        // If no previous reading or it's been at least 15 minutes
        if (latestReading == null ||
            (now.time - latestReading.timestamp.time) > 15 * 60 * 1000) {

            // Add new historical reading
            val history = WaterLevelHistory(
                deviceId = currentDeviceId!!,
                timestamp = now,
                percentage = tankData.percentage,
                volume = tankData.volume,
                distance = tankData.distance,
                waterLevel = tankData.waterLevel
            )

            waterLevelHistoryDao.insertReading(history)
            Timber.d("Added historical reading: $history")

            // Clean up old readings (keep last 30 days)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            val cutoffDate = calendar.time

            withContext(Dispatchers.IO) {
                waterLevelHistoryDao.deleteOldReadings(currentDeviceId!!, cutoffDate)
            }
        }
    }

    /**
     * Get device settings
     */
    suspend fun getDeviceSettings(): ApiResult<DeviceSettings> {
        return deviceApi.getDeviceSettings()
    }

    /**
     * Update tank settings
     */
    suspend fun updateTankSettings(settings: TankSettingsRequest): ApiResult<Unit> {
        return deviceApi.updateTankSettings(settings)
    }

    /**
     * Update sensor settings
     */
    suspend fun updateSensorSettings(settings: SensorSettingsRequest): ApiResult<Unit> {
        return deviceApi.updateSensorSettings(settings)
    }

    /**
     * Update alert settings
     */
    suspend fun updateAlertSettings(settings: AlertSettingsRequest): ApiResult<Unit> {
        return deviceApi.updateAlertSettings(settings)
    }

    /**
     * Calibrate empty tank
     */
    suspend fun calibrateEmpty(): ApiResult<String> {
        return deviceApi.calibrateEmpty()
    }

    /**
     * Calibrate full tank
     */
    suspend fun calibrateFull(): ApiResult<String> {
        return deviceApi.calibrateFull()
    }

    /**
     * Scan for networks from device
     */
    suspend fun scanNetworks(): ApiResult<List<WiFiNetwork>> {
        return deviceApi.scanNetworks()
    }

    /**
     * Configure network settings (for setup process)
     */
    suspend fun configureNetwork(request: NetworkSettingsRequest): ApiResult<Unit> {
        return deviceApi.configureNetwork(request)
    }

    /**
     * Scan for AquaLevel devices on the network
     */
    fun scanForDevices(): Flow<DiscoveredDevice> {
        return mdnsDiscovery.discoverDevices()
    }

    /**
     * Find a device by hostname
     */
    suspend fun findDeviceByHostname(hostname: String): DiscoveredDevice? {
        return mdnsDiscovery.discoverDeviceByHostname(hostname)
    }

    /**
     * Get water level history for a device
     */
    fun getWaterLevelHistory(deviceId: String): Flow<List<WaterLevelHistory>> {
        return waterLevelHistoryDao.getHistoryForDevice(deviceId)
    }

    /**
     * Get water level history for a date range
     */
    fun getWaterLevelHistoryForRange(
        deviceId: String,
        startDate: Date,
        endDate: Date
    ): Flow<List<WaterLevelHistory>> {
        return waterLevelHistoryDao.getHistoryForDateRange(deviceId, startDate, endDate)
    }

    /**
     * Check connection to a device
     */
    suspend fun checkDeviceConnection(deviceId: String): Boolean {
        // Make sure we're using this device
        if (!setCurrentDevice(deviceId)) return false

        // Try to get tank data as a connection test
        return when (deviceApi.getTankData()) {
            is ApiResult.Success -> true
            else -> false
        }
    }

    /**
     * Reconnect to last used device
     */
    suspend fun reconnectToLastDevice(): String? {
        val lastDeviceId = preferenceManager.getLastUsedDevice() ?: return null

        // Check if device still exists in database
        val device = deviceDao.getDeviceByIdSuspend(lastDeviceId) ?: return null

        // Try to connect to the device
        return if (setCurrentDevice(lastDeviceId) && checkDeviceConnection(lastDeviceId)) {
            lastDeviceId
        } else {
            null
        }
    }
}