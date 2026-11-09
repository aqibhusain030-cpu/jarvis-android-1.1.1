# JARVIS Production Remediation Status

Release posture: **hardened source release / build candidate**.

## Remediated controls

- Per-request, single-use voice confirmation state with timeout and cancellation.
- High-risk actions require a separate Android BiometricPrompt / device-credential unlock token.
- Central `SecurityPolicy` is authoritative over `ToolSpec` metadata.
- Sensitive data reads (clipboard, notifications, contacts, location, calendar) require confirmation.
- Browser/network destinations are constrained by HTTPS and private/local-address checks.
- File operations use canonical-path containment checks.
- Emergency kill switch is persistent, reachable from Settings and the foreground notification, and prevents service restart while active.
- Microphone foreground service no longer self-starts from `BOOT_COMPLETED`.
- Manifest permission surface is reduced; broad all-files/package-visibility/accessibility/background-location permissions are removed.
- Target/compile SDK is API 36.
- Exact-alarm scheduling checks `canScheduleExactAlarms()`.
- Whisper JNI rejects odd PCM byte counts and oversized input before copying.
- Raw transcript logging is removed from the normal voice path.
- Failed initialization in security-sensitive Python binding paths is surfaced rather than silently ignored.
- Release lint is blocking.
- Model downloads use immutable URLs, explicit SHA-256 verification, bounded redirects, size limits and atomic replacement.
- Native sources are pinned to immutable Git revisions.
- Gradle distribution URL and SHA-256 are pinned.
- Third-party licensing distinguishes openWakeWord source code from pretrained model licensing.

## Deliberate non-bundled artifacts

Large model files and native third-party source trees are not committed to this source release. The build fetches them through pinned/verified paths. The openWakeWord pretrained model is not bundled because its upstream model license differs from the source-code license and may not be compatible with unrestricted commercial distribution.

## Verification boundary

Static parse, manifest, configuration, shell-syntax, secret-pattern and release-policy checks are performed during packaging. A full Gradle/NDK build, emulator/device execution, model inference test, live network integration test, and third-party malware reputation lookup are **not** claimed here because the release workflow is being generated under a static/no-execution requirement.


## User-facing no-Termux path (native Android)

- The Home screen now downloads the pinned Qwen 3B and Whisper Small models through the app's own ModelManager; the downloader verifies the immutable artifact SHA-256 before activation.
- A foreground, one-utterance Talk now path is available after the models are installed. It uses Android microphone permission, the app's native VAD/Whisper/Llama/TTS chain, and stops after the reply. It does not require Termux or wake-word weights.
- Continuous wake-word listening remains a separate optional mode. The upstream openWakeWord pretrained weights are CC BY-NC-SA 4.0, so they are not bundled or silently downloaded for a business-oriented build. A separately licensed, digest-verified model is required for that mode.
- The microphone foreground service remains explicitly user-started. Android background-start restrictions mean this release does not claim to silently start microphone capture at boot.
