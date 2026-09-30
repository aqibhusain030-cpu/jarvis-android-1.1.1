# JARVIS Android Security and Network Architecture

## Execution authority

LLM -> intent -> Security Director -> Scope Resolver -> Risk Controller -> Capability Registry -> CapabilityExecutor -> backend -> evidence -> world model.

The model never receives arbitrary shell or network credentials.

## Scope

A SecurityScope contains:
- target constraint
- scope ID
- expiry
- allowed capability IDs
- maximum risk
- environment
- integrity proof

Target matching supports exact hostnames, wildcard DNS names, and IPv4 CIDR ranges.

Active network operations additionally consume the existing one-shot operator authorization.

## Implemented network primitives

- DNS resolution
- bounded TCP probing
- bounded TCP port enumeration
- deterministic service identification
- TLS metadata inspection
- asset inventory
- live Android connectivity state
- route and DNS inspection
- structured target profiling
- web security header/cookie analysis
- event bus for network observations

Port enumeration is deliberately bounded to 256 ports per request and 16 concurrent probes.

## Security assessment

Implemented:
- application manifest posture assessment
- authorized assessment planning
- structured integrity-hashed evidence
- authorized port finding retesting
- safe exploit-path simulation without payload execution
- consent-based monitoring

The planner produces an assessment workflow. It does not generate destructive payloads or autonomous compromise instructions.

## Android traffic observation

Android VpnService is the supported platform primitive for a future packet/flow capture implementation. Establishing a VPN requires explicit user action and Android permits only one active VPN at a time. The repository does not pretend that a raw-socket implementation bypasses Android's VPN/security model.

For normal connectivity changes, JARVIS uses ConnectivityManager NetworkCallback.

## Web testing baseline

The web analyzer is intentionally non-destructive and is aligned with the methodology of the OWASP Web Security Testing Guide. Mobile security work should be mapped to OWASP MASVS/MASTG controls.

## Explicit non-goals

This subsystem does not implement:
- credential theft
- persistence
- evasion
- destructive payloads
- arbitrary-target exploitation
- autonomous compromise
- covert packet interception
- weaponized exploit generation

Authorized assessment, vulnerability validation, evidence collection and remediation retesting remain supported.

## Production gate

Before enabling broader active testing:
1. run unit tests
2. build debug and release APKs
3. test on a dedicated lab network
4. verify scope expiry/revocation
5. verify biometric/device-credential gating
6. verify kill-switch behavior
7. verify audit records
8. verify that all active operations reject out-of-scope targets
