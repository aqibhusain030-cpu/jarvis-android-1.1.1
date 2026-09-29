package com.jarvis.models

import android.content.Context
import com.jarvis.config.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Installs all three openWakeWord ONNX assets only after SHA-256 verification.
 * No wake-word weight is accepted without an explicit digest sidecar.
 */
@Singleton
class WakeWordAssetProvisioner @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val config: ConfigRepository
) {
    data class Assets(
        val mel: File,
        val embedding: File,
        val classifier: File
    )

    fun ensureInstalled(): Assets {
        val assets = listOf(
            "melspectrogram.onnx",
            "embedding_model.onnx",
            "hey_jarvis_v0.1.onnx"
        )

        val installed = assets.associateWith { filename ->
            val target = config.modelFile("wakeword", filename)
            val asset = "models/wakeword/$filename"
            val digestFile = File(target.parentFile, "$filename.sha256")
            ensureOne(target, digestFile, asset)
            target
        }

        return Assets(
            mel = installed.getValue("melspectrogram.onnx"),
            embedding = installed.getValue("embedding_model.onnx"),
            classifier = installed.getValue("hey_jarvis_v0.1.onnx")
        )
    }

    private fun ensureOne(target: File, sidecar: File, modelAsset: String) {
        target.parentFile?.mkdirs()

        if (target.exists() && sidecar.exists()) {
            val digest = readDigest(sidecar)
            check(ModelChecksum.verify(target, digest)) {
                "wake-word asset checksum mismatch: ${target.name}"
            }
            return
        }

        val hashAsset = "$modelAsset.sha256"
        runCatching {
            val digest = ctx.assets.open(hashAsset).bufferedReader().use { readDigest(it.readText()) }
            require(digest.matches(Regex("[0-9A-Fa-f]{64}"))) {
                "invalid wake-word asset digest: $modelAsset"
            }

            val tmp = File(target.parentFile, target.name + ".asset.part")
            ctx.assets.open(modelAsset).use { input ->
                tmp.outputStream().use { output -> input.copyTo(output) }
            }
            require(ModelChecksum.verify(tmp, digest)) {
                "wake-word asset checksum mismatch: ${target.name}"
            }
            if (target.exists()) target.delete()
            require(tmp.renameTo(target)) { "wake-word asset install failed: ${target.name}" }
            sidecar.writeText("$digest  ${target.name}\n", Charsets.UTF_8)
        }.getOrElse { e ->
            throw IllegalStateException(
                "Wake-word asset unavailable: ${target.name}. " +
                    "Provision verified assets with scripts/fetch_wakeword.sh.",
                e
            )
        }
    }

    private fun readDigest(raw: String): String =
        raw.trim().split(Regex("\\s+")).firstOrNull().orEmpty()

    companion object {
        // Intentionally no embedded/guessed digests.
    }
}
