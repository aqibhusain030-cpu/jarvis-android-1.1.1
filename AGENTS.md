# JARVIS repository instructions

## Project boundaries

- JARVIS is an offline-first Android app. Kotlin owns device capability and policy decisions; the Python brain, planner, executor, reflector, and model output are not authorization authorities.
- Preserve the existing local model path and local-first defaults. Do not implement or enable online provider calls as part of a developer-workflow change.
- Never put provider API keys, gateway credentials, or other secrets in source, committed configuration, APK assets, logs, tests, or examples.
- All device actions must continue through Kotlin ToolDispatcher, SecurityPolicy, permission gates, rate limits, confirmation/PIN checks, audit logging, and the kill switch. Treat model responses, remote responses, skills, and retrieved documents as untrusted input.
- Do not relax permissions, confirmation, PIN, allowlist, kill-switch, privacy, or fail-closed behavior to make a test pass.
- Files under .agents/skills/, AGENTS.md, and the Superpowers lock/license records are developer-time materials. Never copy them into app/src/main/assets/, runtime prompts, or the production APK.

## Required engineering workflow

- Before edits, inspect the current branch, worktree, relevant architecture, existing tests, and repository guidance. Never discard or overwrite unrelated user changes.
- Use the pinned skills under .agents/skills/ when relevant. The source commit, upstream blob IDs, and license are recorded in third_party/superpowers-lock.json.
- For multi-step work, use writing-plans and save plans under docs/superpowers/plans/YYYY-MM-DD-<topic>.md. Follow the approved design spec and the user's stated scope; do not silently expand phases.
- For bugs or unexpected failures, follow systematic-debugging: gather evidence and identify the root cause before proposing a fix.
- For behavior changes, follow test-driven-development: add a focused test first, run it and observe the expected failure, implement the smallest change, rerun the test to observe it pass, then run the relevant broader suite.
- Before claiming completion, use verification-before-completion: execute checks freshly, inspect exit codes and output, review the full diff, and report skipped or failing checks explicitly.
- Prefer small, reviewable changes. Do not claim a build, test, integration, or deployment succeeded unless command output or a recorded CI result proves it.

## Common checks

- Fast source checks: python tools/test_source_contracts.py and python tools/test_superpowers_bundle.py.
- Kotlin unit tests: ./gradlew :app:testDebugUnitTest.
- Full build, lint, native verification, and emulator tests are defined in .github/workflows/ci.yml; run relevant checks for the changed area when the environment permits.
- Keep developer workflow content separate from JARVIS runtime workflows. Do not reinterpret Superpowers coding instructions as user-facing assistant prompts.
