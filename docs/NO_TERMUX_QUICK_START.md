# JARVIS Android — no-Termux quick start

This repository contains the native Android build. It does not require Termux to run the local voice pipeline.

## 1. Install the APK

Download the latest verified jarvis-debug-apk artifact from the GitHub Actions run for branch fix/no-termux-voice-setup. Debug APKs are for testing, not Play Store publication. Verify the accompanying SHA-256 file before installation.

## 2. Grant permission

Open JARVIS and grant microphone permission when prompted. The app starts microphone capture only after you press Talk now or explicitly start a supported foreground service.

## 3. Download the local models

On the Home screen, press Download offline models. The app downloads two immutable model files (about 2.5 GB combined) into its own private storage and checks each SHA-256 digest before marking it ready. Use Wi-Fi and keep enough free storage for the download and temporary files.

## 4. Use voice

Press Talk now, wait for the listening notification, speak a command, and allow JARVIS to finish the response. Speech recognition and language-model inference run locally through Whisper and llama.cpp; the answer is spoken by the Android TTS bridge. The one-shot microphone service stops after the reply.

## Wake phrase limitation

The source includes an optional continuous openWakeWord detector. Its upstream pretrained weights are licensed CC BY-NC-SA 4.0, so this repository deliberately does not package them for a business/commercial distribution. Continuous Hey JARVIS mode requires a separately licensed and SHA-256-verified wake-word model. Do not use the existing non-commercial weights in a commercial deployment without the required rights.

## Android background behavior

Android requires an eligible microphone foreground service to be started while the app is visible and with microphone permission granted. JARVIS shows an ongoing notification with stop and emergency-stop controls. It does not silently restart microphone recording at boot. This is an operating-system restriction, not a Termux dependency.

## Cloud / agent council

The private jarvis-android-render-ci repository contains the cloud API, multi-agent council and browser HUD. Those features require a deployed HTTPS service and configured model-provider credentials. They are separate from the local, no-Termux voice path until the cloud API is explicitly connected to this native app.
