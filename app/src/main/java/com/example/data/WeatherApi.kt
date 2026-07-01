package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("https://api.open-meteo.com/v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double = 23.8103,
        @Query("longitude") longitude: Double = 90.4125,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,rain,showers,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "Asia/Dhaka"
    ): ForecastResponse

    @GET("https://air-quality-api.open-meteo.com/v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double = 23.8103,
        @Query("longitude") longitude: Double = 90.4125,
        @Query("current") current: String = "pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,us_aqi",
        @Query("timezone") timezone: String = "Asia/Dhaka"
    ): AirQualityResponse
}
