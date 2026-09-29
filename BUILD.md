# JARVIS build and provisioning

## Build

Use JDK 17 and Android SDK API 36 with NDK 26.1.10909125 and CMake 3.22.1.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

`gradlew` and `gradlew.bat` are checksum-verified Gradle bootstrap scripts. They download Gradle 8.11.1 only from `services.gradle.org`, verify the pinned SHA-256, then invoke the cached distribution.

## Native dependencies

```bash
export ANDROID_NDK_HOME=.../ndk/26.1.10909125
bash scripts/build_llama.sh
bash scripts/build_whisper.sh
```

Native sources are fetched only at the immutable commits recorded in `third_party/LOCKFILE`.

## Models

Large model weights are intentionally not embedded in the source release. The runtime `ModelManager`
downloads the locked LLM/STT entries into app-private storage and verifies SHA-256 before activation.
The developer helper `scripts/fetch_models.sh` writes to `.model-cache/`, never into APK assets.

The wake-word asset is optional. `scripts/fetch_wakeword.sh` requires an independently verified
SHA-256 in `JARVIS_WAKEWORD_SHA256`, writes a sidecar digest, and the runtime refuses to load the
model until that digest matches.

## Security

High-risk tools require a one-shot biometric/device-credential unlock plus voice confirmation.
Sensitive reads require voice confirmation. The persistent kill switch is exposed in Settings and
in the foreground-service notification. Boot never automatically starts microphone capture.

## Static release gate

`bash tools/run-all.sh` performs source-only release validation. It does not execute the Android app or any downloaded native/model artifact.
