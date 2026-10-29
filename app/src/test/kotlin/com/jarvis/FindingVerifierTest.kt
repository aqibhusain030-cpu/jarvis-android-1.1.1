package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.network.NetworkEngine
import com.jarvis.security.assessment.FindingVerifier
import com.jarvis.security.assessment.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket

class FindingVerifierTest {
    @Test fun openPortIsReportedAsStillPresent() = runBlocking {
        val registry = SecurityScopeRegistry()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            val port = server.localPort
            assertThat(registry.grant(
                scopeId = "verify_test",
                targets = setOf("127.0.0.1"),
                allowedActions = setOf(SecurityScopeRegistry.ACTION_FINDING_VERIFY),
                maxRisk = SecurityRisk.R3_VALIDATION,
                expiresAtEpochMillis = System.currentTimeMillis() + 60_000,
                explicitlyConfirmed = true
            )).isTrue()

            val verifier = FindingVerifier(registry, NetworkEngine(registry))
            val result = verifier.verifyPortClosed("verify_test", "127.0.0.1", port, "F-TEST-1")
            assertThat(result.status).isEqualTo(VerificationStatus.STILL_PRESENT)
        } finally {
            server.close()
        }
    }

    @Test fun invalidOrUnauthorizedFindingIsInconclusive() = runBlocking {
        val registry = SecurityScopeRegistry()
        val verifier = FindingVerifier(registry, NetworkEngine(registry))
        val result = verifier.verifyPortClosed("no_scope", "127.0.0.1", 443, "F-TEST-2")
        assertThat(result.status).isEqualTo(VerificationStatus.INCONCLUSIVE)
    }
}
