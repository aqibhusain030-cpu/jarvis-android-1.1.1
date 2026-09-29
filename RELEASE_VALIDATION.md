# JARVIS Android 1.1.1 Release Validation

## Applied fixes

- Corrected the wake-word implementation to use the openWakeWord three-stage ONNX pipeline.
- Added verified-digest provisioning for `melspectrogram.onnx`, `embedding_model.onnx`, and `hey_jarvis_v0.1.onnx`.
- Fixed the Kotlin wake-word regex escape.
- Fixed `jarvis_native` JNI library loading and the Llama generate/cancel signatures.
- Fixed Whisper JNI signatures, thread propagation, and PCM16 -> float conversion required by `whisper_full`.
- Removed duplicate LLM manifest/checksum declarations.
- Added strict tool parameter-schema validation at the Kotlin dispatcher boundary.
- Preserved nested JSON values in the Python -> Kotlin bridge.
- Serialized high-risk authorization so concurrent requests cannot invalidate one another's confirmation state.
- Fixed persisted goal resume to retrieve paused/non-terminal goals by ID.
- Cleaned up audio/wake-word/STT/brain resources during service destruction.
- Bumped Android version to 1.1.1.

## What was statically verified in this environment

- Python AST compilation for all Python sources.
- Bash syntax for repository shell scripts.
- Kotlin source-level checks for known duplicate declarations, invalid `\s` escapes, and JNI contract consistency.
- Manifest/build configuration consistency.
- Wake-word model asset naming and script/provisioner agreement.
- Release archive can be reproduced from source without embedding third-party wake-word weights.

## What was not runtime-verified here

A real Android build and physical-device test could not be executed in this environment because the Android SDK/ADB and the pinned Gradle distribution were not locally available. The Gradle wrapper therefore could not complete dependency/build resolution without external network access.

This is deliberately recorded as **not verified**, rather than pretending a static audit is a device test. Humanity has produced enough CI badges that say "green" while nobody actually ran the thing.

## Required release-gate tests on a real arm64 Android device

1. Build `assembleRelease` with the pinned JDK/SDK/NDK/CMake matrix.
2. Run unit + instrumented tests.
3. Provision all three wake-word ONNX assets with independently verified SHA-256 values.
4. Exercise continuous 16 kHz capture -> wake-word -> VAD -> Whisper -> LLM -> tool -> confirmation -> TTS.
5. Test concurrent high-risk tool requests and process death/restart.
6. Verify kill switch blocks new high-risk work and survives process restart.
7. Measure wake-word false accepts/rejects in quiet, speech, music, and background-noise conditions.
8. Validate memory/thermal behavior during sustained local LLM + wake-word operation.
