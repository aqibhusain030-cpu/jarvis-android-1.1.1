# JARVIS AOSP Alignment

This document records which AOSP platform practices are intentionally adopted by the JARVIS Android application and which are excluded because JARVIS is a third-party APK rather than an AOSP system image.

## Adopted now

### AppFunctions integration boundary

JARVIS is architected to expose a narrow, read-only AppFunctions surface when the project upgrades to the required AndroidX AppFunctions toolchain. The current verified build intentionally does not ship the dependency because the current AGP 8.10.1/API 36 toolchain cannot satisfy the AppFunctions alpha10+ requirement for AGP 9.1+ and compileSdk 37. The planned functions are `getJarvisStatus`, `listGoals`, and `getGoal`; high-risk or mutating actions remain behind JARVIS security controls.

## Adopted as engineering practice

### Trade Federation principles

Tradefed is a host-side Android test harness for instrumentation, UIAutomator, native/gtest, host JUnit, recovery, reporting, and scalable device execution. JARVIS does not embed Tradefed in the APK. Instead, JARVIS CI keeps the same separation of concerns:

- host orchestration
- deterministic emulator configuration
- device-side instrumentation
- native dependency verification
- static/security gates
- release artifact publication

A future full-device lab can add Tradefed/CTS/VTS without changing the application runtime.

### Android CLI

The Android CLI is treated as a developer/host tool only. It is appropriate for device inspection, screenshots, UI layout inspection, SDK management, app installation, emulator management, documentation lookup, and AppFunctions smoke testing.

It is intentionally not bundled into the APK and is not a Node/npm dependency.

### Git and Repo

JARVIS remains a normal single Git repository. Repo is not introduced merely for appearance. Repo becomes appropriate only if JARVIS expands into an AOSP/device tree containing multiple coordinated Git repositories.

The project should use Git topic branches, reviewable commits, clean diffs, reproducible CI, and signed release tags where signing infrastructure is available.

## Deliberately not adopted in the APK

### HAL

AOSP HALs are system/vendor interfaces between framework services and device-specific hardware implementations. JARVIS is a normal application and should access approved Android framework APIs instead of implementing or pretending to be a HAL.

If JARVIS later becomes a privileged/system application in a custom AOSP build, hardware-specific functionality should use AIDL HAL interfaces rather than introducing new HIDL interfaces for Android 13+ work.

### SUPL certificate HAL

The referenced `SuplCertificateInterface` is a legacy AGPS/SUPL certificate-management HAL structure from `hardware/libhardware/gps.h`. It has no application-level JARVIS role and is not copied into the APK.

### Kernel/Kleaf/Bazel

Kleaf/Bazel kernel builds are for Android kernel source trees and AOSP system-image development. They are not part of this APK build. Android 13+ kernels should use Kleaf/Bazel where supported; legacy `build.sh` is not a JARVIS application build mechanism.

### RB3 / DB845c

RB3 (DB845c) is a Qualcomm development-board/AOSP target. Its vendor packages, bootloader, flashing flow, UFS/SD boot paths, and GKI kernel process are relevant only to a dedicated AOSP hardware lab. They are not runtime dependencies for JARVIS on consumer Android hardware.

## Validation model

JARVIS CI should preserve a layered model:

1. source/static/security gates
2. wrapper and metadata validation
3. unit tests
4. lint
5. debug/release assembly
6. native dependency checks
7. Android emulator instrumentation
8. AppFunctions registration smoke test on an AppFunctions-capable API level
9. release artifact capture

This separation keeps platform-build concerns out of the APK while still borrowing the useful rigor of AOSP's development and testing ecosystem.
