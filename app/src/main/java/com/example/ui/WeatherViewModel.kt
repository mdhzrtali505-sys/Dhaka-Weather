package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DhakaWeatherState
import com.example.data.GeminiService
import com.example.data.WeatherRepository
import com.example.data.local.WeatherAlert
import com.example.data.local.WeatherDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WeatherDatabase.getDatabase(application)
    private val repository = WeatherRepository(database.weatherAlertDao(), application)
    private val geminiService = GeminiService()

    // UI States
    private val _weatherState = MutableStateFlow(DhakaWeatherState())
    val weatherState: StateFlow<DhakaWeatherState> = _weatherState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _activeSimulationName = MutableStateFlow<String?>(null)
    val activeSimulationName: StateFlow<String?> = _activeSimulationName.asStateFlow()

    private val _aiAdvisory = MutableStateFlow<String?>(null)
    val aiAdvisory: StateFlow<String?> = _aiAdvisory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Countdown state representing estimate for incoming rain
    private val _rainCountdown = MutableStateFlow<String?>(null)
    val rainCountdown: StateFlow<String?> = _rainCountdown.asStateFlow()

    // Alert history from Room Database
    val alertHistory: StateFlow<List<WeatherAlert>> = repository.allAlerts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load initially from offline cache, then pull fresh live weather
        _weatherState.value = repository.getCachedWeatherState()
        refreshWeather()
    }

    fun refreshWeather() {
        if (_isSimulating.value) return // Don't disrupt running simulation

        viewModelScope.launch {
            _isLoading.value = true
            val liveState = repository.fetchLiveDhakaWeather()
            _weatherState.value = liveState
            calculateRainCountdown(liveState)
            _isLoading.value = false
        }
    }

    private fun calculateRainCountdown(state: DhakaWeatherState) {
        // Look at current conditions and simulate a countdown to make storm warning countdown "Pawrful"
        if (state.precipitation > 0.0 || state.rain > 0.0) {
            _rainCountdown.value = "Precipitation is active now. Seeking peak intensity..."
        } else if (state.weatherCode in listOf(2, 3)) { // Cloudy or overcast
            _rainCountdown.value = "Incoming storm front detected. Estimate rain onset: ~2.5 hrs"
        } else {
            _rainCountdown.value = null // Clear skies, no precipitation imminent
        }
    }

    // Trigger Weather Event Simulation
    fun triggerSimulation(type: String) {
        viewModelScope.launch {
            _isSimulating.value = true
            _activeSimulationName.value = type
            _aiAdvisory.value = null // reset AI advice so they can fetch for simulation

            val simulatedState = when (type) {
                "Severe Monsoon Rain" -> DhakaWeatherState(
                    temperature = 25.5,
                    humidity = 95.0,
                    apparentTemperature = 28.0,
                    precipitation = 18.4,
                    rain = 15.0,
                    showers = 3.4,
                    weatherCode = 65, // Heavy rain
                    windSpeed = 24.0,
                    pm25 = 12.0,
                    pm10 = 20.0,
                    co = 40.0,
                    no2 = 8.0,
                    so2 = 1.0,
                    o3 = 10.0,
                    usAqi = 25 // rain washes out pollution
                )
                "Severe Thunderstorm" -> DhakaWeatherState(
                    temperature = 24.0,
                    humidity = 98.0,
                    apparentTemperature = 25.0,
                    precipitation = 25.0,
                    rain = 20.0,
                    showers = 5.0,
                    weatherCode = 95, // Thunderstorm
                    windSpeed = 38.0,
                    pm25 = 8.0,
                    pm10 = 15.0,
                    co = 30.0,
                    no2 = 5.0,
                    so2 = 0.5,
                    o3 = 12.0,
                    usAqi = 18
                )
                "Extreme Winter Smog" -> DhakaWeatherState(
                    temperature = 14.5,
                    humidity = 45.0,
                    apparentTemperature = 14.5,
                    precipitation = 0.0,
                    weatherCode = 45, // Foggy / Smoggy
                    windSpeed = 3.5,
                    pm25 = 185.0,
                    pm10 = 265.0,
                    co = 2400.0,
                    no2 = 95.0,
                    so2 = 28.0,
                    o3 = 65.0,
                    usAqi = 235 // Extremely hazardous AQI
                )
                "Extreme Heatwave" -> DhakaWeatherState(
                    temperature = 41.5,
                    humidity = 62.0,
                    apparentTemperature = 49.0,
                    precipitation = 0.0,
                    weatherCode = 0, // Clear Sky
                    windSpeed = 8.0,
                    pm25 = 55.0,
                    pm10 = 90.0,
                    co = 450.0,
                    no2 = 32.0,
                    so2 = 8.0,
                    o3 = 85.0,
                    usAqi = 125 // Unhealthy for Sensitive Groups
                )
                else -> DhakaWeatherState( // Clear standard day
                    temperature = 29.0,
                    humidity = 60.0,
                    apparentTemperature = 31.5,
                    precipitation = 0.0,
                    weatherCode = 1, // Mainly clear
                    windSpeed = 12.0,
                    pm25 = 18.0,
                    pm10 = 35.0,
                    co = 200.0,
                    no2 = 12.0,
                    so2 = 3.0,
                    o3 = 25.0,
                    usAqi = 38
                )
            }

            _weatherState.value = simulatedState

            // Setup rain/storm warning counts
            _rainCountdown.value = when (type) {
                "Severe Monsoon Rain" -> "Heavy monsoon core active. Current rain: 18.4mm/hr"
                "Severe Thunderstorm" -> "Squall line overhead. Lightning frequency: High (~18/min)"
                "Extreme Winter Smog" -> "No rainfall expected. Substantial dry smog trapping particles."
                "Extreme Heatwave" -> "Heat dome active. Pre-monsoon dry spell. 0% chance of precipitation."
                else -> null
            }

            // Immediately run rules to generate local database alerts
            repository.evaluateAlertRules(simulatedState)
        }
    }

    fun stopSimulation() {
        _isSimulating.value = false
        _activeSimulationName.value = null
        _aiAdvisory.value = null
        refreshWeather()
    }

    // Ask Gemini AI for personalized localized safety recommendations based on current weather values
    fun askGeminiAdvisory() {
        val state = _weatherState.value
        val desc = getWeatherDescription(state.weatherCode)

        viewModelScope.launch {
            _isAiLoading.value = true
            _aiAdvisory.value = geminiService.getDhakaWeatherInsights(
                temp = state.temperature,
                humidity = state.humidity,
                windSpeed = state.windSpeed,
                aqi = state.usAqi,
                weatherDesc = desc
            )
            _isAiLoading.value = false
        }
    }

    fun markAlertAsRead(id: Int) {
        viewModelScope.launch {
            repository.markAlertAsRead(id)
        }
    }

    fun deleteAlert(id: Int) {
        viewModelScope.launch {
            repository.deleteAlertById(id)
        }
    }

    fun clearAlertHistory() {
        viewModelScope.launch {
            repository.clearAllAlerts()
        }
    }

    // Helper to get description for codes
    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Skies"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog & Haze"
            51, 53, 55 -> "Drizzle"
            61 -> "Light Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Monsoon Rain"
            80, 81, 82 -> "Rain Showers"
            95 -> "Severe Thunderstorms"
            96, 99 -> "Severe Hail Storm"
            else -> "Intermittent Conditions"
        }
    }
}
