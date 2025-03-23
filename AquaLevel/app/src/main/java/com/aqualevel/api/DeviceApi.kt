package com.aqualevel.api

import com.aqualevel.api.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result wrapper for API calls
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val errorMessage: String, val code: Int = 0) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

/**
 * DeviceApi handles communication with AquaLevel devices
 */
@Singleton
class DeviceApi @Inject constructor() {
    private var baseUrl: String? = null
    private var apiService: ApiService? = null

    /**
     * Create a client with increased timeout for potentially slow ESP32 responses
     */
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Set the base URL for API calls (device address)
     */
    fun setDeviceAddress(ipAddress: String) {
        // Ensure URL ends with trailing slash for Retrofit
        baseUrl = if (!ipAddress.startsWith("http://")) {
            "http://$ipAddress/"
        } else if (!ipAddress.endsWith("/")) {
            "$ipAddress/"
        } else {
            ipAddress
        }

        Timber.d("Setting device address to: $baseUrl")

        // Create new Retrofit instance with this base URL
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl!!)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    /**
     * Check if the API is configured with a device address
     */
    fun isConfigured(): Boolean {
        return baseUrl != null && apiService != null
    }

    /**
     * Get the current base URL (device address)
     */
    fun getDeviceAddress(): String? = baseUrl

    /**
     * Helper function to make API calls with error handling
     */
    private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): ApiResult<T> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConfigured()) {
                    return@withContext ApiResult.Error("Device address not configured")
                }

                val response = call()

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        ApiResult.Success(body)
                    } else {
                        ApiResult.Error("Response body is null")
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    ApiResult.Error(errorMsg, response.code())
                }
            } catch (e: SocketTimeoutException) {
                ApiResult.Error("Connection timed out. Make sure the device is powered on and connected to the network.")
            } catch (e: IOException) {
                ApiResult.Error("Network error: ${e.message}")
            } catch (e: Exception) {
                ApiResult.Error("An unexpected error occurred: ${e.message}")
            }
        }
    }

    // API Methods

    suspend fun getTankData(): ApiResult<TankData> {
        return safeApiCall { apiService!!.getTankData() }
    }

    suspend fun getDeviceSettings(): ApiResult<DeviceSettings> {
        return safeApiCall { apiService!!.getDeviceSettings() }
    }

    suspend fun scanNetworks(): ApiResult<List<WiFiNetwork>> {
        return safeApiCall { apiService!!.scanNetworks() }
    }

    suspend fun configureNetwork(request: NetworkSettingsRequest): ApiResult<Unit> {
        return safeApiCall {
            apiService!!.configureNetwork(
                request.ssid,
                request.password,
                request.deviceName
            )
        }
    }

    suspend fun resetWifi(): ApiResult<Unit> {
        return safeApiCall { apiService!!.resetWifi() }
    }

    suspend fun updateTankSettings(request: TankSettingsRequest): ApiResult<Unit> {
        return safeApiCall {
            apiService!!.updateTankSettings(
                request.tankHeight,
                request.tankDiameter,
                request.tankVolume
            )
        }
    }

    suspend fun updateSensorSettings(request: SensorSettingsRequest): ApiResult<Unit> {
        return safeApiCall {
            apiService!!.updateSensorSettings(
                request.sensorOffset,
                request.emptyDistance,
                request.fullDistance,
                request.measurementInterval,
                request.readingSmoothing
            )
        }
    }

    suspend fun updateAlertSettings(request: AlertSettingsRequest): ApiResult<Unit> {
        return safeApiCall {
            apiService!!.updateAlertSettings(
                request.alertLevelLow,
                request.alertLevelHigh,
                request.alertsEnabled
            )
        }
    }

    suspend fun calibrateEmpty(): ApiResult<String> {
        return safeApiCall { apiService!!.calibrateEmpty() }
    }

    suspend fun calibrateFull(): ApiResult<String> {
        return safeApiCall { apiService!!.calibrateFull() }
    }
}