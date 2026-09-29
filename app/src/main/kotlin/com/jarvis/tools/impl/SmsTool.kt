package com.jarvis.tools.impl

import android.telephony.SmsManager
import com.jarvis.tools.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsTool @Inject constructor() : Tool {
    override val spec = ToolSpec(
        name = "send_sms",
        descriptionHi = "किसी नंबर पर SMS भेजें",
        descriptionEn = "Send an SMS to a phone number",
        parameters = mapOf(
            "number" to ParamSpec("string", true, "Recipient phone number"),
            "text" to ParamSpec("string", true, "Message body")
        ),
        permissions = listOf(android.Manifest.permission.SEND_SMS),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 20
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val number = args["number"] as? String ?: return ToolResult.Err("number required", "bad_arg")
        val text = args["text"] as? String ?: return ToolResult.Err("text required", "bad_arg")
        return try {
            @Suppress("DEPRECATION") val sm = SmsManager.getDefault()
            val parts = sm.divideMessage(text)
            sm.sendMultipartTextMessage(number, null, parts, null, null)
            ToolResult.Ok("sms queued to $number (${parts.size} part(s))")
        } catch (t: Throwable) { ToolResult.Err(t.message ?: "sms failed", "sms_error") }
    }
}
