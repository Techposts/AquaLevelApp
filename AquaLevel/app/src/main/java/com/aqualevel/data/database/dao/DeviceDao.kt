package com.aqualevel.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.aqualevel.data.database.entity.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    fun getDeviceById(deviceId: String): Flow<Device?>

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    suspend fun getDeviceByIdSuspend(deviceId: String): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("UPDATE devices SET name = :name WHERE id = :deviceId")
    suspend fun updateDeviceName(deviceId: String, name: String)

    @Query("UPDATE devices SET isFavorite = :isFavorite WHERE id = :deviceId")
    suspend fun updateFavoriteStatus(deviceId: String, isFavorite: Boolean)

    @Query("UPDATE devices SET lastPercentage = :percentage, lastVolume = :volume, lastSeen = :lastSeen WHERE id = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, percentage: Float, volume: Float, lastSeen: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM devices WHERE id = :deviceId LIMIT 1)")
    suspend fun deviceExists(deviceId: String): Boolean
}