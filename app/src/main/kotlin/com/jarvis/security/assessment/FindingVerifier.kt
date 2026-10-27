package com.jarvis.security.assessment

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.network.NetworkEngine
import com.jarvis.network.PortScanResult
import com.jarvis.network.PortState
import javax.inject.Inject
import javax.inject.Singleton

enum class VerificationStatus { FIXED, REGRESSED, STILL_PRESENT, INCONCLUSIVE }

data class VerificationResult(
    val findingId: String,
    val status: VerificationStatus,
    val evidence: String
)

/**
 * Re-checks a single TCP-port finding after remediation.
 * Timeouts and ambiguous failures are INCONCLUSIVE, never proof of a fix.
 */
@Singleton
class FindingVerifier @Inject constructor(
    private val scopes: SecurityScopeRegistry,
    private val network: NetworkEngine
) {
    suspend fun verifyPortClosed(
        scopeId: String,
        target: String,
        port: Int,
        findingId: String
    ): VerificationResult {
        if (findingId.isBlank()) {
            return VerificationResult(findingId, VerificationStatus.INCONCLUSIVE, "A finding ID is required.")
        }
        if (port !in 1..65535) {
            return VerificationResult(findingId, VerificationStatus.INCONCLUSIVE, "Port must be between 1 and 65535.")
        }
        if (!scopes.isAuthorized(
                scopeId, target, SecurityScopeRegistry.ACTION_FINDING_VERIFY, SecurityRisk.R3_VALIDATION
            )
        ) {
            return VerificationResult(
                findingId, VerificationStatus.INCONCLUSIVE,
                "Target is outside the authorized scope or the grant has expired."
            )
        }

        val result: PortScanResult = network.scan(
            scopeId = scopeId,
            target = target,
            ports = listOf(port),
            action = SecurityScopeRegistry.ACTION_FINDING_VERIFY,
            risk = SecurityRisk.R3_VALIDATION
        )

        return when (result.stateFor(port)) {
            PortState.OPEN -> VerificationResult(
                findingId, VerificationStatus.STILL_PRESENT, "The TCP port accepted a connection."
            )
            PortState.CLOSED -> VerificationResult(
                findingId, VerificationStatus.FIXED, "The TCP connection was refused during verification."
            )
            PortState.FILTERED -> VerificationResult(
                findingId, VerificationStatus.INCONCLUSIVE,
                "The connection timed out or was unreachable; closure was not proven."
            )
            PortState.ERROR, PortState.NOT_SCANNED -> VerificationResult(
                findingId, VerificationStatus.INCONCLUSIVE,
                result.error ?: "The port state could not be established."
            )
        }
    }
}
