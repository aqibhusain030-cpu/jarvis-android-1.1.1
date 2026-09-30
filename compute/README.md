# JARVIS Compute Infrastructure

Provider-neutral compute layer for large-model research.

Goals:
- Keep model and training orchestration independent of a cloud vendor.
- Use Google Cloud Storage as the durable artifact layer when configured.
- Support free or academic GPU environments for development and validation.
- Scale the same workload definition to multi-GPU and multi-node environments.
- Never place credentials, service-account JSON, API keys, or private model tokens in the repository.

Architecture:
compute workload -> provider -> distributed runtime -> object storage
Providers: local, Kaggle, Colab, GCP, Azure, HPC.
Storage: datasets, checkpoints, model shards, evaluations, manifests.

The Android application remains the operator and capability authority. A remote model can propose text or tool intents, but must not bypass Android policy, permissions, authorization, rate limits, confirmation, or the kill switch.

Free notebook GPUs are useful for validation and small models. They are not a substitute for a distributed cluster for genuine 2-trillion-parameter training.

Before multi-node 2T work, prove the lifecycle:
train -> checkpoint -> terminate -> resume -> evaluate -> export -> serve.

GCS configuration is supplied outside Git:
- JARVIS_GCS_BUCKET
- JARVIS_GCS_PREFIX
- GOOGLE_APPLICATION_CREDENTIALS when applicable

Never commit credentials.

Provider lifecycle:
1. validate environment
2. identify or provision compute
3. stage immutable artifacts
4. launch workload
5. collect logs and metrics
6. checkpoint durably
7. recover and resume
8. publish evaluation artifacts
9. tear down ephemeral resources
