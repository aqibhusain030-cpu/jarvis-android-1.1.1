package com.jarvis.tools.impl

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.jarvis.JarvisApplication
import com.jarvis.R
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotifyTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "notify",
        descriptionHi = "सूचना भेजें",
        descriptionEn = "Post a notification",
        parameters = mapOf(
            "title" to ParamSpec("string", true, "Notification title"),
            "body" to ParamSpec("string", true, "Notification body"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val title = (args["title"] as? String ?: "JARVIS").trim().take(120)
        val body = (args["body"] as? String)?.trim() ?: return ToolResult.Err("body required", "bad_arg")
        if (body.isBlank()) return ToolResult.Err("body required", "bad_arg")
        if (body.length > 4000) return ToolResult.Err("body too long", "bad_arg")
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = NotificationCompat.Builder(ctx, JarvisApplication.CH_ALERTS)
            .setSmallIcon(R.drawable.ic_jarvis)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        nm.notify(System.currentTimeMillis().toInt(), n)
        return ToolResult.Ok("notification posted")
    }
}
