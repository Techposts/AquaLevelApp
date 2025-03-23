package com.aqualevel.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aqualevel.data.database.dao.DeviceDao
import com.aqualevel.data.database.dao.WaterLevelHistoryDao
import com.aqualevel.data.database.entity.Device
import com.aqualevel.data.database.entity.WaterLevelHistory
import com.aqualevel.data.database.util.DateConverter

@Database(
    entities = [Device::class, WaterLevelHistory::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AquaLevelDatabase : RoomDatabase() {

    abstract fun deviceDao(): DeviceDao
    abstract fun waterLevelHistoryDao(): WaterLevelHistoryDao

    companion object {
        private const val DATABASE_NAME = "aqualevel_db"

        @Volatile
        private var INSTANCE: AquaLevelDatabase? = null

        fun getInstance(context: Context): AquaLevelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AquaLevelDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // For simplicity; in production, use proper migrations
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}