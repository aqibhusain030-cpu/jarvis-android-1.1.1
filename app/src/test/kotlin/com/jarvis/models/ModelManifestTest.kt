package com.jarvis.models

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelManifestTest {
    private val qwen = ModelAsset(
        id = "qwen",
        kind = "llm",
        provider = "huggingface",
        modelId = "Qwen/Qwen2.5-3B-Instruct-GGUF",
        revision = "cc1e68eea5f05f88f41a6de1fc73110178f23715",
        format = "gguf",
        filename = "qwen.gguf",
        sizeBytes = 1000L,
        sha256 = "a".repeat(64),
        license = "qwen-research",
        licenseUrl = "https://huggingface.co/license",
        downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/cc1e68eea5f05f88f41a6de1fc73110178f23715/qwen.gguf",
        sourceUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/tree/cc1e68eea5f05f88f41a6de1fc73110178f23715",
        backend = "llama.cpp",
        precision = "q4",
        quantization = "Q4_K_M",
        minHardware = ModelHardwareRequirement(1024, 1000, listOf("arm64-v8a"), "CPU"),
        verification = ModelVerification("SHA-256", "test", true)
    )

    @Test
    fun rejects_mutable_revision() {
        val mutable = qwen.copy(
            revision = "main",
            downloadUrl = qwen.downloadUrl.replace(qwen.revision, "main"),
            sourceUrl = qwen.sourceUrl.replace(qwen.revision, "main")
        )
        assertThat(runCatching {
            ModelManifestValidator.validate(
                ExternalModelManifest(1, "jarvis-test-asset", listOf(mutable))
            )
        }.isFailure).isTrue()
    }

    @Test
    fun rejects_unverified_asset() {
        val bad = qwen.copy(verification = qwen.verification.copy(verified = false))
        assertThat(runCatching {
            ModelManifestValidator.validate(
                ExternalModelManifest(1, "jarvis-test-asset", listOf(bad))
            )
        }.isFailure).isTrue()
    }

    @Test
    fun accepts_pinned_asset_metadata() {
        ModelManifestValidator.validate(
            ExternalModelManifest(1, "jarvis-test-asset", listOf(qwen))
        )
    }
}
