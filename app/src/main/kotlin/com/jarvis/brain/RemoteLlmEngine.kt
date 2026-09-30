package com.jarvis.brain

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI-compatible remote inference adapter for the JARVIS distributed model tier.
 *
 * This adapter is deliberately non-authoritative: it only produces text/tool
 * requests. Android ToolDispatcher remains the execution authority.
 */
@Singleton
class RemoteLlmEngine @Inject constructor(
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val credentials: RemoteInferenceCredentialStore
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(65, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        prompt: String,
        maxTokens: Int = 700,
        temperature: Double = 0.2
    ): String = withContext(Dispatchers.IO) {
        val c = config.current.value
        if (!c.remoteInferenceEnabled) return@withContext ""
        val base = c.remoteInferenceBaseUrl.trim().removeSuffix("/")
        if (!base.startsWith("https://", ignoreCase = true)) {
            logger.w(TAG, "Remote inference disabled: HTTPS endpoint required")
            return@withContext ""
        }

        val body = buildJsonObject {
            put("model", c.remoteInferenceModel)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", temperature.coerceIn(0.0, 2.0))
            put("max_tokens", maxTokens.coerceIn(1, 4096))
            put("stream", false)
        }.toString()

        val request = Request.Builder()
            .url(if (base.endsWith("/v1", ignoreCase = true)) "$base/chat/completions" else "$base/v1/chat/completions")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .apply {
                credentials.getToken()?.let { header("Authorization", "Bearer " + it) }
            }
            .build()

        runCatching {
            client.newBuilder()
                .readTimeout(c.remoteInferenceTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
                .callTimeout((c.remoteInferenceTimeoutMs + 5_000L).coerceAtMost(305_000L), TimeUnit.MILLISECONDS)
                .build()
                .newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logger.w(TAG, "Remote inference HTTP " + response.code)
                    return@use ""
                }
                val raw = response.body?.string().orEmpty()
                val root = json.parseToJsonElement(raw).jsonObject
                root["choices"]?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonObject
                    ?.get("content")
                    ?.toString()
                    ?.trim('"')
                    .orEmpty()
            }
        }.onFailure {
            logger.w(TAG, "Remote inference request failed", it)
        }.getOrDefault("")
    }

    companion object {
        private const val TAG = "RemoteLlmEngine"
    }
}
