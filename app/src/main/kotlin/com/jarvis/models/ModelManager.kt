package com.jarvis.models

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates downloading and locating model files. */
@Singleton
class ModelManager @Inject constructor(
    private val config: ConfigRepository,
    private val downloader: ModelDownloader,
    private val logger: JarvisLogger
) {
    private val manifest = ModelManifest.DEFAULT

    fun entry(id: String): ModelEntry? = manifest.models.firstOrNull { it.id == id }

    fun all(): List<ModelEntry> = manifest.models

    fun isPresent(entry: ModelEntry): Boolean {
        val f = target(entry)
        return f.exists() && ModelChecksum.verify(f, entry.sha256)
    }

    fun target(entry: ModelEntry): File =
        File(File(config.dir("models"), entry.kind), entry.filename)

    suspend fun download(id: String, onProgress: (Long, Long) -> Unit = {_,_->}): File? {
        val entry = entry(id) ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                downloader.download(entry, File(config.dir("models"), entry.kind), onProgress)
            }.onFailure { logger.e(TAG, "download $id failed", it) }.getOrNull()
        }
    }

    companion object { private const val TAG = "ModelManager" }
}
