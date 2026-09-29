package com.jarvis.security

import com.jarvis.logging.JarvisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Append-only operational audit log. Sensitive payloads are intentionally excluded. */
@Singleton
class AuditLogger @Inject constructor(private val logger: JarvisLogger) {
    private val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    suspend fun log(tool: String, args: Map<String, Any?>, result: String, confirmed: Boolean) =
        withContext(Dispatchers.IO) {
            val now = Date()
            val file = File(logger.dir("audit"), "${fmt.format(now)}.log")
            val status = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(result).jsonObject["code"]?.toString() }
                .getOrNull()
                ?: if (result.contains("\"ok\":true")) "ok" else "unknown"
            val keys = args.keys.sorted().take(32).joinToString(",")
            val line = "${timeFmt.format(now)} | $tool | confirmed=$confirmed | arg_keys=$keys | result_code=$status | result_len=${result.length}"
            runCatching { file.appendText(line) }
                .onFailure { logger.e(TAG, "audit write failed", it) }
            cleanup(fmt.format(now))
        }

    private fun cleanup(today: String) {
        val dir = logger.dir("audit")
        val keep = System.currentTimeMillis() - 30L * 86_400_000
        dir.listFiles()?.forEach { f ->
            val day = f.name.removeSuffix(".log")
            if (day != today && f.lastModified() < keep) f.delete()
        }
    }
    companion object { private const val TAG = "AuditLogger" }
}
