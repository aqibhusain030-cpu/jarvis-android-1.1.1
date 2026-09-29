# JARVIS Fixed Production Candidate

This release applies the confirmed remediation set from the forensic audit:

- race-safe, active voice confirmation requests;
- authoritative central policy and one-shot biometric/device-credential unlock;
- private/public URL validation and default-deny app-side network policy;
- sensitive-read authorization and bounded tool inputs;
- persistent reachable kill switch;
- no boot-time microphone foreground-service start;
- Android API 36 target;
- native Whisper odd-length/bounds protection;
- model SHA-256 validation before every native load;
- verified model downloads and wake-word sidecar verification;
- immutable native dependency commits;
- blocking release lint;
- restricted permission reduction;
- complete static release validation and artifact hashing.

This ZIP is a source release. It does not claim a successful Gradle/NDK build or Android runtime test in the packaging environment.
