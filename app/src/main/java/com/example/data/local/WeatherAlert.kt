package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_alerts")
data class WeatherAlert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "WEATHER" (storm/rain/wind) or "AIR_QUALITY" (AQI levels)
    val severity: String, // "INFO", "WARNING", "CRITICAL"
    val title: String,
    val message: String,
    val advice: String, // safety/health recommendations
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
