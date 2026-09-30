package com.jarvis.capability.security

import com.jarvis.core.capability.*
import com.jarvis.security.assessment.FindingVerifier
import com.jarvis.security.assessment.SecurityEvidenceStore
import com.jarvis.security.assessment.SecurityPlanEngine
import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityAssessmentBackend @Inject constructor(
    private val scopes: SecurityScopeRegistry,
    private val planner: SecurityPlanEngine,
    private val verifier: FindingVerifier,
    private val evidence: SecurityEvidenceStore
) : CapabilityBackend {
    override val backendId = "friday.security.assessment"
    override val capabilities = setOf(
        Capability("security.plan", "Generate a bounded authorized security assessment plan", Capability.RiskLevel.HIGH),
        Capability("security.verify.port", "Re-test whether an authorized port finding is still present", Capability.RiskLevel.HIGH),
        Capability("security.evidence.record", "Record structured security evidence", Capability.RiskLevel.MEDIUM)
    )

    override suspend fun execute(r: CapabilityRequest): CapabilityResult {
        val scopeId = r.arguments["scope_id"].orEmpty()
        val target = r.arguments["target"].orEmpty()
        if (scopeId.isBlank() || target.isBlank()) return invalid(r)
        return when (r.capabilityId) {
            "security.plan" -> {
                if (!scopes.isAuthorized(scopeId, target, "target_profile", SecurityRisk.R1_PASSIVE)) return denied(r)
                val plan = planner.build(scopeId, target, r.arguments["objective"].orEmpty())
                CapabilityResult.Success(r.requestId, "Authorized assessment plan generated.", mapOf(
                    "planId" to plan.planId,
                    "steps" to plan.steps.joinToString("\n") { it.order.toString() + "|" + it.capability + "|" + it.risk.name + "|" + it.purpose }
                ))
            }
            "security.verify.port" -> {
                val port = r.arguments["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return invalid(r)
                val findingId = r.arguments["finding_id"].orEmpty().ifBlank { return invalid(r) }
                val result = verifier.verifyPortClosed(scopeId, target, port, findingId)
                CapabilityResult.Success(r.requestId, "Finding retest completed.", mapOf("findingId" to result.findingId, "status" to result.status.name, "evidence" to result.evidence))
            }
            "security.evidence.record" -> {
                if (!scopes.isAuthorized(scopeId, target, "finding_verify", SecurityRisk.R3_VALIDATION)) return denied(r)
                val item = evidence.record(
                    assessmentId = r.arguments["assessment_id"].orEmpty(),
                    target = target,
                    capability = r.arguments["capability"].orEmpty(),
                    observation = r.arguments["observation"].orEmpty(),
                    confidence = r.arguments["confidence"]?.toFloatOrNull() ?: 0.5f
                )
                CapabilityResult.Success(r.requestId, "Evidence recorded.", mapOf("evidenceId" to item.evidenceId, "integrityHash" to item.integrityHash))
            }
            else -> CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unknown security assessment capability.")
        }
    }

    private fun invalid(r: CapabilityRequest) = CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.INVALID_ARGUMENTS, "Required assessment arguments are missing or invalid.")
    private fun denied(r: CapabilityRequest) = CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.POLICY_DENIED, "Target is outside the active authorized security scope.")
}
