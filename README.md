# JARVIS — Android On-Device Voice Assistant

Fully offline-first, multilingual (Hindi / English / Arabic, including common code-switching) on-device AI assistant for Android 12+. Runs as a real APK. No Termux. No mandatory cloud.

## What it does

- User-started microphone foreground service for voice commands.
- Offline speech recognition through whisper.cpp, using the multilingual `ggml-small.bin` model with automatic per-utterance language detection.
- Dedicated **Standalone speech recognition** screen: records a complete utterance, waits for silence, displays the transcript and detected language, and lets the user copy it. This path does not call Android SpeechRecognizer, keyboard dictation, the LLM, TTS, or any cloud endpoint.
- Hindi, English and Arabic transcript classification; mixed Arabic-English and Hindi-English transcripts retain their original scripts.
- On-device LLM (llama.cpp / Qwen2.5) for assistant replies.
- 26 phone-control tools (SMS, calls, camera, files, notifications, weather…).
- Autonomous goals that run for minutes, hours, or days.
- Persistent memory across reboots. Full audit log. Kill switch.

## Requirements

- Android 12 (API 31) minimum, Android SDK 36 target.
- arm64-v8a device with ≥ 8 GB RAM (12 GB recommended).
- ~4 GB free storage (LLM + STT + wake word models).
- JDK 17, Android Studio Hedgehog+, NDK 28.2.13676358, CMake 3.22+, Python 3.10, git, curl.

## Project layout

```
jarvis-android/
├── app/src/main/kotlin/com/jarvis/   # Body: service, voice, tools, UI, memory…
├── app/src/main/python/              # Brain: brain.py, planner, prompts
├── app/src/main/cpp/                 # JNI: llama_jni.cpp, whisper_jni.cpp
├── app/src/test/                     # JVM unit tests
├── app/src/androidTest/              # Instrumented tests
├── scripts/                          # build_llama, build_whisper, fetch_models…
├── config.json                       # Default agent / voice / safety config
└── .github/workflows/ci.yml
```

## Build

```bash
cd jarvis-android

# 1. Native engines (requires ANDROID_NDK_HOME)
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/28.2.13676358}"
bash scripts/build_llama.sh
bash scripts/build_whisper.sh

# 2. Optional developer prefetch cache (large files remain outside the APK)
bash scripts/fetch_models.sh

# 3. Build
./gradlew :app:assembleDebug

# 4. Install on a connected device
bash scripts/install_debug.sh
```

## First run

1. Launch JARVIS.
2. Complete onboarding and grant microphone permission when requested.
3. On Home, download **Whisper speech model only (~488 MB)** to use standalone transcription without downloading Qwen.
4. Open **Standalone speech recognition**, tap **Start recording**, speak a complete sentence, then pause naturally. VAD ends the recording after sustained silence; the Whisper model transcribes locally.
5. To use the full assistant, download both Qwen and Whisper models and use **Talk now**.

The standalone transcription screen displays the transcript only. It does not send the recognized text to the brain or speak a reply. The model is downloaded once and verified against its pinned SHA-256 before it is loaded. Audio remains in memory and is not uploaded or saved as a recording.

## Release

```bash
export JARVIS_KEYSTORE=~/keystores/jarvis.jks
export JARVIS_KEY_ALIAS=jarvis
export JARVIS_STORE_PASS=… JARVIS_KEY_PASS=…
bash scripts/sign_release.sh
```

## Tools (26)

`torch`, `battery_status`, `send_sms`, `make_call`, `contact_lookup`,
`list_notifications`, `get_location`, `set_volume`, `media_control`,
`set_alarm`, `set_timer`, `calendar_read`, `clipboard_get`, `file_read`,
`open_app`, `open_url`, `screenshot`, `take_photo`, `wifi_status`,
`bluetooth_status`, `http_get`, `save_memory`, `create_goal`, `notify`,
`system_info`, `get_weather`

High-risk tools (SMS, call, HTTP, …) require voice confirmation and are rate-limited.
`screenshot` returns a truthful `needs_media_projection` error until the user grants MediaProjection.

## Privacy

Every AI inference, memory read/write, and voice operation runs on-device.
Network calls are disabled by default and must be explicitly enabled per-tool.
The standalone transcription flow uses the phone microphone and local Whisper inference only; it does not use Android's speech-recognition provider or send audio to a server.

## License

Apache-2.0. Third-party attributions in `app/src/main/assets/licenses/THIRD_PARTY.txt`.

## Security remediation (audit)

See [SECURITY.md](SECURITY.md). Critical control-plane defects from the static
audit (confirmation reuse, policy not enforced, path traversal, checksum
fail-open, goal retry/DONE logic, network default-deny, FGS permission gate)
have been fixed in source. Model SHA-256 digests must be pinned before download
will succeed. Full LLM↔tool wiring remains an integration milestone.
