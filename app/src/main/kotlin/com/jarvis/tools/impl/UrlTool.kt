package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarvis.security.SecurityPolicy
import com.jarvis.tools.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Open a URL in the default browser. Only https is accepted; confirmation
 * is required via ToolSpec and SecurityPolicy.
 */
@Singleton
class UrlTool @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val policy: SecurityPolicy
) : Tool {
    override val spec = ToolSpec(
        name = "open_url",
        descriptionHi = "URL खोलें",
        descriptionEn = "Open an HTTPS URL in the default browser",
        parameters = mapOf("url" to ParamSpec("string", true, "https://...")),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 60
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val url = (args["url"] as? String)?.trim()
            ?: return ToolResult.Err("url required", "bad_arg")
        if (!policy.isSafeExternalUrl(url)) {
            return ToolResult.Err("URL is not an allowed public HTTPS destination", "url_denied")
        }
        return try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            ToolResult.Ok("opened $url")
        } catch (t: Throwable) {
            ToolResult.Err(t.message ?: "open failed", "url_error")
        }
    }
}
