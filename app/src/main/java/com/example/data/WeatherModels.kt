package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    @Json(name = "current") val current: CurrentForecast?,
    @Json(name = "hourly") val hourly: HourlyForecast?,
    @Json(name = "daily") val daily: DailyForecast?
)

@JsonClass(generateAdapter = true)
data class CurrentForecast(
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "relative_humidity_2m") val humidity: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "precipitation") val precipitation: Double,
    @Json(name = "rain") val rain: Double,
    @Json(name = "showers") val showers: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double
)

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    @Json(name = "time") val times: List<String>,
    @Json(name = "temperature_2m") val temperatures: List<Double>,
    @Json(name = "weather_code") val weatherCodes: List<Int>,
    @Json(name = "pm2_5") val pm25: List<Double>?,
    @Json(name = "pm10") val pm10: List<Double>?
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    @Json(name = "time") val times: List<String>,
    @Json(name = "weather_code") val weatherCodes: List<Int>,
    @Json(name = "temperature_2m_max") val maxTemps: List<Double>,
    @Json(name = "temperature_2m_min") val minTemps: List<Double>
)

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    @Json(name = "current") val current: CurrentAirQuality?
)

@JsonClass(generateAdapter = true)
data class CurrentAirQuality(
    @Json(name = "pm2_5") val pm25: Double,
    @Json(name = "pm10") val pm10: Double,
    @Json(name = "carbon_monoxide") val carbonMonoxide: Double?,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double?,
    @Json(name = "sulphur_dioxide") val sulphurDioxide: Double?,
    @Json(name = "ozone") val ozone: Double?,
    @Json(name = "us_aqi") val usAqi: Double
)

// Combined high-fidelity weather state for the app's internal logic
data class DhakaWeatherState(
    val temperature: Double = 28.5,
    val humidity: Double = 82.0,
    val apparentTemperature: Double = 32.0,
    val precipitation: Double = 0.0,
    val rain: Double = 0.0,
    val showers: Double = 0.0,
    val weatherCode: Int = 3, // Overcast
    val windSpeed: Double = 8.5,
    val pm25: Double = 24.5,
    val pm10: Double = 42.0,
    val co: Double = 120.0,
    val no2: Double = 18.0,
    val so2: Double = 4.0,
    val o3: Double = 22.0,
    val usAqi: Int = 45,
    val timestamp: Long = System.currentTimeMillis()
)
