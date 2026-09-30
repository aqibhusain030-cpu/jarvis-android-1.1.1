# Compute providers

Provider adapters stay separate from the training runtime.

Prototype targets:
- Kaggle
- Google Colab

Use them for smoke tests, architecture experiments, data processing and small-model training.

Sponsored or research targets:
- Google Cloud research credits
- Microsoft Azure research programs
- University/HPC allocations
- NVIDIA research/academic opportunities

Production-scale targets:
- GCP GPU clusters
- Azure GPU clusters
- university/HPC clusters
- dedicated multi-node GPU infrastructure

Adapters must never hard-code credentials or silently create billable resources.

The workload definition is canonical. Providers only supply compute and storage bindings.
