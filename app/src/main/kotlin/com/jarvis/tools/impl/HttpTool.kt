package com.jarvis.tools.impl

import com.jarvis.security.SecurityPolicy
import com.jarvis.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HTTPS GET against the security-policy allowlist only.
 * Arbitrary hosts are rejected even if confirmation was granted.
 */
@Singleton
class HttpTool @Inject constructor(
    private val policy: SecurityPolicy
) : Tool {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override val spec = ToolSpec(
        name = "http_get",
        descriptionHi = "HTTP GET अनुरोध (allowlist)",
        descriptionEn = "Perform an HTTPS GET against the allowlist",
        parameters = mapOf("url" to ParamSpec("string", true, "https://...")),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 100
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val url = args["url"] as? String ?: return@withContext ToolResult.Err("url required", "bad_arg")
        if (!policy.isAllowedUrl(url)) {
            return@withContext ToolResult.Err("URL not on allowlist or not https: $url", "url_denied")
        }
        try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { r ->
                val body = r.body?.string()?.take(8_192) ?: ""
                ToolResult.Ok("HTTP ${r.code}: $body")
            }
        } catch (t: Throwable) {
            ToolResult.Err(t.message ?: "http failed", "http_error")
        }
    }
}
