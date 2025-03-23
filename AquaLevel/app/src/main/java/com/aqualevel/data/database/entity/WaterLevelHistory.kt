package com.aqualevel.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity representing historical water level readings
 */
@Entity(
    tableName = "water_level_history",
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deviceId")]
)
data class WaterLevelHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String,         // Reference to the device
    val timestamp: Date,          // When the reading was taken
    val percentage: Float,        // Water level percentage
    val volume: Float,            // Water volume in liters
    val distance: Float,          // Distance reading from sensor
    val waterLevel: Float         // Water level in cm
)