# Release Checklist

## Required before production distribution

- [ ] Install JDK 17 and Android SDK/NDK/CMake versions declared by `BUILD.md`.
- [ ] Run `bash tools/run-all.sh` in a disposable CI/development checkout.
- [ ] Build pinned llama.cpp and whisper.cpp revisions successfully.
- [ ] Run JVM unit tests and Android instrumented tests.
- [ ] Run release lint with `abortOnError=true`.
- [ ] Populate/verify the licensed wake-word asset if wake-word functionality is enabled.
- [ ] Verify model downloads against the pinned SHA-256 values.
- [ ] Configure application signing through an external secret store.
- [ ] Verify Android 16 behavior on representative Samsung and AOSP devices.
- [ ] Review Play Console declarations for every remaining restricted permission.
- [ ] Perform a device-level privacy review for notification, contacts, clipboard, location and microphone access.
- [ ] Re-run the forensic audit on the merged release branch and issue a new v3 evidence summary.
