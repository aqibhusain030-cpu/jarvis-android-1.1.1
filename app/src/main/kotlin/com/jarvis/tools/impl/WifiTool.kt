package com.jarvis.tools.impl

import android.content.Context
import android.net.wifi.WifiManager
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "wifi_status",
        descriptionHi = "WiFi की स्थिति बताएँ",
        descriptionEn = "Report Wi-Fi status (enabled, SSID)",
        parameters = emptyMap()
    )
    @Suppress("DEPRECATION")
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val enabled = wm.isWifiEnabled
        return ToolResult.Ok("wifi=${if (enabled) "on" else "off"}")
    }
}
