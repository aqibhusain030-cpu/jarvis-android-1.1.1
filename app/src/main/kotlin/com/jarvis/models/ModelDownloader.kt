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

@Singleton
class ModelDownloader @Inject constructor(private val logger: JarvisLogger) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun download(
        asset: ModelAsset,
        destDir: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        ModelManifestValidator.validate(
            ExternalModelManifest(1, "single-asset-download", listOf(asset))
        )
        require(asset.downloadableToAndroid) { "Asset is not approved for Android: " + asset.id }

        val initial = URI(asset.downloadUrl)
        require(initial.scheme.equals("https", ignoreCase = true)) { "Model URL must use HTTPS" }
        require(initial.port == -1 || initial.port == 443) { "Model URL must use HTTPS port 443" }
        require(isModelHost(initial.host)) { "Model host not allowlisted: " + initial.host }

        destDir.mkdirs()
        val out = File(destDir, asset.filename)
        if (out.exists() && out.length() == asset.sizeBytes && ModelChecksum.verify(out, asset.sha256)) {
            logger.i(TAG, asset.filename + " already present and verified")
            return@withContext out
        }

        val tmp = File(destDir, asset.filename + ".part")
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
                    require(attempt < MAX_REDIRECTS) { "Too many model redirects." }
                    val location = response.header("Location") ?: error("Redirect without Location.")
                    val resolved = response.request.url.resolve(location)?.toString()
                        ?: error("Invalid model redirect.")
                    val redirected = resolved.toHttpUrlOrNull() ?: error("Invalid redirected model URL.")
                    require(redirected.scheme == "https") { "Redirected model URL must be HTTPS." }
                    require(redirected.port == 443) { "Redirected model URL must use port 443." }
                    require(isModelHost(redirected.host)) {
                        "Redirected model host not allowlisted: " + redirected.host
                    }
                    nextUrl = redirected.toString()
                } else {
                    require(response.isSuccessful) { "HTTP " + response.code + " for model URL." }
                    val body = response.body ?: error("Empty model body.")
                    val declaredLength = body.contentLength()
                    require(declaredLength < 0L || declaredLength == asset.sizeBytes) {
                        "Content-Length mismatch for " + asset.id + "."
                    }

                    var read = 0L
                    body.byteStream().use { input ->
                        tmp.outputStream().use { output ->
                            val buffer = ByteArray(1 shl 16)
                            while (true) {
                                val n = input.read(buffer)
                                if (n == -1) break
                                output.write(buffer, 0, n)
                                read += n
                                require(read <= asset.sizeBytes) {
                                    "Download exceeded expected size for " + asset.id + "."
                                }
                                onProgress(read, asset.sizeBytes)
                            }
                        }
                    }
                    require(read == asset.sizeBytes) {
                        "Downloaded byte count mismatch for " + asset.id +
                            ": expected " + asset.sizeBytes + ", got " + read + "."
                    }
                    finished = true
                }
            }
        }

        require(finished && lastCode in 200..299) { "Model request did not complete successfully." }
        require(ModelChecksum.verify(tmp, asset.sha256)) {
            tmp.delete()
            error("Model SHA-256 verification failed for " + asset.id + ".")
        }

        if (out.exists()) out.delete()
        require(tmp.renameTo(out) || run {
            tmp.copyTo(out, overwrite = true)
            tmp.delete()
            true
        }) { "Atomic model install failed for " + asset.id + "." }

        require(out.length() == asset.sizeBytes && ModelChecksum.verify(out, asset.sha256)) {
            out.delete()
            error("Installed model failed final verification for " + asset.id + ".")
        }

        logger.i(TAG, "Downloaded and verified " + asset.filename + " (" + out.length() + " bytes)")
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
