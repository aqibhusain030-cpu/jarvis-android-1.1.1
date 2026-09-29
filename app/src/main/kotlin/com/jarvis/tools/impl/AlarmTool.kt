package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "set_alarm",
        descriptionHi = "अलार्म लगाएँ",
        descriptionEn = "Set an alarm",
        parameters = mapOf(
            "hour" to ParamSpec("int", true, "Hour 0..23"),
            "minute" to ParamSpec("int", true, "Minute 0..59"),
            "label" to ParamSpec("string", false, "Alarm label"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val h = (args["hour"] as? Number)?.toInt() ?: return ToolResult.Err("hour required", "bad_arg")
        val m = (args["minute"] as? Number)?.toInt() ?: return ToolResult.Err("minute required", "bad_arg")
        if (h !in 0..23) return ToolResult.Err("hour must be 0..23", "bad_arg")
        if (m !in 0..59) return ToolResult.Err("minute must be 0..59", "bad_arg")
        val label = (args["label"] as? String ?: "JARVIS alarm").take(200)
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, h)
            .putExtra(AlarmClock.EXTRA_MINUTES, m)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return ToolResult.Ok("alarm set for %02d:%02d".format(h, m))
    }
}
