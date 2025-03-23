package com.aqualevel.api.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for device configuration settings.
 * Corresponds to the JSON response from the /settings endpoint.
 */
data class DeviceSettings(
    // Tank dimensions
    @SerializedName("tankHeight") val tankHeight: Float,
    @SerializedName("tankDiameter") val tankDiameter: Float,
    @SerializedName("tankVolume") val tankVolume: Float,

    // Sensor configuration
    @SerializedName("sensorOffset") val sensorOffset: Float,
    @SerializedName("emptyDistance") val emptyDistance: Float,
    @SerializedName("fullDistance") val fullDistance: Float,
    @SerializedName("measurementInterval") val measurementInterval: Int,
    @SerializedName("readingSmoothing") val readingSmoothing: Int,

    // Alert settings
    @SerializedName("alertLevelLow") val alertLevelLow: Int,
    @SerializedName("alertLevelHigh") val alertLevelHigh: Int,
    @SerializedName("alertsEnabled") val alertsEnabled: Boolean
)

/**
 * Tank settings update request model
 */
data class TankSettingsRequest(
    val tankHeight: Float?,
    val tankDiameter: Float?,
    val tankVolume: Float?
)

/**
 * Sensor settings update request model
 */
data class SensorSettingsRequest(
    val sensorOffset: Float?,
    val emptyDistance: Float?,
    val fullDistance: Float?,
    val measurementInterval: Int?,
    val readingSmoothing: Int?
)

/**
 * Alert settings update request model
 */
data class AlertSettingsRequest(
    val alertLevelLow: Int?,
    val alertLevelHigh: Int?,
    val alertsEnabled: Boolean?
)