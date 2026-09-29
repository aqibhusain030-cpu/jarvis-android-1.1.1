package com.jarvis.tools.impl

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "contact_lookup",
        descriptionHi = "नाम से संपर्क खोजें",
        descriptionEn = "Look up a contact by name or number",
        parameters = mapOf("query" to ParamSpec("string", true, "Name or partial number")),
        confirmation = Confirmation.VOICE,
        permissions = listOf(android.Manifest.permission.READ_CONTACTS),
        rateLimitPerHour = 120
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val q = (args["query"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return ToolResult.Err("query required", "bad_arg")
        if (q.length > 100) return ToolResult.Err("query too long", "bad_arg")
        val cr: ContentResolver = ctx.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val proj = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER)
        val sel = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR " +
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val out = mutableListOf<String>()
        cr.query(uri, proj, sel, arrayOf("%$q%", "%$q%"), null)?.use { c ->
            while (c.moveToNext() && out.size < 5) {
                out += "${c.getString(0)}: ${c.getString(1)}"
            }
        }
        return if (out.isEmpty()) ToolResult.Err("No match for $q", "not_found")
        else ToolResult.Ok(out.joinToString("; "))
    }
}
