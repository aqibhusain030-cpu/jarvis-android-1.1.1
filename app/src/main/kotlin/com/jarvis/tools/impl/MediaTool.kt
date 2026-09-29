package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Play / pause / next / previous via AudioManager dispatchMediaKeyEvent. */
@Singleton
class MediaTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "media_control",
        descriptionHi = "मीडिया नियंत्रित करें",
        descriptionEn = "Control media playback (play/pause/next/prev)",
        parameters = mapOf("action" to ParamSpec("string", true, "play|pause|next|prev|stop"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val action = (args["action"] as? String ?: return ToolResult.Err("action required", "bad_arg")).lowercase()
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val code = when (action) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return ToolResult.Err("unknown action", "bad_arg")
        }
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        return ToolResult.Ok("media $action")
    }
}
