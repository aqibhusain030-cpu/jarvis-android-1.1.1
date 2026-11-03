package com.jarvis.core.capability

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

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
        val low = FakeBackend("low", setOf(Capability("test.action", "test")))
        val high = FakeBackend("high", setOf(Capability("test.action", "test")))
        val registry = CapabilityRegistry()
        registry.register(low, 1)
        registry.register(high, 10)

        assertThat(registry.find("test.action")?.backendId).isEqualTo("high")
    }

    @Test
    fun executor_rejects_unknown_capability() = runTest {
        val executor = CapabilityExecutor(
            CapabilityRegistry(),
            CapabilityPolicy(DefaultCapabilityPolicy())
        )

        val result = executor.execute(CapabilityRequest("missing"))

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
        val registry = CapabilityRegistry()
        registry.register(backend)

        val executor = CapabilityExecutor(
            registry,
            CapabilityPolicy(DefaultCapabilityPolicy())
        )

        val result = executor.execute(CapabilityRequest("lock"))

        assertThat(result).isInstanceOf(CapabilityResult.Rejected::class.java)
        assertThat(backend.executions).isEqualTo(0)
    }

    @Test
    fun executor_allows_confirmed_high_risk_action() = runTest {
        val backend = FakeBackend(
            "security",
            setOf(Capability("lock", "Lock device", Capability.RiskLevel.HIGH))
        )
        val registry = CapabilityRegistry()
        registry.register(backend)

        val executor = CapabilityExecutor(
            registry,
            CapabilityPolicy(DefaultCapabilityPolicy())
        )

        val result = executor.execute(
            CapabilityRequest(
                capabilityId = "lock",
                confirmationGranted = true
            )
        )

        assertThat(result).isInstanceOf(CapabilityResult.Success::class.java)
        assertThat(backend.executions).isEqualTo(1)
    }
}
