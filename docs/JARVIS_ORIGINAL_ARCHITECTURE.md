# JARVIS Original Architecture

This document defines the application-level architecture derived from the Android platform, Dynamic Partitions, AVB, compatibility, package, and Play Feature Delivery references reviewed for JARVIS.

## 1. Platform boundary

JARVIS is an Android application. It uses public Android framework/Jetpack APIs and does not implement AOSP HALs, kernel code, dynamic partitions, or AVB itself.

If JARVIS later becomes part of a custom AOSP image, those layers move into a separate platform repository.

## 2. Original JARVIS capabilities

### Core assistant
- on-device wake word
- local Whisper STT
- local LLM inference through llama.cpp
- deterministic command planning and dispatch
- immutable confirmed action plans
- local TTS
- one user confirmation immediately before execution
- Android platform permissions and authorizations remain authoritative

### Agent integration
- Android AppFunctions for narrow, typed, read-only system-agent capabilities
- future mutation functions only through explicit confirmation and existing security controls
- Android assistant/voice-role integration is a separate platform-facing phase, not a workaround for background execution limits

### Memory
- Room persistence for goals and adaptive learning
- bounded lesson fingerprints and confidence
- AppSearch is a future index for user-searchable memory, not a second source of truth
- no raw secrets or unnecessary sensitive data in indexed content

### Skill architecture
- typed ToolDispatcher capabilities
- optional feature modules only where Play Feature Delivery is actually beneficial
- large AI models/assets are content packs, not arbitrary executable code
- every downloaded model/content pack requires manifest metadata, size limits, SHA-256 integrity, compatibility bounds, and safe replacement/rollback

### Updates
- app binaries are delivered through normal Android/Play mechanisms
- optional code can use Play Feature Delivery
- model/content packs use JARVIS's own signed manifest and integrity layer
- AVB is relied upon as the device/platform trust boundary, not reimplemented inside the APK

## 3. Compatibility

JARVIS targets public SDK contracts and maintains:
- minSdk 31
- targetSdk 36
- compileSdk 36 until CI provides the required newer platform package
- source contracts for permissions, exported components, JNI, model checksums, migrations, and AppFunctions

Compatibility testing should eventually cover:
- API 31
- API 33
- API 35
- API 36
- representative Samsung/Pixel hardware

## 4. Dynamic partitions and AVB

Dynamic partitions are a device/system-image concern. JARVIS does not resize or flash partitions.

AVB protects the boot/system trust chain. JARVIS should instead verify application-managed downloaded content with cryptographic hashes and authenticated manifests. A content-pack update must be fail-closed on integrity mismatch.

## 5. Originality

JARVIS is not defined by copying an AOSP service. Its differentiator is the integration layer:

wake word -> local speech -> context/memory -> planner -> immutable action plan -> user confirmation -> tool execution -> verification/result -> learning -> voice response

The system must learn execution strategy without learning its security authority.

## 6. Future platform edition

A future JARVIS OS/device repository may contain:
- AOSP device configuration
- AIDL HALs
- vendor integration
- Kleaf/Bazel kernel build
- AVB signing
- dynamic partitions
- Tradefed/CTS/VTS
- Repo multi-repository manifest

Those components must remain outside the application repository until such a platform exists.

## 7. Sound-trigger alignment

JARVIS follows the Android sound-trigger architecture at the application boundary without pretending to be an STHAL implementation. The current wake-word engine remains an app-owned ONNX/openWakeWord pipeline using AudioRecord because a normal APK cannot replace the privileged vendor Sound Trigger HAL.

The detector now exposes an explicit lifecycle:

- UNLOADED -> READY -> LISTENING -> TRIGGERED
- LISTENING -> PAUSED on cancellation/stop
- LISTENING -> ERROR on inference/runtime failure

A successful recognition produces a bounded `WakeTriggerEvent` containing the model identifier, confidence score, and elapsed-realtime timestamp. This gives the voice loop a clean event boundary matching the conceptual Sound Trigger stack: capture -> recognition -> trigger -> handoff to speech processing.

If JARVIS later becomes a privileged/system component on a custom AOSP device, the same detector contract can be backed by the platform Sound Trigger service/ISoundTriggerHw implementation while retaining the application-level wake event contract. The APK must not implement or ship an STHAL.

### Power and privacy boundary

The wake recognizer runs only inside the explicitly user-started microphone foreground service. It does not self-start at boot, does not expose raw microphone data to external agents, and pauses/relinquishes recognition when the voice loop stops.


## 8. Universal command execution boundary

Every executable voice command follows one deterministic lifecycle:

1. Understand the request.
2. Generate an explicit action plan containing exact tools and arguments.
3. Validate the plan against the installed capability registry and Android platform prerequisites.
4. Present the exact planned effects to the user.
5. Wait for one explicit confirmation.
6. Lock the approved plan by SHA-256.
7. Execute only the locked actions in their declared order.
8. Reject any argument or tool-name change after confirmation.
9. Stop the plan on an execution error and report the actual platform/tool result.
10. Record execution outcomes for adaptive learning without granting the learning subsystem execution authority.

The Python layer is a reasoning/planning layer. It cannot execute commands. Kotlin owns the execution boundary.

## 9. Broad Android capability layer

The capability registry separates the assistant from individual Android implementations. Current capability families include intents, packages, accessibility UI control, notifications, contacts, calendar, telephony, SMS, media, camera, files, network, alarms/timers, Bluetooth, Wi-Fi, and location.

Accessibility is an optional platform integration. When the user explicitly enables the JARVIS AccessibilityService, the assistant can inspect the active UI and perform supported click, text-entry, scroll, and global navigation actions. Without that platform authorization, those actions return an actual accessibility_disabled result rather than pretending they succeeded.

AppFunctions remains represented as a future platform backend only. It is not falsely compiled into the current API 36/AGP 8.10.1 application.
