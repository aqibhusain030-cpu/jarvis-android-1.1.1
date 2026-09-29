package com.jarvis.models

import com.jarvis.logging.JarvisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads only immutable, HTTPS model URLs; verifies a 64-hex SHA-256 before
 * replacing the runtime artifact. Redirects are manually bounded and restricted
 * to the Hugging Face host family, with the final digest remaining authoritative.
 */
@Singleton
class ModelDownloader @Inject constructor(private val logger: JarvisLogger) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun download(
        entry: ModelEntry,
        destDir: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        require(entry.sha256.matches(Regex("[0-9A-Fa-f]{64}"))) {
            "Model ${entry.filename} has no valid pinned SHA-256"
        }
        require(entry.filename.isNotBlank() &&
            !entry.filename.contains('\u0000') &&
            !entry.filename.contains("..") &&
            !entry.filename.startsWith("/") &&
            !entry.filename.contains('\\')) {
            "Invalid model filename: ${entry.filename}"
        }

        val initial = URI(entry.url)
        require(initial.scheme.equals("https", ignoreCase = true)) { "Model URL must use HTTPS" }
        require(initial.port == -1 || initial.port == 443) { "Model URL must use HTTPS port 443" }
        require(isModelHost(initial.host)) { "Model host not allowlisted: ${initial.host}" }

        destDir.mkdirs()
        val out = File(destDir, entry.filename)
        if (out.exists() && ModelChecksum.verify(out, entry.sha256)) {
            logger.i(TAG, "${entry.filename} already present and verified")
            return@withContext out
        }

        val tmp = File(destDir, "${entry.filename}.part")
        if (tmp.exists()) tmp.delete()
        var nextUrl = initial.toString()
        var finished = false
        var lastCode = -1

        repeat(MAX_REDIRECTS + 1) { attempt ->
            if (finished) return@repeat
            val request = Request.Builder().url(nextUrl).build()
            client.newCall(request).execute().use { response ->
                lastCode = response.code
                if (response.isRedirect) {
                    require(attempt < MAX_REDIRECTS) { "too many model redirects" }
                    val location = response.header("Location") ?: error("redirect without Location")
                    val resolved = response.request.url.resolve(location)?.toString()
                        ?: error("invalid model redirect")
                    val redirected = resolved.toHttpUrlOrNull()
                        ?: error("invalid redirected model URL")
                    require(redirected.scheme == "https") { "redirected model URL must be HTTPS" }
                    require(redirected.port == 443) { "redirected model URL must use port 443" }
                    require(isModelHost(redirected.host)) { "redirected model host not allowlisted: ${redirected.host}" }
                    nextUrl = redirected.toString()
                } else {
                    require(response.isSuccessful) { "HTTP ${response.code} for model URL" }
                    val body = response.body ?: error("empty model body")
                    val total = body.contentLength().takeIf { it > 0 } ?: entry.sizeBytes
                    var read = 0L
                    body.byteStream().use { input ->
                        tmp.outputStream().use { output ->
                            val buffer = ByteArray(1 shl 16)
                            while (true) {
                                val n = input.read(buffer)
                                if (n == -1) break
                                output.write(buffer, 0, n)
                                read += n
                                if (entry.sizeBytes > 0 &&
                                    read > entry.sizeBytes + maxOf(1_048_576L, entry.sizeBytes / 10)) {
                                    error("download exceeded expected size for ${entry.filename}")
                                }
                                onProgress(read, total)
                            }
                        }
                    }
                    finished = true
                }
            }
        }

        require(finished && lastCode in 200..299) { "model request did not complete successfully" }
        require(ModelChecksum.verify(tmp, entry.sha256)) {
            tmp.delete()
            false
        }
        if (out.exists()) out.delete()
        if (!tmp.renameTo(out)) {
            tmp.copyTo(out, overwrite = true)
            tmp.delete()
        }
        logger.i(TAG, "downloaded and verified ${entry.filename} (${out.length()} bytes)")
        out
    }

    private fun isModelHost(host: String?): Boolean {
        val h = host?.lowercase()?.trim() ?: return false
        return h == "huggingface.co" || h.endsWith(".huggingface.co") ||
            h == "hf.co" || h.endsWith(".hf.co")
    }

    companion object {
        private const val TAG = "ModelDownloader"
        private const val MAX_REDIRECTS = 4
    }
}
