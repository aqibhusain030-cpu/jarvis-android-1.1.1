package com.jarvis.tools.impl

import com.jarvis.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Open-Meteo (no API key, free, opt-in). Returns a short text forecast. */
@Singleton
class WeatherTool @Inject constructor() : Tool {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()

    override val spec = ToolSpec(
        name = "get_weather",
        descriptionHi = "मौसम बताएँ",
        descriptionEn = "Get current weather for a lat/lng",
        parameters = mapOf(
            "lat" to ParamSpec("float", true, "Latitude"),
            "lng" to ParamSpec("float", true, "Longitude")),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 30
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val lat = (args["lat"] as? Number)?.toDouble() ?: return@withContext ToolResult.Err("lat required", "bad_arg")
        val lng = (args["lng"] as? Number)?.toDouble() ?: return@withContext ToolResult.Err("lng required", "bad_arg")
        if (!lat.isFinite() || lat !in -90.0..90.0) return@withContext ToolResult.Err("invalid latitude", "bad_arg")
        if (!lng.isFinite() || lng !in -180.0..180.0) return@withContext ToolResult.Err("invalid longitude", "bad_arg")
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,weather_code"
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                val body = r.body?.string() ?: ""
                ToolResult.Ok(body.take(512))
            }
        } catch (t: Throwable) { ToolResult.Err(t.message ?: "weather failed", "weather_error") }
    }
}
