# JARVIS Production Status

## What “production complete” means in this repository

This tree is a **production-oriented source distribution** of an on-device Android
assistant. It is not a prebuilt APK with multi‑GB models baked in (that would be
many gigabytes and license-sensitive).

### Complete in source

| Layer | Status |
|-------|--------|
| Compose UI + onboarding | Implemented |
| Foreground service + wake word + VAD + TTS | Implemented |
| STT via Whisper JNI | Implemented (needs model + native build) |
| LLM via Llama JNI + Python brain | **Wired** via LocalLlmEngine + BrainBootstrap |
| ToolDispatcher + SecurityPolicy + confirmations | Hardened (post-audit) |
| 26 tools | Implemented |
| Goal engine + scheduler alarm → GoalEngine | Wired |
| Room memory + audit log + kill switch | Implemented |
| CI skeleton | Present |

### Required on the build machine / device

1. **NDK build** of llama.cpp + whisper.cpp (`scripts/build_*.sh`)
2. **Gradle bootstrap** is included in `gradlew` / `gradlew.bat` and verifies the Gradle 8.11.1 distribution SHA-256.
3. **Model files** are downloaded at runtime into app-private storage by `ModelManager` and verified before activation.
4. **Device**: arm64, ≥8 GB RAM recommended, Android 12+

### Runtime data path (now wired)

```
Wake word → VAD → Whisper STT → BrainBridge
  → BrainBootstrap (LLM + ToolBridge)
  → Python brain.respond
  → optional ```tool``` calls → ToolBridgeKotlin → ToolDispatcher
  → TTS
```

### Honest limits

- No root: reboot/shutdown/toggle airplane mode unavailable
- Screenshot needs MediaProjection user consent
- Play Store restricted permissions need policy declarations or removal
- openWakeWord pretrained models may be CC BY-NC-SA — check before commercial use
- Without a loaded GGUF model, brain uses rule-based fallbacks only

### First production build

```bash
unzip jarvis-android-complete.zip && cd jarvis-android
export ANDROID_NDK_HOME=…/ndk/26.1.10909125
bash scripts/build_llama.sh && bash scripts/build_whisper.sh
# Pin real SHA-256 in ModelManifest.kt, then:
bash scripts/fetch_models.sh
# Copy models into device filesDir or use in-app ModelManager after fixing hashes
./gradlew :app:assembleRelease
```
