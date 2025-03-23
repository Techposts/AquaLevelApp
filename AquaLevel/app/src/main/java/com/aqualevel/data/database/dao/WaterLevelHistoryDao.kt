package com.aqualevel.data.database.dao

import androidx.room.*
import com.aqualevel.data.database.entity.WaterLevelHistory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WaterLevelHistoryDao {
    @Query("SELECT * FROM water_level_history WHERE deviceId = :deviceId ORDER BY timestamp DESC")
    fun getHistoryForDevice(deviceId: String): Flow<List<WaterLevelHistory>>

    @Query("SELECT * FROM water_level_history WHERE deviceId = :deviceId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getHistoryForDateRange(deviceId: String, startDate: Date, endDate: Date): Flow<List<WaterLevelHistory>>

    @Query("SELECT * FROM water_level_history WHERE deviceId = :deviceId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReading(deviceId: String): WaterLevelHistory?

    @Insert
    suspend fun insertReading(reading: WaterLevelHistory)

    @Insert
    suspend fun insertReadings(readings: List<WaterLevelHistory>)

    @Query("DELETE FROM water_level_history WHERE deviceId = :deviceId AND timestamp < :olderThan")
    suspend fun deleteOldReadings(deviceId: String, olderThan: Date)

    @Query("SELECT AVG(percentage) FROM water_level_history WHERE deviceId = :deviceId AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getAveragePercentage(deviceId: String, startDate: Date, endDate: Date): Float?

    @Query("SELECT MIN(percentage) FROM water_level_history WHERE deviceId = :deviceId AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getMinPercentage(deviceId: String, startDate: Date, endDate: Date): Float?

    @Query("SELECT MAX(percentage) FROM water_level_history WHERE deviceId = :deviceId AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getMaxPercentage(deviceId: String, startDate: Date, endDate: Date): Float?
}