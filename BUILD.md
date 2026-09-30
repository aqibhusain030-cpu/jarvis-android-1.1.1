# JARVIS Android build and model supply chain

## Model supply chain

Model weights are never trusted by filename alone.

The production runtime uses the immutable asset catalog:

`app/src/main/assets/models/manifest.v1.json`

Every catalog entry requires:

- provider and model ID
- immutable 40-character revision
- exact filename and format
- exact byte size
- SHA-256 digest
- license and source URL
- immutable download URL
- backend/precision/quantization
- minimum Android hardware requirements
- verification metadata

The runtime sequence is:

`manifest parse → schema validation → hardware check → download → byte-count check → SHA-256 → atomic install → final SHA-256 → native load`

A model that fails any stage is rejected and is never passed to llama.cpp or whisper.cpp.

Large model weights are intentionally not stored in Git. They are downloaded to app-private storage only through the pinned manifest.

## Current Android assets

- Qwen2.5-3B-Instruct Q4_K_M, llama.cpp backend
- Whisper.cpp small GGML model

The manifest contains the authoritative revisions and digests. Do not replace `resolve/<revision>/...` with `resolve/main/...`, and do not change a digest without independently verifying the upstream artifact.

## Updating a model

1. Select the exact upstream model revision.
2. Record the exact file size and SHA-256.
3. Record the license and source.
4. Update `manifest.v1.json`.
5. Update or add regression tests.
6. Run all CI jobs.
7. Verify the model on a real target device before release.

Never commit model weights to the source repository merely to avoid the verification process.


## Immutable model supply chain

Model weights are never trusted by filename alone. The production runtime uses `app/src/main/assets/models/manifest.v1.json`. Every asset requires an exact provider/model ID, immutable 40-hex revision, format, filename, byte size, SHA-256, license, source URL, pinned download URL, backend, precision/quantization, verification metadata, and minimum hardware.

Runtime sequence: `manifest parse → schema validation → hardware check → download → byte-count check → SHA-256 → atomic install → final SHA-256 → native load`.

Large model weights are intentionally excluded from Git. Never replace a pinned `/resolve/<revision>/...` URL with `/resolve/main/...`, and never change a digest without independently verifying the upstream artifact.
