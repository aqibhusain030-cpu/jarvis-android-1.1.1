package com.jarvis.tools.impl

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorchTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "torch",
        descriptionHi = "टॉर्च चालू या बंद करें",
        descriptionEn = "Turn the flashlight on or off",
        parameters = mapOf("state" to ParamSpec("string", true, "on | off")),
        rateLimitPerHour = 120
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val want = (args["state"] as? String ?: return ToolResult.Err("state required", "bad_arg")).lowercase()
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull { c ->
            cm.getCameraCharacteristics(c).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ToolResult.Err("No flash unit", "no_flash")
        return try {
            when (want) {
                "on" -> { cm.setTorchMode(id, true); ToolResult.Ok("torch on") }
                "off" -> { cm.setTorchMode(id, false); ToolResult.Ok("torch off") }
                else -> ToolResult.Err("state must be on|off", "bad_arg")
            }
        } catch (t: Throwable) { ToolResult.Err(t.message ?: "torch failed", "torch_error") }
    }
}
