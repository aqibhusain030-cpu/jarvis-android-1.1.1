package com.jarvis.tools.impl

import android.content.Context
import com.jarvis.notifications.NotificationListener
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "list_notifications",
        descriptionHi = "सक्रिय सूचनाएँ सूचीबद्ध करें",
        descriptionEn = "List currently active notifications",
        confirmation = Confirmation.VOICE,
        parameters = mapOf("limit" to ParamSpec("int", false, "Max entries", "10"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val limit = ((args["limit"] as? Number)?.toInt() ?: 10).coerceIn(1, 20)
        val items = NotificationListener.active().take(limit)
        if (items.isEmpty()) return ToolResult.Ok("no notifications")
        return ToolResult.Ok(items.joinToString("\n") { "${it.pkg}: ${it.title} — ${it.text}" })
    }
}
