package com.aqualevel.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aqualevel.MainActivity
import com.aqualevel.R
import com.aqualevel.data.database.entity.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing notifications
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID_ALERTS = "aqualevel_alerts"
        private const val CHANNEL_ID_UPDATES = "aqualevel_updates"

        private const val NOTIFICATION_ID_LOW_WATER = 1
        private const val NOTIFICATION_ID_HIGH_WATER = 2
        private const val NOTIFICATION_ID_OFFLINE = 3
    }

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Alert channel (high priority)
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alerts_description)
                enableVibration(true)
                enableLights(true)
            }

            // Updates channel (default priority)
            val updatesChannel = NotificationChannel(
                CHANNEL_ID_UPDATES,
                context.getString(R.string.notification_channel_updates),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_updates_description)
            }

            // Register channels
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(alertChannel, updatesChannel))
        }
    }

    /**
     * Show low water level notification
     */
    fun showLowWaterNotification(device: Device, percentage: Float) {
        // Create intent to open app to the device's detail screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("deviceId", device.id)
            putExtra("openDeviceDetail", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle(context.getString(R.string.low_water_alert_title))
            .setContentText(
                context.getString(
                    R.string.low_water_alert_message,
                    device.name,
                    percentage.toInt()
                )
            )
            .setSmallIcon(R.drawable.ic_notification_low_water)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_LOW_WATER + device.id.hashCode(), notification)
        }
    }

    /**
     * Show high water level notification
     */
    fun showHighWaterNotification(device: Device, percentage: Float) {
        // Create intent to open app to the device's detail screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("deviceId", device.id)
            putExtra("openDeviceDetail", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setContentTitle(context.getString(R.string.high_water_alert_title))
            .setContentText(
                context.getString(
                    R.string.high_water_alert_message,
                    device.name,
                    percentage.toInt()
                )
            )
            .setSmallIcon(R.drawable.ic_notification_high_water)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_HIGH_WATER + device.id.hashCode(), notification)
        }
    }

    /**
     * Show device offline notification
     */
    fun showDeviceOfflineNotification(device: Device) {
        // Create intent to open app to the device's detail screen
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("deviceId", device.id)
            putExtra("openDeviceDetail", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_UPDATES)
            .setContentTitle(context.getString(R.string.device_offline_title))
            .setContentText(
                context.getString(R.string.device_offline_message, device.name)
            )
            .setSmallIcon(R.drawable.ic_notification_offline)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show notification
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_OFFLINE + device.id.hashCode(), notification)
        }
    }

    /**
     * Clear all notifications for a device
     */
    fun clearNotificationsForDevice(deviceId: String) {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_LOW_WATER + deviceId.hashCode())
            cancel(NOTIFICATION_ID_HIGH_WATER + deviceId.hashCode())
            cancel(NOTIFICATION_ID_OFFLINE + deviceId.hashCode())
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }
}