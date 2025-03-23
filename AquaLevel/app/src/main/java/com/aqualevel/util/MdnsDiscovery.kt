package com.aqualevel.util

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class for discovered AquaLevel devices
 */
data class DiscoveredDevice(
    val serviceName: String,
    val hostname: String,
    val ipAddress: String,
    val port: Int
)

/**
 * Service for discovering AquaLevel devices on the local network using mDNS
 */
@Singleton
class MdnsDiscovery @Inject constructor(private val context: Context) {

    companion object {
        private const val SERVICE_TYPE = "_http._tcp."
    }

    private val nsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    /**
     * Discover AquaLevel devices on the network
     */
    fun discoverDevices(): Flow<DiscoveredDevice> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("Service discovery started: $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.d("Service found: ${serviceInfo.serviceName}")

                // Check if it's likely an AquaLevel device
                // (serviceName typically starts with the device hostname)
                if (serviceInfo.serviceName.contains("aqualevel", ignoreCase = true)) {
                    Timber.d("Found potential AquaLevel device: ${serviceInfo.serviceName}")
                    nsdManager.resolveService(serviceInfo, createResolveListener(this@callbackFlow))
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("Service lost: ${serviceInfo.serviceName}")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("Service discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Start discovery failed: $serviceType, error: $errorCode")
                close(Exception("Failed to start service discovery, error code: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.e("Stop discovery failed: $serviceType, error: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start discovery")
            close(e)
        }

        // Clean up when Flow collection ends
        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Timber.e(e, "Failed to stop discovery")
            }
        }
    }

    /**
     * Create a resolve listener for service discovery
     */
    private fun createResolveListener(
        channel: kotlinx.coroutines.channels.ProducerScope<DiscoveredDevice>
    ): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.e("Resolve failed: ${serviceInfo.serviceName}, error: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Timber.d("Service resolved: ${serviceInfo.serviceName}")

                val host = serviceInfo.host.hostAddress ?: return
                val port = serviceInfo.port
                val serviceName = serviceInfo.serviceName

                // Extract hostname from serviceName or construct a default one
                val hostname = if (serviceName.endsWith(".local")) {
                    serviceName
                } else {
                    "aqualevel-${serviceName.lowercase().replace("[^a-z0-9]".toRegex(), "-")}.local"
                }

                val device = DiscoveredDevice(
                    serviceName = serviceName,
                    hostname = hostname,
                    ipAddress = host,
                    port = port
                )

                Timber.d("Sending discovered device: $device")
                channel.trySend(device)
            }
        }
    }

    /**
     * Discover a specific AquaLevel device by hostname
     * Returns null if device is not found after timeout
     */
    suspend fun discoverDeviceByHostname(hostname: String, timeoutMs: Long = 30000): DiscoveredDevice? {
        var discoveredDevice: DiscoveredDevice? = null

        try {
            // Start discovery and wait for specific device or timeout
            kotlinx.coroutines.withTimeout(timeoutMs) {
                discoverDevices().collect { device ->
                    if (device.hostname.equals(hostname, ignoreCase = true) ||
                        device.serviceName.equals(hostname, ignoreCase = true)) {
                        discoveredDevice = device
                        throw kotlinx.coroutines.CancellationException("Device found")
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Timber.d("Discovery timed out for device: $hostname")
        } catch (e: kotlinx.coroutines.CancellationException) {
            Timber.d("Device found: $hostname")
        } catch (e: Exception) {
            Timber.e(e, "Error during discovery")
        }

        return discoveredDevice
    }
}