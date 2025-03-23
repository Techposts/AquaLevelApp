package com.aqualevel.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.net.InetAddress
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkUtils @Inject constructor(private val context: Context) {

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val wifiManager by lazy {
        context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    /**
     * Check if device is connected to a WiFi network
     */
    fun isConnectedToWifi(): Boolean {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(
            connectivityManager.activeNetwork
        )
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    /**
     * Get current WiFi SSID
     */
    fun getCurrentWifiSsid(): String? {
        if (!isConnectedToWifi()) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - getConnectionInfo() is deprecated
            val networkInfo = connectivityManager.activeNetwork ?: return null
            val capabilities = connectivityManager.getNetworkCapabilities(networkInfo) ?: return null
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                // We know we're on WiFi but need permission to get SSID in Android 11+
                try {
                    val wifiInfo = wifiManager.connectionInfo
                    val ssid = wifiInfo.ssid
                    // SSID comes with quotes, remove them
                    return ssid.removeSurrounding("\"")
                } catch (e: SecurityException) {
                    Timber.w("Need location permission to get SSID: ${e.message}")
                    return null
                }
            }
            null
        } else {
            // Below Android 11
            val wifiInfo = wifiManager.connectionInfo
            val ssid = wifiInfo.ssid
            // SSID comes with quotes, remove them
            ssid.removeSurrounding("\"")
        }
    }

    /**
     * Check if device is connected to an AquaLevel device's access point
     */
    fun isConnectedToAquaLevelAP(): Boolean {
        val ssid = getCurrentWifiSsid() ?: return false
        return ssid.startsWith("AquaLevel-", ignoreCase = true)
    }

    /**
     * Open WiFi settings
     */
    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Get AquaLevel device name from AP SSID
     */
    fun getDeviceNameFromApSsid(ssid: String): String? {
        val pattern = Pattern.compile("AquaLevel-(.+)-Setup", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(ssid)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    /**
     * Monitor network changes
     */
    fun networkChanges(): Flow<Boolean> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Initially send current state
        trySend(isConnectedToWifi())

        // Clean up when Flow collection ends
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    /**
     * Get local IP address
     */
    fun getLocalIpAddress(): String? {
        val wifiInfo = wifiManager.connectionInfo
        val ipInt = wifiInfo.ipAddress

        // Convert little-endian to human-readable format
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    /**
     * Convert AquaLevel device name to standard mDNS hostname
     */
    fun getDeviceHostname(deviceName: String): String {
        var hostname = deviceName.lowercase()
            .replace(" ", "-")
            .replace(".", "-")
            .replace("_", "-")

        // Remove any non-alphanumeric characters except hyphen
        hostname = hostname.filter { it.isLetterOrDigit() || it == '-' }

        // Add aqualevel- prefix if not present
        if (!hostname.startsWith("aqualevel-")) {
            hostname = "aqualevel-$hostname"
        }

        return "$hostname.local"
    }
}