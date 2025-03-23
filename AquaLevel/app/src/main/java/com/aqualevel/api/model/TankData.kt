package com.aqualevel.api.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for water tank measurements and status.
 * Corresponds to the JSON response from the /tank-data endpoint.
 */
data class TankData(
    // Current tank measurements
    @SerializedName("distance") val distance: Float,
    @SerializedName("waterLevel") val waterLevel: Float,
    @SerializedName("percentage") val percentage: Float,
    @SerializedName("volume") val volume: Float,

    // Tank parameters
    @SerializedName("tankHeight") val tankHeight: Float,
    @SerializedName("tankDiameter") val tankDiameter: Float,
    @SerializedName("tankVolume") val tankVolume: Float,

    // Alert settings
    @SerializedName("alertLevelLow") val alertLevelLow: Int,
    @SerializedName("alertLevelHigh") val alertLevelHigh: Int,
    @SerializedName("alertsEnabled") val alertsEnabled: Boolean
)