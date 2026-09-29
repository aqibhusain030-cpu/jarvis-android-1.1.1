# JARVIS Architecture

```
BODY (Kotlin / Android)
  UI (Compose) · user-started microphone ForegroundService · NotificationListener
  WakeWordDetector (ONNX) · AudioCapture · VAD · TTS · STT bridge
  ToolRegistry → ToolDispatcher → tools/impl/*
  Room (memories, messages, goals) · ModelManager · SecurityUnlockManager

BRIDGE
  BrainBridge.kt ↔ PythonRuntime (Chaquopy) ↔ tools_bridge.py
  LlamaJNI / WhisperJNI ↔ native llama.cpp / whisper.cpp

BRAIN (Python 3.10)
  brain.respond / plan_goal
  language · memory · planner · executor · reflector · prompts/*
```

Every tool call follows:
`kill switch → registry → policy → permission → network policy → rate limit →
one-shot biometric for high-risk → fresh voice confirmation → timeout → audit`.

The microphone foreground service is **not** started from BOOT_COMPLETED. It is explicitly
started by the visible application after microphone permission has been granted.
