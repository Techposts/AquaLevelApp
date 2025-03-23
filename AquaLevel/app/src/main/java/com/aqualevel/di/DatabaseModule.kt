package com.aqualevel.di

import android.content.Context
import com.aqualevel.data.database.AquaLevelDatabase
import com.aqualevel.data.database.dao.DeviceDao
import com.aqualevel.data.database.dao.WaterLevelHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AquaLevelDatabase {
        return AquaLevelDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDeviceDao(database: AquaLevelDatabase): DeviceDao {
        return database.deviceDao()
    }

    @Provides
    @Singleton
    fun provideWaterLevelHistoryDao(database: AquaLevelDatabase): WaterLevelHistoryDao {
        return database.waterLevelHistoryDao()
    }
}