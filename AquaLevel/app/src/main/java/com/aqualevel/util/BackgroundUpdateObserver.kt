package com.aqualevel.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.aqualevel.data.DeviceRepository
import com.aqualevel.data.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class BackgroundUpdateObserver(
    private val deviceRepository: DeviceRepository,
    private val preferenceManager: PreferenceManager
) : DefaultLifecycleObserver {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var updateJob: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        Timber.d("App moved to foreground - stopping background updates")
        stopBackgroundUpdates()
    }

    override fun onStop(owner: LifecycleOwner) {
        Timber.d("App moved to background - starting background updates")
        startBackgroundUpdates()
    }

    private fun startBackgroundUpdates() {
        // Cancel any existing job first
        stopBackgroundUpdates()

        updateJob = coroutineScope.launch {
            try {
                // Get update interval from preferences (default: 15 minutes)
                val updateIntervalMinutes = preferenceManager.refreshInterval.first() / 60
                val intervalMs = updateIntervalMinutes * 60 * 1000L

                Timber.d("Starting background updates with interval: $updateIntervalMinutes minutes")

                while (isActive) {
                    try {
                        // Update data for all devices
                        val devices = deviceRepository.getAllDevices().first()

                        for (device in devices) {
                            try {
                                if (deviceRepository.setCurrentDevice(device.id)) {
                                    deviceRepository.getTankData()
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Error updating device in background: ${device.id}")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error in background update")
                    }

                    // Wait for next update
                    delay(intervalMs)
                }
            } catch (e: Exception) {
                Timber.e(e, "Background update job failed")
            }
        }
    }

    private fun stopBackgroundUpdates() {
        updateJob?.cancel()
        updateJob = null
    }
}