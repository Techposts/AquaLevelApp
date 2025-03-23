package com.aqualevel.di

import android.content.Context
import com.aqualevel.data.DeviceRepository
import com.aqualevel.data.PreferenceManager
import com.aqualevel.data.database.AquaLevelDatabase
import com.aqualevel.data.database.dao.DeviceDao
import com.aqualevel.data.database.dao.WaterLevelHistoryDao
import com.aqualevel.util.MdnsDiscovery
import com.aqualevel.util.NetworkUtils
import com.aqualevel.util.NotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideMdnsDiscovery(@ApplicationContext context: Context): MdnsDiscovery {
        return MdnsDiscovery(context)
    }

    @Provides
    @Singleton
    fun providePreferenceManager(@ApplicationContext context: Context): PreferenceManager {
        return PreferenceManager(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideDeviceRepository(
        @ApplicationContext context: Context,
        deviceDao: DeviceDao,
        waterLevelHistoryDao: WaterLevelHistoryDao,
        deviceApi: com.aqualevel.api.DeviceApi,
        mdnsDiscovery: MdnsDiscovery,
        networkUtils: NetworkUtils,
        preferenceManager: PreferenceManager
    ): DeviceRepository {
        return DeviceRepository(
            context,
            deviceDao,
            waterLevelHistoryDao,
            deviceApi,
            mdnsDiscovery,
            networkUtils,
            preferenceManager
        )
    }
}