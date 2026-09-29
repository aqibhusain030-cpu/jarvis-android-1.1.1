# Wake-word model provisioning

The JARVIS source package does **not** embed openWakeWord model weights.

The runtime expects these three ONNX assets under the app-private `models/wakeword/` directory:

- `melspectrogram.onnx`
- `embedding_model.onnx`
- `hey_jarvis_v0.1.onnx`

Use `scripts/fetch_wakeword.sh` with three independently verified SHA-256 values:

```bash
export JARVIS_WAKEWORD_MELSPEC_SHA256="<64 hex>"
export JARVIS_WAKEWORD_EMBEDDING_SHA256="<64 hex>"
export JARVIS_WAKEWORD_SHA256="<64 hex>"
./scripts/fetch_wakeword.sh
```

The script refuses missing/invalid digests, downloads over HTTPS, verifies each temporary file, and writes a matching `.sha256` sidecar.

openWakeWord's current repository describes the architecture as a preprocessing mel-spectrogram model, a shared embedding backbone, and a wake-word classification model.

Licensing must be reviewed before commercial distribution. The project's README states that included pretrained models are CC BY-NC-SA 4.0, while a June 2026 upstream issue specifically asks for clarification about the licensing of the shared feature-extraction models.
