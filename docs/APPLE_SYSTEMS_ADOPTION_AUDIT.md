# Apple systems references adopted by JARVIS

Audited references:
- WebKit/WebKit workflow run 27165737523
- apple/pkl issue 990 and current Pkl repository documentation
- apple/embedding-atlas workflow artifacts from run 36188202169 and repository documentation
- apple/pkl workflow run 36571000159
- apple/container workflow run 35799462307 and container documentation

## WebKit
The supplied workflow is a successful dependency/graph update run. WebKit's source architecture shows explicit process boundaries such as WebProcess and NetworkProcess, site isolation mechanisms, and privacy-aware resource-load statistics. JARVIS adopts the boundary principle: network access is mediated centrally by SecurityPolicy and ToolDispatcher. JARVIS does not embed WebKit or copy Apple-specific process code.

## Pkl
Issue #990 is an open feature request for a Pkl renderer, not a completed feature. The current Pkl documentation emphasizes strong configuration validation, schema-aware tooling, typed modules, and local/remote module loading. JARVIS adopts the useful part: a versioned, strict runtime configuration schema that rejects unknown keys and invalid ranges before constructing JarvisConfig. Pkl itself is not added as an Android dependency.

## Embedding Atlas
The project supports multimodal datasets, clustering, nearest-neighbor search, cross-filtering, and MCP access. Its documentation describes nearest-neighbor structures and real-time search. JARVIS adopts the local semantic-memory principle: embeddings can be searched by cosine similarity, with a lexical fallback when no verified embedding model exists. Memory remains on-device.

The supplied Embedding Atlas artifacts were independently checked for provenance: the Python distribution and Node distribution are non-expired and carry SHA-256 digests.

## Apple Container
Apple Container consumes OCI images and runs Linux containers in lightweight VMs on Apple silicon. JARVIS cannot use that runtime on Android. The portable principles are adopted for development/build isolation and reproducibility: model/tool artifacts should have explicit provenance, immutable digests, bounded capabilities, and deterministic build inputs.

## Deliberate exclusions
- No Core ML runtime dependency on Android.
- No Apple Container runtime inside the Android APK.
- No Pkl runtime dependency merely to parse config.
- No unverified embedding or vision checkpoint added to the production model manifest.
- No WebKit source copied into JARVIS.

These exclusions are intentional compatibility and supply-chain controls, not missing features.
