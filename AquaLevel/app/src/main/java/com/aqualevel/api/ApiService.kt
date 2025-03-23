package com.aqualevel.api

import com.aqualevel.api.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for communicating with AquaLevel devices
 */
interface ApiService {
    /**
     * Get current tank data
     */
    @GET("tank-data")
    suspend fun getTankData(): Response<TankData>

    /**
     * Get device settings
     */
    @GET("settings")
    suspend fun getDeviceSettings(): Response<DeviceSettings>

    /**
     * Scan for available WiFi networks
     */
    @GET("scannetworks")
    suspend fun scanNetworks(): Response<List<WiFiNetwork>>

    /**
     * Configure WiFi connection
     */
    @GET("network")
    suspend fun configureNetwork(
        @Query("ssid") ssid: String,
        @Query("password") password: String,
        @Query("deviceName") deviceName: String
    ): Response<Unit>

    /**
     * Reset WiFi settings
     */
    @GET("resetwifi")
    suspend fun resetWifi(): Response<Unit>

    /**
     * Update tank settings
     */
    @GET("set")
    suspend fun updateTankSettings(
        @Query("tankHeight") tankHeight: Float? = null,
        @Query("tankDiameter") tankDiameter: Float? = null,
        @Query("tankVolume") tankVolume: Float? = null
    ): Response<Unit>

    /**
     * Update sensor settings
     */
    @GET("set")
    suspend fun updateSensorSettings(
        @Query("sensorOffset") sensorOffset: Float? = null,
        @Query("emptyDistance") emptyDistance: Float? = null,
        @Query("fullDistance") fullDistance: Float? = null,
        @Query("measurementInterval") measurementInterval: Int? = null,
        @Query("readingSmoothing") readingSmoothing: Int? = null
    ): Response<Unit>

    /**
     * Update alert settings
     */
    @GET("set")
    suspend fun updateAlertSettings(
        @Query("alertLevelLow") alertLevelLow: Int? = null,
        @Query("alertLevelHigh") alertLevelHigh: Int? = null,
        @Query("alertsEnabled") alertsEnabled: Boolean? = null
    ): Response<Unit>

    /**
     * Calibrate empty tank
     */
    @GET("calibrate")
    suspend fun calibrateEmpty(
        @Query("type") type: String = "empty"
    ): Response<String>

    /**
     * Calibrate full tank
     */
    @GET("calibrate")
    suspend fun calibrateFull(
        @Query("type") type: String = "full"
    ): Response<String>
}