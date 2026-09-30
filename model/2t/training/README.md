# JARVIS 2T Training Pipeline

This directory defines the training boundary for the JARVIS 2T sparse-MoE model.

## Required stages

1. Architecture instantiation and exact parameter audit.
2. Tokenizer freeze and vocabulary audit.
3. Licensed dataset ingestion and deduplication.
4. Distributed base pre-training.
5. Deterministic checkpoint and resume validation.
6. Instruction and Android tool-use tuning.
7. Reasoning and reliability evaluation.
8. JARVIS-specific adapter or finetuning.
9. Checkpoint integrity and deployment attestation.
10. Distributed inference serving.

## Non-negotiable gate

Training must not start from model/2t/config.json until an implementation instantiates the configuration and produces an exact parameter count matching the declared target.

The repository intentionally contains the architecture contract and integration code, not fabricated 2T weights.

## Runtime safety

The training corpus may contain examples of security testing and Android administration, but runtime authorization remains a separate Android control-plane concern. Training a capable model does not grant the model permission to bypass Android or JARVIS policy.