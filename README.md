# JARVIS — Android On-Device Voice Assistant

Fully offline-first, trilingual (Hindi / English / Hinglish) on-device AI
assistant for Android 12+. Runs as a real APK. No Termux. No mandatory cloud.

## What it does

- User-started always-listening wake word ("Hey JARVIS") via a microphone Foreground Service.
- Speech in Hindi, English, or code-switched Hinglish — answered in kind.
- On-device LLM (llama.cpp / Qwen2.5) and STT (whisper.cpp / ggml-small).
- 26 phone-control tools (SMS, calls, camera, files, notifications, weather…).
- Autonomous goals that run for minutes, hours, or days.
- Persistent memory across reboots. Full audit log. Kill switch.

## Requirements

- Android 12 (API 31) minimum, Android 14 (API 34) target.
- arm64-v8a device with ≥ 8 GB RAM (12 GB recommended).
- ~4 GB free storage (LLM + STT + wake word models).
- JDK 17, Android Studio Hedgehog+, NDK r26+, CMake 3.22+, Python 3.10, git, curl.

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
export ANDROID_NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/26.1.10909125}"
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
2. Complete onboarding (language → permissions → models → voice → wake word → safety).
3. Say "Hey JARVIS", then "टाइम क्या है?" or "What's the battery level?".

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

## License

Apache-2.0. Third-party attributions in `app/src/main/assets/licenses/THIRD_PARTY.txt`.

## Security remediation (audit)

See [SECURITY.md](SECURITY.md). Critical control-plane defects from the static
audit (confirmation reuse, policy not enforced, path traversal, checksum
fail-open, goal retry/DONE logic, network default-deny, FGS permission gate)
have been fixed in source. Model SHA-256 digests must be pinned before download
will succeed. Full LLM↔tool wiring remains an integration milestone.
