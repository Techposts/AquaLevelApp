package com.aqualevel

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.aqualevel.data.DeviceRepository
import com.aqualevel.data.PreferenceManager
import com.aqualevel.util.BackgroundUpdateObserver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AquaLevelApplication : Application() {

    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var backgroundUpdateObserver: BackgroundUpdateObserver

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize background updates
        setupBackgroundUpdates()

        // Reconnect to last used device if any
        reconnectToLastDevice()
    }

    private fun setupBackgroundUpdates() {
        // Create and register the process lifecycle observer for background updates
        backgroundUpdateObserver = BackgroundUpdateObserver(
            deviceRepository = deviceRepository,
            preferenceManager = preferenceManager
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(backgroundUpdateObserver)
    }

    private fun reconnectToLastDevice() {
        applicationScope.launch {
            try {
                deviceRepository.reconnectToLastDevice()
            } catch (e: Exception) {
                Timber.e(e, "Error reconnecting to last device")
            }
        }
    }
}