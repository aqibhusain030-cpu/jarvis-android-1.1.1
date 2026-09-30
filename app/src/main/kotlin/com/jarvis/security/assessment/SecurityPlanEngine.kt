package com.jarvis.security.assessment

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import javax.inject.Inject
import javax.inject.Singleton

data class SecurityPlanStep(
    val order: Int,
    val capability: String,
    val risk: SecurityRisk,
    val purpose: String
)

data class SecurityPlan(
    val planId: String,
    val scopeId: String,
    val target: String,
    val objective: String,
    val steps: List<SecurityPlanStep>
)

@Singleton
class SecurityPlanEngine @Inject constructor(
    private val scopes: SecurityScopeRegistry
) {
    fun build(scopeId: String, target: String, objective: String): SecurityPlan {
        check(scopes.isAuthorized(scopeId, target, "target_profile", SecurityRisk.R1_PASSIVE))
        val steps = listOf(
            SecurityPlanStep(1, "network.dns_resolve", SecurityRisk.R0_INFORMATION, "Resolve target identity and addresses."),
            SecurityPlanStep(2, "network.port_scan", SecurityRisk.R2_ENUMERATION, "Enumerate only the bounded ports requested by the authorized scope."),
            SecurityPlanStep(3, "network.service_detect", SecurityRisk.R2_ENUMERATION, "Identify observed services."),
            SecurityPlanStep(4, "network.tls_inspect", SecurityRisk.R1_PASSIVE, "Inspect TLS metadata where applicable."),
            SecurityPlanStep(5, "websec.analyze", SecurityRisk.R3_VALIDATION, "Run safe web security-control checks."),
            SecurityPlanStep(6, "security.simulate.exploit", SecurityRisk.R3_VALIDATION, "Model a permitted exploit path without executing a payload."),
            SecurityPlanStep(7, "finding_verify", SecurityRisk.R3_VALIDATION, "Re-test evidence and classify the result.")
        )
        return SecurityPlan(
            planId = java.util.UUID.randomUUID().toString(),
            scopeId = scopeId,
            target = target,
            objective = objective.ifBlank { "Authorized security assessment" },
            steps = steps
        )
    }
}
