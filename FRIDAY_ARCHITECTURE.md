# F.R.I.D.A.Y. Canonical Architecture

F.R.I.D.A.Y. is implemented as a policy-governed successor layer over the existing JARVIS Android stack.

## Mapping

| MCU concept | Android implementation |
|---|---|
| Friday Prime | `FridayPrime` |
| Executive control | `FridayExecutive` |
| Safety / directives | `FridayDirectives` |
| Tactical world model | `TacticalWorldModel` |
| Mission memory | `FridayMemory` |
| Audit trail | `FridayAuditSink` |
| Action boundary | `FridayActionGateway` |
| Android capability execution | existing `CapabilityExecutor` |
| Android intent/package/accessibility | existing capability backends |
| Existing JARVIS tools | existing `ToolRegistry` / `ToolDispatcher` |

## Runtime flow

```
Voice/UI
  -> intent + structured command
  -> FridayPrime
  -> FridayExecutive
  -> confirmation / refusal / execution
  -> FridayActionGateway
  -> CapabilityExecutor
  -> existing Android capability backend
  -> audit
```

Perception and world-state providers feed `TacticalWorldModel`. Memory and audit are explicit interfaces so persistent implementations can be backed by the existing Room infrastructure without changing the executive contract.

## Safety boundary

The MCU specification contains fictional weapon systems, unrestricted hacking, global surveillance, and destructive self-defense behavior. The Android implementation therefore exposes **named, policy-governed capabilities**, not arbitrary shell commands, exploit primitives, weapon controls, or unrestricted remote execution.

High and critical actions remain confirmation-gated by the capability control plane. A compromised execution context is isolated by `FridayExecutive`.

## Distributed identity

The current Android process is the local primary instance. Distributed synchronization, satellite links, suit hardware, and cross-device replicas are represented as future transport/state interfaces rather than pretending a phone has access to fictional Stark infrastructure.

## Voice

The application may provide a configurable voice persona, but does not embed or impersonate a real actor's voice. A compatible synthetic voice can be selected through the existing TTS abstraction.
