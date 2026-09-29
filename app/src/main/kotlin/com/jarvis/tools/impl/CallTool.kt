package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallTool @Inject constructor(@ApplicationContext private val ctx: Context) : Tool {
    override val spec = ToolSpec(
        name = "make_call",
        descriptionHi = "किसी नंबर पर कॉल करें",
        descriptionEn = "Place a phone call",
        parameters = mapOf("number" to ParamSpec("string", true, "Destination number")),
        permissions = listOf(android.Manifest.permission.CALL_PHONE),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 20
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val number = args["number"] as? String ?: return ToolResult.Err("number required", "bad_arg")
        return try {
            ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ToolResult.Ok("call placed to $number")
        } catch (t: Throwable) { ToolResult.Err(t.message ?: "call failed", "call_error") }
    }
}
