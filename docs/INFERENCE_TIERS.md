# JARVIS Inference Tiers

## Tier 1: Android edge

The phone remains usable without a network dependency:

- Whisper.cpp STT
- Llama.cpp local LLM
- deterministic voice-command routing
- FRIDAY capability policy
- ToolDispatcher authorization
- Android app/device operations
- Room/MemoryStore context

The current locked edge LLM is Qwen2.5-3B-Instruct Q4_K_M. Its SHA-256 is pinned in ModelManifest and verified before loading.

## Tier 2: LAN command grid

The architecture is intentionally provider-neutral. A LAN inference service can expose an OpenAI-compatible endpoint such as vLLM. Tool definitions must still be passed through JARVIS/FRIDAY policy before execution.

vLLM supports named and automatic tool calling, structured tool arguments, continuous batching, quantization, tensor parallelism, pipeline parallelism and expert parallelism.

## Tier 3: distributed reasoning

Large dense or MoE models can be hosted outside the phone. JARVIS should treat them as inference providers, not authorities.

For vLLM deployments:

- single-node multi-GPU: tensor parallelism
- multi-node: tensor + pipeline parallelism
- MoE: expert parallelism where appropriate
- quantized weights/KV cache where validated
- tool calls return to the Android policy plane before execution

A 2T-class model is therefore a deployment target, not an artifact that can be created by this Android repository. The repository provides the control plane and provider boundary; trained weights and the required compute cluster remain external assets.

## Security boundary

Remote models never receive direct Android privileges.

The authoritative execution path is:

voice/text -> intent/router -> FRIDAY decision -> capability policy -> ToolDispatcher/capability backend -> Android operation -> audit

A remote model can propose a tool call. It cannot bypass the local policy, confirmation, permission, rate-limit, kill-switch, or audit layers.
