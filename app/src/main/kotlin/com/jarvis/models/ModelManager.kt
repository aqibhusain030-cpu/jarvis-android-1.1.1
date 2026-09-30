package com.jarvis.models

import android.content.Context
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: ConfigRepository,
    private val downloader: ModelDownloader,
    private val verifier: ModelAssetVerifier,
    private val logger: JarvisLogger
) {
    private val manifest: ExternalModelManifest by lazy {
        val raw = context.assets.open(MANIFEST_ASSET).bufferedReader().use { it.readText() }
        ModelManifestParser.parse(raw)
    }

    fun asset(id: String): ModelAsset? = manifest.assets.firstOrNull { it.id == id }

    fun assets(): List<ModelAsset> = manifest.assets.toList()

    fun assetForFilename(kind: String, filename: String): ModelAsset? =
        manifest.assets.firstOrNull { it.kind == kind && it.filename == filename }

    fun isPresent(asset: ModelAsset): Boolean =
        verifier.verifyFile(asset, target(asset)).verified

    fun target(asset: ModelAsset): File =
        File(File(config.dir("models"), asset.kind), asset.filename)

    fun verify(asset: ModelAsset): ModelVerificationResult =
        verifier.verifyFile(asset, target(asset))

    suspend fun download(
        id: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): File? = withContext(Dispatchers.IO) {
        val asset = asset(id)
        if (asset == null) {
            logger.e(TAG, "Unknown model asset: " + id)
            return@withContext null
        }
        runCatching {
            downloader.download(asset, File(config.dir("models"), asset.kind), onProgress)
        }.onFailure {
            logger.e(TAG, "Model download failed: " + id, it)
        }.getOrNull()
    }

    companion object {
        private const val TAG = "ModelManager"
        private const val MANIFEST_ASSET = "models/manifest.v1.json"
    }
}
