#!/usr/bin/env python3
"""Fail-closed architecture budget audit for the JARVIS 2T MoE target.

This is a budget audit, not a claim that trained weights exist. Exact total
parameters require the instantiated implementation and must be audited before
training.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
CONFIG = ROOT / "model" / "2t" / "config.json"


def main() -> int:
    cfg = json.loads(CONFIG.read_text(encoding="utf-8"))
    target = int(cfg["target_total_parameters"])
    hidden = int(cfg["hidden_size"])
    experts = int(cfg["num_routed_experts"])
    expert_intermediate = int(cfg["expert_intermediate_size"])
    layers = int(cfg["num_hidden_layers"])
    dense_layers = int(cfg["num_dense_layers"])

    if target != 2_000_000_000_000:
        raise SystemExit("FAIL: target_total_parameters must remain exactly 2T.")

    routed_layers = layers - dense_layers
    if routed_layers <= 0:
        raise SystemExit("FAIL: routed layer count is invalid.")

    # SwiGLU-style expert budget: gate, up, and down projections.
    expert_mlp_params = 3 * hidden * expert_intermediate
    routed_expert_budget = expert_mlp_params * experts * routed_layers
    shared_expert_budget = expert_mlp_params * int(cfg["num_shared_experts"]) * routed_layers
    residual = target - routed_expert_budget - shared_expert_budget

    print(f"target_parameters={target}")
    print(f"routed_expert_budget={routed_expert_budget}")
    print(f"shared_expert_budget={shared_expert_budget}")
    print(f"remaining_budget_for_embeddings_attention_norms={residual}")

    if residual <= 0:
        raise SystemExit("FAIL: expert budget alone exceeds the 2T target.")

    print(
        "PASS: architecture budget is internally positive. "
        "An instantiated-model exact parameter audit is still required before training."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
