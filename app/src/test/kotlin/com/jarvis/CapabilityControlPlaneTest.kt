package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.core.capability.Capability
import com.jarvis.core.capability.CapabilityBackend
import com.jarvis.core.capability.CapabilityExecutor
import com.jarvis.core.capability.CapabilityPolicy
import com.jarvis.core.capability.CapabilityRegistry
import com.jarvis.core.capability.CapabilityRequest
import com.jarvis.core.capability.CapabilityResult
import com.jarvis.core.capability.DefaultCapabilityPolicy
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Regression coverage for the public capability-control-plane contracts.
 *
 * This test intentionally lives in package com.jarvis so its class name does
 * not collide with the more focused capability tests in the subpackage.
 */
class CapabilityControlPlaneTest {

    private class FakeBackend(
        override val backendId: String,
        override val capabilities: Set<Capability>
    ) : CapabilityBackend {
        var executions = 0

        override suspend fun execute(request: CapabilityRequest): CapabilityResult {
            executions++
            return CapabilityResult.Success(request.requestId, "ok")
        }
    }

    @Test
    fun registry_prefers_higher_priority_backend() = runTest {
        val capability = Capability("test.action", "test")
        val low = FakeBackend("low", setOf(capability))
        val high = FakeBackend("high", setOf(capability))
        val registry = CapabilityRegistry()
        registry.register(low, priority = 1)
        registry.register(high, priority = 10)

        assertThat(registry.resolve("test.action")?.backendId).isEqualTo("high")
    }

    @Test
    fun executor_rejects_unknown_capability() = runTest {
        val executor = CapabilityExecutor(
            registry = CapabilityRegistry(),
            policyProvider = DefaultCapabilityPolicy()
        )

        val result = executor.execute(
            CapabilityRequest(
                capabilityId = "missing",
                requestId = "missing-request"
            )
        )

        assertThat(result).isInstanceOf(CapabilityResult.Rejected::class.java)
        assertThat((result as CapabilityResult.Rejected).code)
            .isEqualTo(CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY)
    }

    @Test
    fun executor_requires_confirmation_for_high_risk_action() = runTest {
        val backend = FakeBackend(
            "security",
            setOf(Capability("lock", "Lock device", Capability.RiskLevel.HIGH))
        )
        val registry = CapabilityRegistry().apply { register(backend) }
        val executor = CapabilityExecutor(
            registry = registry,
            policyProvider = DefaultCapabilityPolicy()
        )

        val result = executor.execute(
            CapabilityRequest(
                capabilityId = "lock",
                requestId = "lock-unconfirmed"
            )
        )

        assertThat(result).isInstanceOf(CapabilityResult.Rejected::class.java)
        assertThat((result as CapabilityResult.Rejected).code)
            .isEqualTo(CapabilityResult.RejectionCode.CONFIRMATION_REQUIRED)
        assertThat(backend.executions).isEqualTo(0)
    }

    @Test
    fun executor_allows_confirmed_high_risk_action() = runTest {
        val backend = FakeBackend(
            "security",
            setOf(Capability("lock", "Lock device", Capability.RiskLevel.HIGH))
        )
        val registry = CapabilityRegistry().apply { register(backend) }
        val executor = CapabilityExecutor(
            registry = registry,
            policyProvider = DefaultCapabilityPolicy()
        )

        val result = executor.execute(
            CapabilityRequest(
                capabilityId = "lock",
                requestId = "lock-confirmed",
                confirmationGranted = true
            )
        )

        assertThat(result).isInstanceOf(CapabilityResult.Success::class.java)
        assertThat(backend.executions).isEqualTo(1)
    }
}
