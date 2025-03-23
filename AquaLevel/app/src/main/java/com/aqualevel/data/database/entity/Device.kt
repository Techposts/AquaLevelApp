package com.aqualevel.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing a saved AquaLevel device
 */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val id: String,                   // Device's hostname (e.g., "aqualevel-kitchen.local")
    val name: String,                 // User-friendly name (e.g., "Kitchen Water Tank")
    val ipAddress: String,            // IP address of the device
    val lastSeen: Date,               // When the device was last detected
    val lastPercentage: Float? = null, // Last known water level percentage
    val lastVolume: Float? = null,     // Last known water volume
    val tankHeight: Float? = null,     // Tank height in cm
    val tankDiameter: Float? = null,   // Tank diameter in cm
    val tankVolume: Float? = null,     // Tank volume in liters
    val isFavorite: Boolean = false,   // Whether this device is marked as favorite
    val alertLevelLow: Int? = null,    // Low water alert level
    val alertLevelHigh: Int? = null,   // High water alert level
    val alertsEnabled: Boolean = true  // Whether alerts are enabled for this device
)