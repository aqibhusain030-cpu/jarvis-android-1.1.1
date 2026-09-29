# JARVIS Android Post-Remediation Static Assessment

Release: 1.1.1 / versionCode 3
Base evidence archive SHA-256: `03834312450a6f3f5eb6ada86e095d4e226374d4a6e13d439d5b964b144109ec`
Mode: source/static validation; Android application execution and physical-device validation were not performed.

## Remediation status

The release was re-audited against the high-priority recommendations. Critical compile/integration defects found in the 1.1.0 source package were corrected.

| Control | Result |
|---|---|
| Correct openWakeWord three-stage ONNX pipeline | PASS (source contract) |
| Explicit SHA-256 provisioning for all wake-word ONNX assets | PASS |
| Kotlin wake-word regex correctness | PASS |
| Llama Kotlin/JNI library + signature alignment | PASS |
| Whisper Kotlin/JNI signature alignment | PASS |
| Whisper PCM16 -> float conversion | PASS |
| Duplicate LLM manifest/checksum declarations removed | PASS |
| Strict Python-to-Kotlin tool parameter validation | PASS |
| Nested tool JSON preserved across bridge | PASS |
| Serialized high-risk confirmation/unlock path | PASS |
| Persisted non-terminal goal resume lookup | PASS |
| Goal consecutive-failure circuit breaker | PASS |
| Service audio/voice resource cleanup | PASS |
| Versioned release metadata | PASS |

## Static gates

- Python AST parsing: PASS
- JSON parsing: PASS
- XML parsing: PASS
- Bash syntax (`bash -n`): PASS
- Existing repository security/manifest gates: PASS
- New source contract gate: PASS

## Not claimed

A successful Android/NDK build, instrumented test run, wake-word inference, LLM inference, physical device behavior, thermal/memory measurements, permission behavior, network behavior, or commercial-license clearance has **not** been claimed. The environment used for this audit did not provide an Android SDK/ADB installation and could not download the pinned Gradle distribution.

## Third-party licensing

openWakeWord's upstream project describes the runtime architecture as preprocessing, shared embedding extraction, and wake-word classification. The project's README states that included pretrained models are CC BY-NC-SA 4.0; a June 2026 upstream issue remains open regarding licensing clarification for the shared feature-extraction models. This release therefore keeps all wake-word model digests as explicit provisioning inputs rather than silently bundling or assuming commercial rights.
