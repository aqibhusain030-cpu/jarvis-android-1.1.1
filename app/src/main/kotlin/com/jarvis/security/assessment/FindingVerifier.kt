package com.jarvis.security.assessment

import com.jarvis.core.capability.OperatorAuthorization
import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.network.NetworkEngine
import javax.inject.Inject
import javax.inject.Singleton

enum class VerificationStatus { FIXED, REGRESSED, STILL_PRESENT, INCONCLUSIVE }

data class VerificationResult(
    val findingId: String,
    val status: VerificationStatus,
    val evidence: String
)

@Singleton
class FindingVerifier @Inject constructor(
    private val scopes: SecurityScopeRegistry,
    private val network: NetworkEngine,
    private val operator: OperatorAuthorization
) {
    suspend fun verifyPortClosed(scopeId: String, target: String, port: Int, findingId: String): VerificationResult {
        if (!scopes.isAuthorized(scopeId, target, "finding_verify", SecurityRisk.R3_VALIDATION)) {
            return VerificationResult(findingId, VerificationStatus.INCONCLUSIVE, "Target is outside the authorized scope.")
        }
        if (!operator.consume()) {
            return VerificationResult(findingId, VerificationStatus.INCONCLUSIVE, "Fresh operator authorization is required for active verification.")
        }
        val result = network.scan(scopeId, target, listOf(port))
        return if (port !in result.openPorts) {
            VerificationResult(findingId, VerificationStatus.FIXED, "Port is no longer observed as open.")
        } else {
            VerificationResult(findingId, VerificationStatus.STILL_PRESENT, "Port remains open.")
        }
    }
}
