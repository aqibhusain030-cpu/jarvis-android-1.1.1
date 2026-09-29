package com.jarvis.tools.impl

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "bluetooth_status",
        descriptionHi = "Bluetooth की स्थिति",
        descriptionEn = "Report Bluetooth status",
        parameters = emptyMap(),
        permissions = listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
    )
    @SuppressLint("MissingPermission")
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val bm = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val a = bm?.adapter ?: return ToolResult.Err("No Bluetooth adapter", "no_bt")
        return ToolResult.Ok("bluetooth=${if (a.isEnabled) "on" else "off"}")
    }
}
