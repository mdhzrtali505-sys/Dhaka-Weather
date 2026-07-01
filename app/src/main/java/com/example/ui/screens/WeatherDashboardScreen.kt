package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DhakaWeatherState
import com.example.data.local.WeatherAlert
import com.example.ui.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val activeSimName by viewModel.activeSimulationName.collectAsStateWithLifecycle()
    val aiAdvisory by viewModel.aiAdvisory.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val rainCountdown by viewModel.rainCountdown.collectAsStateWithLifecycle()
    val alertHistory by viewModel.alertHistory.collectAsStateWithLifecycle()

    var showHistoryDialog by remember { mutableStateOf(false) }

    // Dynamic theme background gradient based on the active weather state
    val backgroundBrush = when {
        isSimulating && activeSimName == "Severe Monsoon Rain" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF020617))
        )
        isSimulating && activeSimName == "Severe Thunderstorm" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF172554), Color(0xFF0F172A), Color(0xFF020617))
        )
        isSimulating && activeSimName == "Extreme Winter Smog" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
        )
        isSimulating && activeSimName == "Extreme Heatwave" -> Brush.verticalGradient(
            colors = listOf(Color(0xFF7C2D12), Color(0xFF451A03), Color(0xFF0C0A09))
        )
        weatherState.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1E2640), Color(0xFF111827))
        )
        weatherState.usAqi > 150 -> Brush.verticalGradient(
            colors = listOf(Color(0xFF331B3F), Color(0xFF111827))
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0B131E), Color(0xFF162535), Color(0xFF080D14))
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DHAKA SKY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Weather & AQI Monitor",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.LightGray.copy(alpha = 0.8f)
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshWeather() },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh weather",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 1. Simulation Indicator Bar
                if (isSimulating) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3B82F6))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Simulation: $activeSimName",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "STOP",
                                    color = Color(0xFF60A5FA),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clickable { viewModel.stopSimulation() }
                                        .padding(4.dp)
                                        .testTag("stop_simulation_button")
                                )
                            }
                        }
                    }
                }

                // 2. Active Hazards Alert Banner
                val activeAlert = alertHistory.firstOrNull { !it.isRead }
                if (activeAlert != null) {
                    item {
                        val alertColor = if (activeAlert.severity == "CRITICAL") Color(0xFFEF4444) else Color(0xFFF59E0B)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, alertColor, RoundedCornerShape(16.dp))
                                .testTag("active_hazard_banner")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Active Warning",
                                        tint = alertColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = activeAlert.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = alertColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeAlert.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "SAFETY: ${activeAlert.advice}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { viewModel.markAlertAsRead(activeAlert.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = alertColor)
                                    ) {
                                        Text("Dismiss Alert")
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Current Weather Status Card
                item {
                    WeatherSummaryCard(
                        state = weatherState,
                        desc = viewModel.getWeatherDescription(weatherState.weatherCode),
                        isLoading = isLoading
                    )
                }

                // 4. Storm Rain / Countdown Clock
                if (rainCountdown != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Storm warning countdown",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "STORM & PRECIPITATION WATCH",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF60A5FA),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = rainCountdown ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Air Quality Index (AQI) Alerts Card
                item {
                    AirQualityCard(state = weatherState)
                }

                // 6. AI Weather Advisor Card
                item {
                    AIAdvisorCard(
                        aiAdvisory = aiAdvisory,
                        isAiLoading = isAiLoading,
                        onAskAi = { viewModel.askGeminiAdvisory() }
                    )
                }

                // 7. Simulation Control Panel (Control Deck)
                item {
                    SimulationDeck(
                        onTrigger = { type -> viewModel.triggerSimulation(type) }
                    )
                }

                // 8. Alerts Logs Navigation Bar
                item {
                    Button(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("view_history_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Alert history log",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Active & Past Alerts (${alertHistory.size})",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Overlay modal showing the Room-persisted Alert Logs History
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                if (alertHistory.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearAlertHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Clear Logs")
                    }
                }
            },
            title = {
                Text(
                    text = "Alerts Log History",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (alertHistory.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "No alerts",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All Clear in Dhaka!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Use simulation controls to generate and log alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(alertHistory, key = { it.id }) { alert ->
                                AlertLogItem(
                                    alert = alert,
                                    onRead = { viewModel.markAlertAsRead(alert.id) },
                                    onDelete = { viewModel.deleteAlert(alert.id) }
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun WeatherSummaryCard(
    state: DhakaWeatherState,
    desc: String,
    isLoading: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Dhaka Location",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Dhaka, BD",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(state.timestamp))
                    Text(
                        text = "Updated $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // Custom condition tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = desc,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${state.temperature.toInt()}°C",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Feels like ${state.apparentTemperature.toInt()}°C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }

                // Stylized Custom Weather Icon Drawer (Avoids asset dependency issues)
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    when {
                        state.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> {
                            // Rain cloud icon representation
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Rain Icon",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        state.weatherCode in listOf(95, 96, 99) -> {
                            // Thunderstorm icon
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Storm Icon",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        state.weatherCode in listOf(45, 48) -> {
                            // Fog/Smog
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = "Fog Icon",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        else -> {
                            // Sun / Sunny
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Sunny Icon",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(16.dp))

            // Grid of subsidiary parameters
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("HUMIDITY", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${state.humidity.toInt()}%", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("WIND SPEED", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${state.windSpeed} km/h", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRECIPITATION", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${state.precipitation} mm", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AirQualityCard(state: DhakaWeatherState) {
    val aqi = state.usAqi
    val (aqiDesc, aqiColor, healthTips) = when {
        aqi <= 50 -> Triple("Good", Color(0xFF10B981), "Air quality is satisfactory. Safe for all commutes.")
        aqi <= 100 -> Triple("Moderate", Color(0xFFF59E0B), "Air quality is acceptable. Extremely sensitive persons should consider masks near heavy dust.")
        aqi <= 150 -> Triple("Unhealthy for Sensitive Groups", Color(0xFFF97316), "Sensitive groups (asthma/elderly) should limit long stays on high-dust roads.")
        aqi <= 200 -> Triple("Unhealthy", Color(0xFFEF4444), "Active particulate smog. Wear protective masks when traveling Dhaka streets.")
        else -> Triple("Hazardous", Color(0xFF7F1D1D), "Severe Smog/Dust blanket! MANDATORY: Use N95 respiratory masks and stay indoors if possible.")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AIR QUALITY INDEX",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$aqi - $aqiDesc",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = aqiColor
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(aqiColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = "Air Quality",
                        tint = aqiColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual AQI indicator slider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                // Approximate slider fill position
                val progress = (aqi.toFloat() / 300f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(aqiColor)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "COMMUTING TIP:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = healthTips,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            // Pollutants breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PollutantLabel(name = "PM2.5", value = "${state.pm25.toInt()} µg")
                PollutantLabel(name = "PM10", value = "${state.pm10.toInt()} µg")
                PollutantLabel(name = "CO", value = "${state.co.toInt()} ppm")
                PollutantLabel(name = "NO₂", value = "${state.no2.toInt()} ppb")
            }
        }
    }
}

@Composable
fun PollutantLabel(name: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AIAdvisorCard(
    aiAdvisory: String?,
    isAiLoading: Boolean,
    onAskAi: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5))),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF818CF8).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI Insights",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Dhaka Sky AI Advisor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (aiAdvisory == null && !isAiLoading) {
                    Button(
                        onClick = onAskAi,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("ai_advisor_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text("Get Insights", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isAiLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF818CF8),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Consulting Dhaka Sky AI...",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                aiAdvisory != null -> {
                    Column {
                        Text(
                            text = aiAdvisory,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = onAskAi,
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF818CF8))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate insights", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Recalculate Advisor Insights", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Generate smart recommendations for traffic congestion, waterlogging, rickshaw safety, and mask requirements based on the current conditions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SimulationDeck(onTrigger: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "SAFETY SIMULATION DECK",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trigger intense hazards to test emergency alert notifications and countdown watches locally.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SimulationButton(
                        label = "Monsoon Heavy Rain",
                        color = Color(0xFF3B82F6),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulation_monsoon_button"),
                        onClick = { onTrigger("Severe Monsoon Rain") }
                    )
                    SimulationButton(
                        label = "Severe Thunderstorm",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulation_storm_button"),
                        onClick = { onTrigger("Severe Thunderstorm") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SimulationButton(
                        label = "Winter Smog Hazard",
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulation_smog_button"),
                        onClick = { onTrigger("Extreme Winter Smog") }
                    )
                    SimulationButton(
                        label = "Extreme Heatwave",
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulation_heatwave_button"),
                        onClick = { onTrigger("Extreme Heatwave") }
                    )
                }
                SimulationButton(
                    label = "Reset to Clear Skies",
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulation_clear_button"),
                    onClick = { onTrigger("Clear Sunny Day") }
                )
            }
        }
    }
}

@Composable
fun SimulationButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.15f), contentColor = color),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun AlertLogItem(
    alert: WeatherAlert,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    val severityColor = when (alert.severity) {
        "CRITICAL" -> Color(0xFFEF4444)
        "WARNING" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isRead) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (alert.isRead) 0.dp else 1.dp,
                color = severityColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (alert.isRead) Color.Transparent else severityColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.title,
                        fontWeight = FontWeight.Bold,
                        color = if (alert.isRead) Color.LightGray else Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.75f)
                    )
                }

                Row {
                    if (!alert.isRead) {
                        IconButton(
                            onClick = onRead,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Mark as read",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete alert",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = alert.message,
                color = Color.LightGray.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Safe Practice: ${alert.advice}",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))
            val alertTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(alert.timestamp))
            Text(
                text = "Triggered at $alertTime",
                color = Color.Gray,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
