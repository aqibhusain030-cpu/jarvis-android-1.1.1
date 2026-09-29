package com.jarvis.tools.impl

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "calendar_read",
        descriptionHi = "कैलेंडर पढ़ें",
        descriptionEn = "Read upcoming calendar events",
        parameters = mapOf("days" to ParamSpec("int", false, "Look ahead N days", "1")),
        confirmation = Confirmation.VOICE,
        permissions = listOf(android.Manifest.permission.READ_CALENDAR)
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val days = ((args["days"] as? Number)?.toInt() ?: 1).coerceIn(0, 30)
        val now = System.currentTimeMillis()
        val end = now + days * 86_400_000L
        val proj = arrayOf(
            CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
        val sel = "${CalendarContract.Events.DTSTART} BETWEEN ? AND ?"
        val out = mutableListOf<String>()
        ctx.contentResolver.query(CalendarContract.Events.CONTENT_URI,
            proj, sel, arrayOf(now.toString(), end.toString()),
            "${CalendarContract.Events.DTSTART} ASC")?.use { c ->
            while (c.moveToNext() && out.size < 10) {
                out += "${c.getString(0)} @ ${c.getLong(1)}"
            }
        }
        return if (out.isEmpty()) ToolResult.Ok("no events") else ToolResult.Ok(out.joinToString("\n"))
    }
}
