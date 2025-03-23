package com.aqualevel.api.model

/**
 * Model for WiFi configuration request
 */
data class NetworkSettingsRequest(
    val ssid: String,
    val password: String,
    val deviceName: String
)

/**
 * Model representing a scanned WiFi network from the device
 */
data class WiFiNetwork(
    val ssid: String,
    val rssi: Int,
    val secure: Boolean
) {
    /**
     * Get signal strength level from 0-4 based on RSSI
     */
    fun getSignalStrength(): Int {
        return when {
            rssi >= -50 -> 4  // Excellent
            rssi >= -60 -> 3  // Good
            rssi >= -70 -> 2  // Fair
            rssi >= -80 -> 1  // Poor
            else -> 0         // Very poor
        }
    }
}

/**
 * Network scan response
 */
data class NetworkScanResponse(
    val networks: List<WiFiNetwork>
)