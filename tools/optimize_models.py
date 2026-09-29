#!/usr/bin/env python3
"""Portable model-production helper inspired by Apple's Core AI workflow.

This tool does not alter Android runtime code. It validates an ONNX model and
creates a reproducible optimization profile describing the intended deployment
precision/compression. Actual quantization is delegated to onnxruntime tools
when installed, so the runtime remains ONNX Runtime on Android.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path


PROFILES = {
    "fastvit-mobile": {
        "backend": "onnxruntime",
        "precision": "int8",
        "compression": "dynamic-int8",
        "input": {"layout": "NCHW", "dtype": "float32", "size": [224, 224], "channels": 3},
        "capabilities": ["vision", "image-classification"],
    },
    "vision-balanced": {
        "backend": "onnxruntime",
        "precision": "fp16",
        "compression": "fp16-cast",
        "input": {"layout": "NCHW", "dtype": "float32", "size": [224, 224], "channels": 3},
        "capabilities": ["vision"],
    },
    "llm-mobile": {
        "backend": "llama.cpp",
        "precision": "mixed-q4",
        "compression": "Q4_K_M",
        "capabilities": ["chat", "tools", "reasoning"],
    },
}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def inspect_onnx(path: Path) -> dict:
    try:
        import onnx
    except ImportError as exc:
        raise SystemExit("onnx is required for ONNX inspection: pip install onnx") from exc
    model = onnx.load(str(path))
    onnx.checker.check_model(model)
    inputs = []
    for value in model.graph.input:
        tensor = value.type.tensor_type
        shape = []
        for dim in tensor.shape.dim:
            shape.append(dim.dim_value if dim.dim_value else dim.dim_param or -1)
        inputs.append({"name": value.name, "shape": shape, "elem_type": tensor.elem_type})
    return {
        "ir_version": model.ir_version,
        "opset": max((x.version for x in model.opset_import), default=0),
        "inputs": inputs,
        "nodes": len(model.graph.node),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("model", type=Path)
    parser.add_argument("--profile", choices=sorted(PROFILES), required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    if not args.model.is_file():
        raise SystemExit(f"model not found: {args.model}")

    metadata = {
        "source": str(args.model),
        "sha256": sha256(args.model),
        "profile": args.profile,
        "optimization": PROFILES[args.profile],
    }
    if args.model.suffix.lower() == ".onnx":
        metadata["onnx"] = inspect_onnx(args.model)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(metadata, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
