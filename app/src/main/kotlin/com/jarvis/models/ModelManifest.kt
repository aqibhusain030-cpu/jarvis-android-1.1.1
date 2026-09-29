package com.jarvis.models

/** Immutable model catalog. Only artifacts with authoritative digests are downloadable by default. */
data class ModelEntry(
    val id: String,
    val kind: String,
    val filename: String,
    val url: String,
    val sizeBytes: Long,
    val sha256: String
)

data class ModelManifest(val models: List<ModelEntry>) {
    companion object {
        // Qwen2.5-3B-Instruct-Q4_K_M from the Hugging Face file listing.
        // Whisper small ggml-small.bin from whisper.cpp model listing.
        // The wake-word model is intentionally NOT in the automatic catalog:
        // its upstream release currently does not expose a cryptographic asset digest.
        val DEFAULT = ModelManifest(
            listOf(
                ModelEntry(
                    id = "qwen3b",
                    kind = "llm",
                    filename = "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
                    url = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d/qwen2.5-3b-instruct-q4_k_m.gguf",
                    sizeBytes = 2_000_000_000L,
                    sha256 = "626b4a6678b86442240e33df819e00132d3ba7dddfe1cdc4fbb18e0a9615c62d"
                ),
                ModelEntry(
                    id = "whisper-small",
                    kind = "stt",
                    filename = "ggml-small.bin",
                    url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b/ggml-small.bin",
                    sizeBytes = 488_000_000L,
                    sha256 = "1be3a9b2063867b937e64e2ec7483364a79917e157fa98c5d94b5c1fffea987b"
                )
            )
        )
    }
}
