package com.jarvis.tools.impl

import android.content.Context
import android.media.AudioManager
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "set_volume",
        descriptionHi = "वॉल्यूम सेट करें",
        descriptionEn = "Set a stream's volume level (0..100)",
        parameters = mapOf(
            "stream" to ParamSpec("string", false, "music|ring|alarm|call", "music"),
            "level" to ParamSpec("int", true, "0..100"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (args["stream"] as? String ?: "music") {
            "ring" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "call" -> AudioManager.STREAM_VOICE_CALL
            else -> AudioManager.STREAM_MUSIC
        }
        val level = (args["level"] as? Number)?.toInt()
            ?: return ToolResult.Err("level required", "bad_arg")
        val max = am.getStreamMaxVolume(stream)
        val target = (level * max / 100).coerceIn(0, max)
        am.setStreamVolume(stream, target, 0)
        return ToolResult.Ok("volume set to $target/$max")
    }
}
