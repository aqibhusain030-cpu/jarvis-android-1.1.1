package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "set_timer",
        descriptionHi = "टाइमर लगाएँ",
        descriptionEn = "Set a countdown timer",
        parameters = mapOf(
            "seconds" to ParamSpec("int", true, "Duration in seconds"),
            "label" to ParamSpec("string", false, "Timer label"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val s = (args["seconds"] as? Number)?.toInt()
            ?: return ToolResult.Err("seconds required", "bad_arg")
        val label = args["label"] as? String ?: "JARVIS timer"
        ctx.startActivity(Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, s)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ToolResult.Ok("timer set for ${s}s")
    }
}
