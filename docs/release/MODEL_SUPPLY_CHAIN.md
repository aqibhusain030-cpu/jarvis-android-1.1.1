# JARVIS Model Supply Chain

JARVIS treats model weights as release artifacts, not arbitrary downloads.

## Trust model

An asset is accepted only when its bundled manifest entry has a supported provider, exact 40-hex revision, immutable source/download URLs, supported format, exact positive byte size, 64-hex SHA-256, license metadata, verified status, and minimum hardware requirements. The downloaded file must then match both the declared size and SHA-256 before installation.

## Atomic installation

Downloads use a `.part` file. A failed or unverified download cannot replace an existing verified model.

## Runtime boundary

`ModelManager` is the single model catalog interface. `LocalLlmEngine` and `SpeechRecognizerBridge` obtain assets from it and refuse native initialization when verification fails.

## Release blockers

Placeholder hashes, mutable branch URLs, missing size/license, unsupported formats, unverified metadata, checksum mismatches, size mismatches, and unsupported hardware are release-blocking.

Wake-word assets remain on their separate sidecar verification path until authoritative hashes for the complete upstream bundle are available.
