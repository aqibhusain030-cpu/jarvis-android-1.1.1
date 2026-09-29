# Apple Core AI adoption in JARVIS

JARVIS does not depend on Apple's Core AI runtime because the production target is Android. The Apple repositories are used as architecture and model-engineering references.

## Adopted principles

- Model registry with explicit backend, precision, compression and capability metadata.
- Export/optimization is separated from device runtime.
- Deterministic model provenance and SHA-256 verification remain mandatory.
- Tool calls are deterministic and duplicate calls are suppressed within a response.
- Optional vision models are treated as independently provisioned artifacts.
- Train-time complexity should be removed from inference artifacts where the model format permits it.

## FastViT

FastViT's RepMixer and structural reparameterization demonstrate a useful deployment principle: train with richer branches when helpful, then produce a simpler inference graph. JARVIS does not copy the Core ML package into Android. An Android vision artifact must be exported to a runtime-supported format such as ONNX and independently validated on the target device.

The FastViT reference implementation reports mobile latency measurements on iPhone 12 Pro and provides Core ML packages. Those measurements must not be reused as Android performance claims.

## Core AI Models / Torch / Optimization

The Apple stack separates model authoring/export, optimization and runtime. JARVIS mirrors that separation:

1. tools/optimize_models.py records a reproducible deployment profile and validates ONNX artifacts.
2. ModelManifest records the runtime backend and compression/precision contract.
3. ModelManager and checksum verification remain the runtime trust boundary.
4. Android inference continues to use llama.cpp, whisper.cpp and ONNX Runtime.

## Future vision artifact contract

A FastViT-derived Android artifact should provide:
- ONNX format
- fixed input contract
- independently published SHA-256
- benchmark data captured on the actual Android target
- license/provenance record
- no dependency on Apple .mlpackage or .aimodel runtimes

No unverified model is added to the production manifest.
