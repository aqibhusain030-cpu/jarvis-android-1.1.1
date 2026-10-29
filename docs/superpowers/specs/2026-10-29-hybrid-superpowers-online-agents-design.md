# JARVIS Hybrid Workflows and Online-Agent Integration

**Status:** Draft for owner approval  
**Date:** 2026-10-29  
**Repository base:** `c2a5264c71825f3b1f9f5c659461b70a49ba7a68`  
**Working branch:** `feat/superpowers-workflows`

## 1. Intent

Extend the existing Android JARVIS application with two related but separate capabilities:

1. **Developer-time Superpowers workflows** for planning, test-driven development, systematic debugging, and verification while changing this repository.
2. **Runtime JARVIS workflows plus optional online agents** for more capable reasoning, research, and second opinions when a network connection and user consent are available.

JARVIS remains offline-first. Local inference continues to work without a configured gateway. Online model access is an explicit capability, not a silent requirement.

## 2. What exists today

The application is an Android/Kotlin app with a Chaquopy-hosted Python reasoning layer. `BrainBridge.kt` calls `brain.py`; `brain.py` uses a Kotlin-provided `LocalLlmEngine.generate(prompt, max_tokens, temperature)`, a Python planner, and a tool bridge. Android `ToolDispatcher` and `SecurityPolicy` remain the authoritative gate for phone actions.

The current asset config uses `llm.provider = local`, `cloud.provider = none`, and `safety.cloud_fallback_enabled = false`. The strict config validator accepts only the local LLM provider. Thus, cloud inference is not currently wired end-to-end.

The existing planner/executor/reflector are useful foundations, but should not be replaced by a second competing agent runtime.

## 3. Approaches considered

### A. Local-only Superpowers-inspired workflows

Keep all runtime inference on-device and add only planning/debugging/verification workflows.

- Advantages: simplest privacy story; works offline.
- Trade-off: does not meet the additional requirement to make online agents available when applicable.

### B. Direct provider APIs from the Android app

Call individual model providers directly from the APK.

