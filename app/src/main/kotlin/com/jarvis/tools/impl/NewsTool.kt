package com.jarvis.tools.impl

import com.jarvis.security.SecurityPolicy
import com.jarvis.tools.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class NewsArticle(
    val title: String,
    val url: String,
    val domain: String,
    val seenAtUtc: String,
    val language: String,
    val sourceCountry: String
)

object NewsParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(raw: String, max: Int): List<NewsArticle> {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            ?: return emptyList()
        val rows = root["articles"]?.jsonArray ?: return emptyList()
        return rows.mapNotNull { element ->
            val obj = element.jsonObject
            val title = obj["title"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().trim()
            val url = obj["url"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().trim()
            val domain = obj["domain"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().trim()
            if (title.isBlank() || url.isBlank()) return@mapNotNull null
            NewsArticle(
                title = title.take(500),
                url = url.take(2048),
                domain = domain.take(253),
                seenAtUtc = obj["seendate"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().take(32),
                language = obj["language"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().take(64),
                sourceCountry = obj["sourcecountry"]?.jsonPrimitive?.contentOrNullSafe().orEmpty().take(64)
            )
        }
            .distinctBy { it.url }
            .take(max.coerceIn(1, 20))
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { content }.getOrNull()
}

@Singleton
class NewsTool @Inject constructor(
    private val policy: SecurityPolicy
) : Tool {

    override val spec = ToolSpec(
        name = "get_news",
        descriptionHi = "ताज़ा समाचार खोजें",
        descriptionEn = "Fetch near-real-time news headlines and articles",
        parameters = mapOf(
            "query" to ParamSpec("string", required = false, description = "Topic or keywords; empty for broad headlines"),
            "limit" to ParamSpec("int", required = false, description = "Number of articles, 1-20"),
            "timespan" to ParamSpec("string", required = false, description = "15m, 1h, 6h, 24h, 3d, or 7d")
        ),
        rateLimitPerHour = 30,
        timeoutMs = 20_000
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun invoke(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val query = sanitizeQuery(args["query"] as? String ?: "")
        val limit = (args["limit"] as? Number)?.toInt()?.coerceIn(1, 20) ?: 8
        val timespan = sanitizeTimespan(args["timespan"] as? String ?: "24h")

        val encodedQuery = URLEncoder.encode(
            query.ifBlank { "news" },
            StandardCharsets.UTF_8.name()
        )
        val url = "$API_BASE?query=$encodedQuery&mode=artlist&maxrecords=$limit&timespan=$timespan&sort=datedesc&format=json"

        if (!policy.isAllowedUrl(url)) {
            return@withContext ToolResult.Err("Live news provider is blocked by network policy.", "network_denied")
        }

        try {
            client.newCall(
                Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "JARVIS-Android/1.1.1")
                    .get()
                    .build()
            ).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext ToolResult.Err(
                        "News provider returned HTTP ${response.code}.",
                        "news_http_${response.code}"
                    )
                }
                val body = response.body ?: return@withContext ToolResult.Err(
                    "News provider returned an empty response.",
                    "news_empty"
                )
                if (body.contentLength() > MAX_RESPONSE_BYTES) {
                    return@withContext ToolResult.Err(
                        "News response exceeded the size limit.",
                        "news_response_too_large"
                    )
                }
                val raw = body.byteStream().use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (total < MAX_RESPONSE_BYTES) {
                        val n = input.read(
                            buffer,
                            0,
                            minOf(buffer.size, (MAX_RESPONSE_BYTES - total).toInt())
                        )
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        total += n
                    }
                    out.toString(StandardCharsets.UTF_8.name())
                }

                val articles = NewsParser.parse(raw, limit)
                    .filter { policy.isSafeExternalUrl(it.url) }

                ToolResult.Ok(
                    mapOf(
                        "provider" to "GDELT DOC 2.0",
                        "query" to query.ifBlank { "news" },
                        "timespan" to timespan,
                        "count" to articles.size,
                        "articles" to articles.map {
                            mapOf(
                                "title" to it.title,
                                "url" to it.url,
                                "source" to it.domain,
                                "seenAtUtc" to it.seenAtUtc,
                                "language" to it.language,
                                "sourceCountry" to it.sourceCountry
                            )
                        }
                    )
                )
            }
        } catch (t: Throwable) {
            ToolResult.Err(t.message ?: "Unable to fetch live news.", "news_error")
        }
    }

    private fun sanitizeQuery(value: String): String =
        value.replace(Regex("[\\u0000-\\u001F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(256)

    private fun sanitizeTimespan(value: String): String =
        value.trim().lowercase().takeIf { it.matches(TIMESPAN_REGEX) } ?: "24h"

    private companion object {
        const val API_BASE = "https://api.gdeltproject.org/api/v2/doc/doc"
        const val MAX_RESPONSE_BYTES = 128 * 1024L
        val TIMESPAN_REGEX = Regex("(?:15m|30m|1h|6h|12h|24h|2d|3d|7d)")
    }
}
