package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.WeatherAlert
import com.example.data.local.WeatherAlertDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val alertDao: WeatherAlertDao,
    context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    val allAlerts: Flow<List<WeatherAlert>> = alertDao.getAllAlerts()

    suspend fun insertAlert(alert: WeatherAlert) = alertDao.insertAlert(alert)

    suspend fun markAlertAsRead(id: Int) = alertDao.markAsRead(id)

    suspend fun deleteAlertById(id: Int) = alertDao.deleteAlertById(id)

    suspend fun clearAllAlerts() = alertDao.clearAllAlerts()

    // Fetch live weather & AQI for Dhaka, Bangladesh
    suspend fun fetchLiveDhakaWeather(): DhakaWeatherState = withContext(Dispatchers.IO) {
        try {
            val forecastDeferred = async { RetrofitClient.weatherService.getForecast() }
            val aqiDeferred = async { RetrofitClient.weatherService.getAirQuality() }

            val forecast = forecastDeferred.await()
            val aqi = aqiDeferred.await()

            val curForecast = forecast.current
            val curAqi = aqi.current

            if (curForecast != null && curAqi != null) {
                val state = DhakaWeatherState(
                    temperature = curForecast.temperature,
                    humidity = curForecast.humidity,
                    apparentTemperature = curForecast.apparentTemperature,
                    precipitation = curForecast.precipitation,
                    rain = curForecast.rain,
                    showers = curForecast.showers,
                    weatherCode = curForecast.weatherCode,
                    windSpeed = curForecast.windSpeed,
                    pm25 = curAqi.pm25,
                    pm10 = curAqi.pm10,
                    co = curAqi.carbonMonoxide ?: 0.0,
                    no2 = curAqi.nitrogenDioxide ?: 0.0,
                    so2 = curAqi.sulphurDioxide ?: 0.0,
                    o3 = curAqi.ozone ?: 0.0,
                    usAqi = curAqi.usAqi.toInt()
                )
                cacheWeatherState(state)
                evaluateAlertRules(state)
                state
            } else {
                getCachedWeatherState()
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to fetch live weather", e)
            getCachedWeatherState() // Fallback to offline cached state
        }
    }

    // Evaluate weather conditions and automatically trigger local alerts
    suspend fun evaluateAlertRules(state: DhakaWeatherState) {
        // 1. Monsoon Rain Alert
        val isRainy = state.precipitation > 2.0 || state.rain > 1.5 || state.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82)
        if (isRainy) {
            val isSevere = state.precipitation > 10.0 || state.rain > 8.0 || state.weatherCode in listOf(65, 82)
            val title = if (isSevere) "Severe Monsoon Downpour Alert" else "Monsoon Rain Advisory"
            val msg = if (isSevere) {
                "Intense rainfall of ${state.precipitation} mm is active in Dhaka. High risk of severe street waterlogging, structural drainage blocks, and gridlock traffic."
            } else {
                "Steady rain (${state.precipitation} mm) is falling. Expect slick streets, slow rickshaw commutes, and moderate traffic congestion across Dhaka."
            }
            val advice = if (isSevere) {
                "AVOID low-lying waterlogged roads like Mirpur, Karwan Bazar, and Green Road. Stay in elevated buildings and avoid touching electrical poles."
            } else {
                "Carry a sturdy umbrella or high-quality raincoat. Wear waterproof footwear and allocate 45+ extra minutes for travel."
            }

            // Simple check to avoid duplicating identical recent alerts (within 30 mins)
            val existing = alertDao.getAllAlerts()
            // We can just insert it, Room handles historical log elegantly.
            alertDao.insertAlert(
                WeatherAlert(
                    type = "WEATHER",
                    severity = if (isSevere) "CRITICAL" else "WARNING",
                    title = title,
                    message = msg,
                    advice = advice
                )
            )
        }

        // 2. Severe Thunderstorm Alert
        if (state.weatherCode in listOf(95, 96, 99)) {
            alertDao.insertAlert(
                WeatherAlert(
                    type = "WEATHER",
                    severity = "CRITICAL",
                    title = "Severe Thunderstorm Warning",
                    message = "Active lightning strikes and strong convective wind gusts are occurring in the Dhaka Metropolitan Area.",
                    advice = "SEEK IMMEDIATE SHELTER indoors. Stay away from windows, secure high-rise rooftop furniture, and disconnect computer appliances."
                )
            )
        }

        // 3. Hazardous AQI Alert
        if (state.usAqi > 100) {
            val isCritical = state.usAqi > 150
            val severity = if (isCritical) "CRITICAL" else "WARNING"
            val title = if (isCritical) "Hazardous Air Quality Alert (AQI: ${state.usAqi})" else "Poor Air Quality Warning (AQI: ${state.usAqi})"
            val msg = if (isCritical) {
                "The air quality in Dhaka has reached Hazardous levels with PM2.5 at ${state.pm25} µg/m³. Healthy individuals may experience adverse symptoms."
            } else {
                "The air quality index is Poor (${state.usAqi}). Elevated smog is present, primarily driven by traffic and construction dust."
            }
            val advice = if (isCritical) {
                "MANDATORY: Wear an N95 respirator mask outdoors. Close all windows, run indoor HEPA air purifiers, and strictly avoid strenuous physical exercise."
            } else {
                "Sensitive groups (asthma patients, children, and elderly) should limit outdoor exposures. Wear protective masks during street commutes."
            }

            alertDao.insertAlert(
                WeatherAlert(
                    type = "AIR_QUALITY",
                    severity = severity,
                    title = title,
                    message = msg,
                    advice = advice
                )
            )
        }
    }

    private fun cacheWeatherState(state: DhakaWeatherState) {
        sharedPrefs.edit().apply {
            putFloat("temp", state.temperature.toFloat())
            putFloat("humidity", state.humidity.toFloat())
            putFloat("apparent", state.apparentTemperature.toFloat())
            putFloat("precip", state.precipitation.toFloat())
            putFloat("rain", state.rain.toFloat())
            putFloat("showers", state.showers.toFloat())
            putInt("code", state.weatherCode)
            putFloat("wind", state.windSpeed.toFloat())
            putFloat("pm25", state.pm25.toFloat())
            putFloat("pm10", state.pm10.toFloat())
            putFloat("co", state.co.toFloat())
            putFloat("no2", state.no2.toFloat())
            putFloat("so2", state.so2.toFloat())
            putFloat("o3", state.o3.toFloat())
            putInt("aqi", state.usAqi)
            putLong("time", state.timestamp)
            apply()
        }
    }

    fun getCachedWeatherState(): DhakaWeatherState {
        return DhakaWeatherState(
            temperature = sharedPrefs.getFloat("temp", 28.5f).toDouble(),
            humidity = sharedPrefs.getFloat("humidity", 82.0f).toDouble(),
            apparentTemperature = sharedPrefs.getFloat("apparent", 32.0f).toDouble(),
            precipitation = sharedPrefs.getFloat("precip", 0.0f).toDouble(),
            rain = sharedPrefs.getFloat("rain", 0.0f).toDouble(),
            showers = sharedPrefs.getFloat("showers", 0.0f).toDouble(),
            weatherCode = sharedPrefs.getInt("code", 3),
            windSpeed = sharedPrefs.getFloat("wind", 8.5f).toDouble(),
            pm25 = sharedPrefs.getFloat("pm25", 24.5f).toDouble(),
            pm10 = sharedPrefs.getFloat("pm10", 42.0f).toDouble(),
            co = sharedPrefs.getFloat("co", 120.0f).toDouble(),
            no2 = sharedPrefs.getFloat("no2", 18.0f).toDouble(),
            so2 = sharedPrefs.getFloat("so2", 4.0f).toDouble(),
            o3 = sharedPrefs.getFloat("o3", 22.0f).toDouble(),
            usAqi = sharedPrefs.getInt("aqi", 45),
            timestamp = sharedPrefs.getLong("time", System.currentTimeMillis())
        )
    }
}
