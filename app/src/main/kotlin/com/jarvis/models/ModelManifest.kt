package com.jarvis.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ModelHardwareRequirement(
    val ramMb: Long,
    val freeStorageMb: Long,
    val abis: List<String>,
    val accelerator: String
)

@Serializable
data class ModelVerification(
    val method: String,
    val source: String,
    val verified: Boolean
)

@Serializable
data class ModelAsset(
    val id: String,
    val kind: String,
    val provider: String,
    val modelId: String,
    val revision: String,
    val format: String,
    val filename: String,
    val sizeBytes: Long,
    val sha256: String,
    val license: String,
    val licenseUrl: String,
    val downloadUrl: String,
    val sourceUrl: String,
    val backend: String,
    val precision: String,
    val quantization: String,
    val capabilities: List<String> = emptyList(),
    val downloadableToAndroid: Boolean = false,
    val minHardware: ModelHardwareRequirement,
    val verification: ModelVerification
)

@Serializable
data class ExternalModelManifest(
    val schemaVersion: Int,
    val manifestId: String,
    val assets: List<ModelAsset>
)

object ModelManifestParser {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = true
    }

    fun parse(raw: String): ExternalModelManifest {
        val manifest = json.decodeFromString<ExternalModelManifest>(raw)
        ModelManifestValidator.validate(manifest)
        return manifest
    }
}

object ModelManifestValidator {
    private val SHA256 = Regex("[0-9a-fA-F]{64}")
    private val REVISION = Regex("[0-9a-fA-F]{40}")
    private val SAFE_FILENAME = Regex("[A-Za-z0-9._-]+")
    private val ALLOWED_FORMATS = setOf("gguf", "ggml", "onnx", "tflite")
    private val MODEL_ID = Regex("[A-Za-z0-9._-]+/[A-Za-z0-9._-]+")

    fun validate(manifest: ExternalModelManifest) {
        require(manifest.schemaVersion == 1) { "Unsupported model manifest schema." }
        require(manifest.manifestId.matches(Regex("[A-Za-z0-9._-]{8,128}"))) {
            "Invalid model manifest ID."
        }
        require(manifest.assets.isNotEmpty()) { "Model manifest contains no assets." }

        val ids = manifest.assets.map { it.id }
        require(ids.distinct().size == ids.size) { "Model manifest contains duplicate asset IDs." }

        manifest.assets.forEach { asset ->
            require(asset.id.isNotBlank()) { "Model asset ID is blank." }
            require(asset.kind.isNotBlank()) { "Model asset kind is blank for " + asset.id + "." }
            require(asset.provider == "huggingface") {
                "Unsupported model provider for " + asset.id + "."
            }
            require(asset.modelId.matches(MODEL_ID)) {
                "Invalid model ID for " + asset.id + "."
            }
            require(asset.revision.matches(REVISION)) {
                "Model " + asset.id + " must use an immutable 40-hex revision."
            }
            require(asset.format in ALLOWED_FORMATS) {
                "Unsupported model format for " + asset.id + "."
            }
            require(asset.filename.matches(SAFE_FILENAME)) {
                "Unsafe model filename for " + asset.id + "."
            }
            require(asset.sizeBytes > 0L) { "Invalid model size for " + asset.id + "." }
            require(asset.sha256.matches(SHA256)) {
                "Model " + asset.id + " has no valid SHA-256."
            }
            require(asset.license.isNotBlank()) { "License missing for " + asset.id + "." }
            require(isHttps(asset.licenseUrl)) { "License URL must be HTTPS for " + asset.id + "." }
            require(isHttps(asset.downloadUrl)) { "Download URL must be HTTPS for " + asset.id + "." }
            require(isHttps(asset.sourceUrl)) { "Source URL must be HTTPS for " + asset.id + "." }
            require(asset.downloadUrl.contains("/resolve/" + asset.revision + "/" + asset.filename)) {
                "Download URL is not pinned to the declared revision for " + asset.id + "."
            }
            require(asset.sourceUrl.contains("/tree/" + asset.revision)) {
                "Source URL is not pinned to the declared revision for " + asset.id + "."
            }
            require(asset.verification.verified) {
                "Model " + asset.id + " is not marked as independently verified."
            }
            require(asset.verification.method.equals("SHA-256", ignoreCase = true)) {
                "Model " + asset.id + " must use SHA-256 verification."
            }
            require(asset.minHardware.ramMb > 0L)
            require(asset.minHardware.freeStorageMb >= asset.sizeBytes / 1_000_000L)
            require(asset.minHardware.abis.isNotEmpty())
        }
    }

    private fun isHttps(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true)
}
