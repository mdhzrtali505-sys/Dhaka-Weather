package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Double? = null,
    @Json(name = "topP") val topP: Double? = null,
    @Json(name = "topK") val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiService {
    suspend fun getDhakaWeatherInsights(
        temp: Double,
        humidity: Double,
        windSpeed: Double,
        aqi: Int,
        weatherDesc: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI Advisor is currently unavailable because the Gemini API key is not configured in the Secrets panel. Please open the Secrets panel in AI Studio and add your GEMINI_API_KEY to test this feature."
        }

        val prompt = """
            Generate a localized, practical, and highly helpful weather & air quality advisory for commuters and residents in Dhaka, Bangladesh.
            Current Conditions:
            - Temperature: $temp°C
            - Humidity: $humidity%
            - Wind Speed: $windSpeed km/h
            - Air Quality Index (US AQI): $aqi
            - General Sky Condition: $weatherDesc

            Dhaka Specific Insights:
            1. Suggest safety practices specific to Dhaka commutes (e.g., watch out for waterlogging/drainage issues in areas like Mirpur, Dhanmondi, or Motijheel if raining; carry umbrellas; stay hydrated in heat).
            2. Suggest air quality measures (e.g., wear N95 mask if AQI > 100, close windows, filter air).
            3. Highlight specific commute risks (heavy traffic, open construction dust, rickshaw transport comfort).
            4. Keep the advice concise, positive, action-focused, and highly readable with clean bullet points. Keep under 4 brief bullet points total.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(temperature = 0.5),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "You are 'Dhaka Sky AI', an intelligent, polite, localized weather assistant for Dhaka, Bangladesh.")))
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Unable to generate custom recommendations. Please stay safe and check local alerts!"
        } catch (e: Exception) {
            "Unable to connect to Dhaka Sky AI: ${e.localizedMessage}. Please ensure your API key is correctly configured and that your network is active."
        }
    }
}
