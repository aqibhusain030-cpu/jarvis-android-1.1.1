# JARVIS vision model deployment contract

## Depth Anything V2

Depth Anything V2 is a strong candidate for JARVIS spatial perception. The paper describes model sizes from 25M to 1.3B parameters and reports improved robustness and efficiency over its predecessor. JARVIS should start with a mobile-sized variant and validate it on the actual Samsung Android target before enabling it by default.

Reference: https://arxiv.org/abs/2406.09414

## Android contract

A production vision artifact must:
1. Be exported to ONNX or another Android-supported runtime format.
2. Declare exact input layout, dimensions, dtype and normalization.
3. Declare exact output semantics and dtype.
4. Carry an immutable source/revision and SHA-256 digest.
5. Have a license/provenance record.
6. Pass numerical parity tests against the reference model within an explicitly recorded tolerance.
7. Have device benchmarks for latency, peak memory and thermal behavior.
8. Be disabled by default until all of the above are verified.

The Core ML ecosystem's emphasis on explicit model interfaces and graph/export validation is adopted as a design principle, but Core ML model files are not Android runtime artifacts.

## Current state

JARVIS now has a depth-anything-v2-mobile optimization profile in tools/optimize_models.py. It describes an ONNX Runtime deployment target with FP16 conversion and a relative-depth output contract.

No Depth Anything V2 weight is added to the production ModelManifest yet. This is deliberate: a model URL without a verified digest and Android benchmark would turn a useful research reference into an unverified supply-chain dependency.

## Perception pipeline

Camera frame -> resize/normalize -> depth model -> depth map -> spatial context -> LLM/tool planner.

Depth is treated as an observation, not a command. Any action derived from visual perception must still pass JARVIS's existing tool authorization, confirmation and kill-switch controls.