- Rejected for production credentials: provider API keys must not be embedded in a mobile app. They can be extracted and abused. Official provider guidance recommends keeping keys server-side and routing requests through a backend. See [OpenAI API-key safety](https://help.openai.com/en/articles/5112595-best-practices-for-api-key-safety) and [Gemini API-key safety](https://ai.google.dev/gemini-api/docs/api-key).

### C. Hybrid app + authenticated online-agent gateway (**recommended**)

Keep local inference and add a provider-neutral HTTPS gateway client. The gateway keeps provider secrets server-side and may route complex requests to one agent or a multi-agent council. The mobile app receives a structured answer and metadata. The app keeps all phone-tool permissions, confirmations, and execution local.

- Advantages: provider-neutral; supports a council; avoids shipping vendor keys; preserves offline fallback.
- Trade-off: online mode needs a deployed gateway, network access, authentication, and a cost/rate-limit policy.

## 4. Proposed architecture

### 4.1 Development-time workflows

- Add repository instructions that tell coding agents to follow the chosen Superpowers workflow for the task: brainstorm/design before features, write a plan for multi-step work, use TDD for code changes, debug from evidence, and verify before claiming completion.
- Vendor only the selected upstream skill files needed for development, pinned to upstream commit `2ce8aa15d05bd57213e6f746f953aafb7ded7a8b` (Superpowers v7.0.1), with its MIT license and source attribution.
- Keep these development skill files outside Android assets so they are not shipped inside the production APK.

### 4.2 Runtime workflows

Adapt the relevant *ideas* to JARVIS's current planner and goal engine instead of shipping coding-agent instructions as production prompts. Initial workflow kinds:

- `plan`: create a bounded, ordered plan with explicit prerequisites.
- `research`: collect and summarize information through the configured online gateway, without device-tool access.
- `debug`: state observed symptoms, competing hypotheses, evidence needed, and a falsifiable next check.
- `execute`: execute only approved, locally validated actions through the existing Android dispatcher.
- `verify`: check each claimed outcome against tool results or other observable evidence; do not infer success from a model's statement.
- `reflect`: record a brief lesson from verified outcomes, avoiding raw sensitive payloads.

A workflow may ask for clarification or approval where necessary. It must not turn model output into authority.

### 4.3 Online-agent gateway

Introduce a Kotlin gateway client behind a small interface compatible with the existing model-generation call. All network work runs off the UI thread with bounded connect/read/write timeouts and response sizes.

A versioned request contract should include:
- request ID and protocol version;
- task type/workflow ID;
- user prompt;
- output token limit and a configured model profile;
- only the minimal context the user has consented to share.

A versioned response contract should include:
- final answer;
- selected provider/model profile;
- optional agent roles used (for example researcher, planner, reviewer);
- request ID and usage metadata where the gateway supplies it;
- normalized error information on failure.

The gateway, not the APK, chooses vendor providers and holds vendor credentials. A complex-task council should be optional and bounded; ordinary requests should use one suitable model rather than incur unnecessary multi-agent cost.

The backend endpoint is an explicit deployment dependency. The Android work must not pretend online mode is operational until the configured endpoint contract and an integration test are available.

### 4.4 Routing and fallback

- Default mode remains `local`.
- `hybrid` allows explicit online requests and, only when enabled by the user, policy-defined fallback.
- Cloud fallback stays disabled by default.
- If the network is unavailable, the gateway is unconfigured, consent is absent, the request times out, or the response is invalid, return a clear status and fall back to local inference when appropriate.
- Never silently upload notification contents, contacts, location, clipboard, calendar, or stored memory.
- The current user message is not sent to a remote service unless online mode is selected/consented to. Additional history or memories require a separate, explicit context-sharing choice.
- Show when an answer used an online provider and make cloud mode easy to disable.

### 4.5 Security boundary

- No OpenAI/Gemini/Anthropic or other provider API key is stored in the APK, checked into Git, or logged.
- The gateway uses HTTPS, authenticated per-device/user access, server-side provider secrets, request budgets, rate limits, and abuse monitoring.
- A gateway credential, if required on-device, is user-specific and stored using Android Keystore-backed protection, not in `config.json` or plaintext logs.
- The remote model has no direct authority to invoke Android tools. Any proposed device action must be converted into a typed candidate and passed through the existing Kotlin `ToolDispatcher`, `SecurityPolicy`, permission gates, rate limits, audit logging, and confirmation/PIN requirements.
- Remote output and skill content are untrusted data. They cannot override the system policy, safety configuration, or user consent.
- Do not log raw prompts, conversations, authorization headers, credentials, sensitive tool results, or provider response bodies. Log request IDs, duration, provider/profile, outcome category, and token/cost metadata only where safe and available.

### 4.6 Configuration and Settings

- Extend the strict config schema in a backward-compatible migration, preserving existing installations and the current local defaults.
- Model configuration should use `local` or `hybrid` routing and a provider-neutral gateway profile; individual vendor credentials stay server-side.
- Keep `cloud_fallback_enabled` false by default.
- Add clear Settings controls for online mode, gateway connection/profile, context-sharing consent, and a test-connection/status result. Do not show or log a stored credential after entry.
- Reject non-HTTPS gateway URLs and unsafe loopback/private-network destinations for production remote inference unless a separate developer-only local test mode is explicitly designed.

## 5. Scope boundaries

### In scope
- Repository-level Superpowers workflow instructions and a pinned, attributed developer skill subset.
- A lightweight runtime workflow abstraction integrated with the current Python brain/planner.
- A provider-neutral authenticated gateway client and local/hybrid routing seam.
- Explicit cloud-consent behavior, safe fallback, and security-preserving tool execution.
- Unit/source-contract tests and CI verification for configuration, routing, redaction, and fail-closed behavior.

### Out of scope for the first increment
- Replacing the Android app architecture or local LLM.
- Embedding provider API keys in the APK.
- Automatically granting a remote agent access to SMS, calls, contacts, files, notifications, camera, or other device capabilities.
- Running every request through a council of agents.
- Claiming a production gateway exists before its API contract is confirmed/deployed.
- Shipping developer-oriented Superpowers markdown as runtime prompts.

## 6. Acceptance criteria

1. Existing local-only configuration parses and behaves as before; cloud remains disabled by default.
2. Tests prove that local mode never makes a gateway call.
3. Online mode refuses requests without valid HTTPS gateway configuration and required user consent.
4. Gateway requests contain only explicitly permitted context; secrets and raw sensitive payloads never enter diagnostic logs.
5. A timeout, HTTP error, malformed response, or offline state produces a normalized error and a documented local fallback decision.
6. Remote model output cannot bypass the local dispatcher, confirmation, permissions, network policy, or kill switch.
7. Planning and verification workflows distinguish proposed actions from completed actions, and claim success only with evidence.
8. English, Hindi, and Hinglish prompt behavior remains supported.
9. CI runs relevant Python tests, Kotlin tests, strict configuration checks, source-contract checks, and the existing build/lint suite.
10. Online functionality is labeled available only after the gateway protocol is implemented and a real integration test passes.

## 7. Implementation sequence after approval

1. Pin and add developer-time Superpowers instructions/selected skills with attribution.
2. Add focused failing tests for workflow selection, configuration compatibility, local-vs-online routing, and consent/redaction.
3. Add the runtime workflow types and local verification behavior.
4. Implement the Kotlin gateway interface/client and bounded request/response contract.
5. Add secure settings/config persistence and explicit opt-in UX.
6. Connect routing to `BrainBridge`/Python without allowing cloud output to bypass the Android tool dispatcher.
7. Add or confirm the backend gateway endpoint and test the end-to-end online path.
8. Run all applicable tests, build, lint, inspect the diff, and open a reviewable pull request. Do not merge automatically.

## 8. Approval needed

This design assumes a **server-side provider gateway** rather than vendor API keys inside Android. It also assumes cloud use remains opt-in and local-first, while a multi-agent council is reserved for complex tasks. The backend endpoint and authentication contract must be confirmed before production cloud routing can be enabled.
