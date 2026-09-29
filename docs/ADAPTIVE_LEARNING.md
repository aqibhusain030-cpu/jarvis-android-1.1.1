# JARVIS adaptive mistake learning

JARVIS now has a bounded feedback loop for mistakes and recovery.

## What is learned
- Tool name and failure context
- Stable error code
- Sanitized, length-bounded error message
- Optional correction
- Failure count and successful recovery count
- Evidence-based confidence
- Last successful recovery timestamp

## Learning loop
1. ToolDispatcher records failures after validation, policy, permission, network, authorization, timeout, or execution errors.
2. MemoryStore creates a deterministic SHA-256 fingerprint from tool + context + error code.
3. Duplicate failures update the existing lesson instead of creating unlimited records.
4. A later successful invocation of the same tool after an execution failure raises the corresponding lesson confidence.
5. BrainBridge retrieves up to five relevant lessons and supplies them as structured context for the next reasoning cycle.
6. Lessons older than 90 days are pruned.

## Safety boundaries
- Learning cannot change the APK, security policy, permissions, kill switch, or model weights.
- Failure alone never increases confidence.
- Lesson text is bounded before persistence.
- Lessons stay in the local Room database.
- The system does not automatically execute a proposed correction merely because it was learned.

This is adaptive behavior through persistent evidence and retrieval, not uncontrolled self-modification.
