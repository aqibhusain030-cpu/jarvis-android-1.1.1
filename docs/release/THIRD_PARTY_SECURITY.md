# Third-Party Security Pinning

| Component | Source | Immutable revision | Verification note |
|---|---|---|---|
| llama.cpp | `https://github.com/ggml-org/llama.cpp.git` | `526c43b8f7dfea9032e9f35e7a1be9183ca7cc20` | Revision resolved from GitHub; commit verification recorded in `third_party/LOCKFILE`. |
| whisper.cpp | `https://github.com/ggml-org/whisper.cpp.git` | `927cfce34f31707e17f2bff35c349632fb9e2c3a` (`v1.9.4`) | Tag object is unsigned; commit SHA is explicitly pinned. |
| Gradle | `https://services.gradle.org/distributions/gradle-8.11.1-bin.zip` | `8.11.1` | Distribution SHA-256 is pinned in `gradle-wrapper.properties`. |
| Qwen 3B GGUF | Hugging Face | immutable revision URL | SHA-256 is pinned in `ModelManifest.kt`. |
| Whisper small | Hugging Face | immutable revision URL | SHA-256 is pinned in `ModelManifest.kt`. |
