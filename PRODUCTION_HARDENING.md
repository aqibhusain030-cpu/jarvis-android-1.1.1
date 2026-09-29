# JARVIS Production Hardening Release

## Security boundary changes

- Confirmation input is active only for a fresh pending request and is cleared race-safely.
- Every high-risk operation requires a one-shot biometric/device-credential unlock token.
- Network access is deny-by-default, with strict allowlisting for app-side HTTP and public-HTTPS validation for browser launches.
- File and model paths are validated before access.
- Native Whisper PCM buffers reject odd-length input and oversized buffers.
- The emergency kill switch persists across process/service restarts.
- Boot no longer starts the microphone foreground service.
- Release lint is blocking.
- Restricted/broad Android permissions were removed from the manifest.
- Model and native-source supply chain inputs are pinned and hash-checked.

## Build posture

The archive includes a secure, checksum-verified Gradle bootstrap in `gradlew` and `gradlew.bat`.
It intentionally does not ship a Gradle wrapper JAR. The bootstrap reads the pinned
`distributionUrl` and `distributionSha256Sum` from `gradle/wrapper/gradle-wrapper.properties`,
downloads only from `services.gradle.org` over HTTPS, verifies SHA-256, extracts to a dedicated
Gradle cache, and then invokes the distribution's `gradle` executable.

## Model posture

No multi-hundred-megabyte model weights are embedded in this source release. Model downloads
are explicit and verified before activation. The wake-word helper requires an independently
verified SHA-256 supplied through `JARVIS_WAKEWORD_SHA256`; this is deliberate because the
upstream release API did not expose a cryptographic asset digest in the evidence used for this build.

## Release validation

Run `bash tools/run-all.sh` for static release gates. Full Gradle/Android/native compilation and
physical-device testing are not performed by this packaging step.
