# JARVIS security model

## Authorization

Every tool call passes through `ToolDispatcher` and `SecurityPolicy`. High-risk actions require:
1. a fresh biometric/device-credential unlock performed in the foreground Settings UI;
2. a per-request confirmation where policy requires it;
3. permission checks, rate limiting, timeout and audit metadata.

The high-risk unlock token is one-shot and memory-only.

## Kill switch

The emergency stop is persistent. It is available in Settings and as an action in the foreground notification.
The service is `START_NOT_STICKY` and never self-starts from BOOT_COMPLETED.

## Network

`http_get`, `http_post`, and model download paths use HTTPS and explicit host/revision policy.
`open_url` is separated from app-side HTTP and rejects obvious local/private targets.

## Sensitive data

Clipboard, notifications, contacts, calendar and location reads require explicit tool authorization.
Raw voice transcripts are not written to operational logs. Audit logs contain metadata, not payloads.

## Models and native dependencies

Default LLM/STT assets use immutable Hugging Face revisions plus SHA-256 verification.
Native dependencies are pinned in `third_party/LOCKFILE`.
The wake-word pretrained model is intentionally excluded from automatic provisioning until an
independently verified digest is supplied because the upstream release API does not expose one.
