package com.jarvis.capability.security

import com.jarvis.core.capability.*
import com.jarvis.friday.security.*
import com.jarvis.core.capability.OperatorAuthorization

class SecurityLabBackend(
    private val scopes: SecurityScopeRegistry,
    private val assessment: SecurityAssessmentEngine,
    private val simulator: ExploitSimulator,
    private val monitor: ConsentMonitor,
    private val operator: OperatorAuthorization
) : CapabilityBackend {
    override val backendId = "friday.security"
    override val capabilities = setOf(
        Capability("security.scope.enroll", "Enroll an explicitly authorized security target", Capability.RiskLevel.HIGH),
        Capability("security.assess.app", "Assess declared Android application security controls"),
        Capability("security.simulate.exploit", "Simulate an exploit path without executing it", Capability.RiskLevel.MEDIUM),
        Capability("security.monitor.enable", "Enable consent-based security monitoring", Capability.RiskLevel.MEDIUM),
        Capability("security.monitor.disable", "Disable security monitoring"),
        Capability("security.monitor.status", "Read security monitoring status"),
        Capability("security.monitor.events", "Read recent security monitoring events", Capability.RiskLevel.MEDIUM)
    )

    override suspend fun execute(r: CapabilityRequest): CapabilityResult {
        if (!operator.consume()) {
            return CapabilityResult.Rejected(
                r.requestId,
                CapabilityResult.RejectionCode.POLICY_DENIED,
                "Fresh operator authorization is required for security operations."
            )
        }
        return when (r.capabilityId) {
        "security.scope.enroll" -> enroll(r)
        "security.assess.app" -> assess(r)
        "security.simulate.exploit" -> simulate(r)
        "security.monitor.enable" -> enableMonitor(r)
        "security.monitor.disable" -> {
            monitor.disable()
            CapabilityResult.Success(r.requestId, "Security monitoring disabled.")
        }
        "security.monitor.status" -> CapabilityResult.Success(
            r.requestId,
            if (monitor.enabled) "Security monitoring is enabled." else "Security monitoring is disabled.",
            mapOf("enabled" to monitor.enabled.toString())
        )
        "security.monitor.events" -> events(r)
        else -> CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unknown security capability.")
    }

    private fun enroll(r: CapabilityRequest): CapabilityResult {
        val target = r.arguments["target"]?.trim().orEmpty()
        val expiry = runCatching { java.time.Instant.parse(r.arguments["expiresAt"]?.trim().orEmpty()) }.getOrNull()
        if (target.isBlank() || expiry == null) return invalid(r)
        val authorized = r.arguments["authorized"]?.toBooleanStrictOrNull() == true
        val scope = SecurityScope(target = target, label = target, authorizedByUser = authorized, expiresAt = expiry)
        return if (scopes.enroll(scope)) CapabilityResult.Success(r.requestId, "Security scope enrolled.", mapOf("scopeId" to scope.id, "target" to target))
        else denied(r)
    }

    private fun assess(r: CapabilityRequest): CapabilityResult {
        val target = r.arguments["target"]?.trim().takeUnless { it.isNullOrBlank() } ?: "local-app"
        if (target != "local-app" && !scopes.isAuthorized(target)) return denied(r)
        val result = assessment.assessManifest(
            target,
            r.arguments["cleartextDisabled"]?.toBooleanStrictOrNull() == true,
            r.arguments["allowBackupDisabled"]?.toBooleanStrictOrNull() == true,
            r.arguments["accessibilityServiceExported"]?.toBooleanStrictOrNull() == true,
            r.arguments["excessivePackageVisibility"]?.toBooleanStrictOrNull() == true
        )
        return CapabilityResult.Success(r.requestId, "Security assessment completed.", mapOf("target" to result.target, "score" to result.score.toString(), "findingCount" to result.findings.size.toString()))
    }

    private fun simulate(r: CapabilityRequest): CapabilityResult {
        val target = r.arguments["target"]?.trim().orEmpty()
        if (target.isBlank() || !scopes.isAuthorized(target)) return denied(r)
        val scenario = runCatching { SimulatedScenario.valueOf(r.arguments["scenario"]?.uppercase().orEmpty()) }.getOrNull() ?: return invalid(r)
        val simulation = simulator.run(scenario, target)
        return CapabilityResult.Success(r.requestId, "Exploit simulation completed without execution.", mapOf("target" to simulation.target, "scenario" to simulation.scenario.name, "blocked" to simulation.blocked.toString()))
    }

    private fun enableMonitor(r: CapabilityRequest): CapabilityResult {
        val confirmed = r.arguments["confirmed"]?.toBooleanStrictOrNull() == true
        return if (monitor.enable(confirmed)) CapabilityResult.Success(r.requestId, "Consent-based security monitoring enabled.")
        else CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.CONFIRMATION_REQUIRED, "Explicit monitoring consent is required.")
    }

    private suspend fun events(r: CapabilityRequest): CapabilityResult {
        val recent = monitor.recent()
        val data = buildMap {
            put("count", recent.size.toString())
            recent.forEachIndexed { index, item -> put("event." + index, item.timestamp.toString() + "|" + item.source + "|" + item.summary) }
        }
        return CapabilityResult.Success(r.requestId, "Monitoring events retrieved.", data)
    }

    private fun invalid(r: CapabilityRequest) = CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.INVALID_ARGUMENTS, "Required security arguments are missing or invalid.")
    private fun denied(r: CapabilityRequest) = CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.POLICY_DENIED, "Security scope is not authorized.")
}