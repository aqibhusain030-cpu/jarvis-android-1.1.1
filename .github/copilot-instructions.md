# Copilot instructions for JARVIS

## Overview

JARVIS is an offline-first Android voice assistant that runs as a real APK and keeps its AI pipeline on-device. The project is built around three layers:

- Kotlin/Compose Android runtime in `app/src/main/kotlin/com/jarvis` for UI, foreground services, security gates, tool execution, memory, and model management.
- Python brain in `app/src/main/python` for planning, memory access, and response generation, bridged via Chaquopy (`BrainBridge`, `PythonRuntime`).
- Native JNI layer in `app/src/main/cpp` for llama.cpp and whisper.cpp workloads; native builds are pinned and validated by scripts under `scripts/` and `tools/`.

The key security model is explicit and defense-in-depth: no microphone capture starts from `BOOT_COMPLETED`, high-risk tools require biometric/device-credential unlock plus fresh voice confirmation, and network access is deny-by-default unless the tool explicitly allows it.

## Environment and prerequisites

Use the project’s pinned toolchain and SDK defaults:

- JDK 17
- Android SDK with API 36/platforms and Android build-tools
- NDK 28.2.13676358
- CMake 3.22.1
- Python 3.10 for Chaquopy
- Gradle wrapper is checksum-verified and intentionally downloads Gradle 8.11.1 only from `services.gradle.org`

Typical setup:

```bash
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/28.2.13676358}"
./gradlew :app:tasks --all
```

If native libraries are not already built, install prerequisites and build the pinned native dependencies:

```bash
sdkmanager "platforms;android-36" "build-tools;35.0.0" "ndk;28.2.13676358" "cmake;3.22.1"
bash scripts/build_llama.sh
bash scripts/build_whisper.sh
```

## Build, test, and lint commands

Run the repo’s normal verification flow for Android targets:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Run one specific JVM test by class or method name:

```bash
./gradlew :app:testDebugUnitTest --tests "com.jarvis.<Package>.<TestClass>"
./gradlew :app:testDebugUnitTest --tests "com.jarvis.<Package>.<TestClass>.<methodName>"
```

For the emulator/instrumented path:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Additional repo-level validation used by CI and release checks:

```bash
bash tools/run-all.sh
bash tools/verify_native_libs.sh
```

## High-level architecture

### 1. Android app runtime

The app is configured as a single Android application with Compose UI and Hilt DI. Important runtime entry points:

- `JarvisApplication` initializes notification channels and starts the Python runtime / app state in a controlled way.
- `MainActivity` hosts the app UI and navigation.
- Foreground service code in `app/src/main/kotlin/com/jarvis/service` controls the microphone flow and wake-word lifecycle.
- `WakeWordDetector` is responsible for the on-device always-listening trigger path.

### 2. Brain and tool execution pipeline

The app intentionally splits the reasoning layer from the Android runtime:

- `BrainBridge` serializes user context into a Python payload, calls into the Chaquopy runtime, and deserializes the structured reply.
- `PythonRuntime` and the Python files under `app/src/main/python` house the planning/respond logic.
- `ToolRegistry` is the central registry for capability exposure; tools are discovered and dispatched through a policy-enforced path.
- Security-sensitive actions flow through the project’s gate sequence: kill switch -> registry -> policy -> permission -> network policy -> rate limit -> biometric unlock -> voice confirmation.

### 3. Persistent state and model management

- `MemoryStore` and Room entities under `app/src/main/kotlin/com/jarvis/memory` are the durable memory layer for messages, goals, and lessons.
- `ModelManager` and `ModelDownloader` handle pinned model entries and checksum verification before activation.
- Model and wake-word assets are not treated as optional source assets; they are fetched and validated by scripts and runtime checks.

### 4. Native binary and CI contract

Native sources are fetched only at the immutable commits recorded in `third_party/LOCKFILE`. CI enforces build prerequisites, native verification, static release gates, and packaging checks. Treat native changes as security-sensitive and keep them consistent with the pinned build scripts.

## Key conventions and repo-specific patterns

- Keep the microphone Foreground Service user-started; do not auto-start capture from boot or background triggers.
- Maintain the default-deny security posture: network is off unless a specific tool intentionally opts in.
- Preserve the audit trail and kill-switch semantics for tool usage; security-sensitive tooling is not just UI-level gating.
- Prefer extending the existing tool/registry pattern rather than introducing ad hoc direct calls from unrelated classes.
- Treat model downloads as checksum-verified assets, not arbitrary network content.
- Follow the existing Dagger Hilt + Compose + Room + DataStore pattern used across the app (`@AndroidEntryPoint`, `@Singleton`, `@HiltViewModel`, Room entities/DAOs, config repository style).
- Keep the offline-first behavior unchanged; any feature with cloud dependence should be explicit and opt-in.
- When changing release or packaging behaviors, check the repo’s validation scripts under `scripts/` and `tools/` before concluding work.

## Working expectations for this repo

- Changes often touch both Kotlin and Python or native build scripts; verify the full control path, not just the UI surface.
- Security and permission changes should be checked against the project’s documented enforcement rules and release-validation scripts.
- Native libs and model artifacts are intentionally pinned and verified; do not bypass the lockfile/checksum flow for “quick fixes”.
