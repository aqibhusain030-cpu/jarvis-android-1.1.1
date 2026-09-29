package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "take_photo",
        descriptionHi = "तस्वीर खींचें",
        descriptionEn = "Open the camera to take a photo",
        parameters = mapOf("lens" to ParamSpec("string", false, "front|back", "back")),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 60
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val lens = args["lens"] as? String ?: "back"
        val i = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .putExtra("android.intent.extras.CAMERA_FACING", if (lens == "front") 1 else 0)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return ToolResult.Ok("camera opened ($lens)")
    }
}
