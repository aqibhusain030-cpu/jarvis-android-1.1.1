package com.jarvis.tools.impl

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "get_location",
        descriptionHi = "वर्तमान स्थान बताएँ",
        descriptionEn = "Return the current GPS location",
        parameters = mapOf("highAccuracy" to ParamSpec("bool", false, "Prefer GPS", "true")),
        confirmation = Confirmation.VOICE,
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION),
        rateLimitPerHour = 120
    )
    @SuppressLint("MissingPermission")
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = if ((args["highAccuracy"] as? Boolean ?: true))
            LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
        val loc = lm.getLastKnownLocation(provider)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: return ToolResult.Err("No location fix", "no_fix")
        return ToolResult.Ok("lat=${loc.latitude}, lng=${loc.longitude}, acc=${loc.accuracy}m")
    }
}
