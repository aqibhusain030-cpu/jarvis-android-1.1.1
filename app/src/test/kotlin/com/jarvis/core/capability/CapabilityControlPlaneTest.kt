package com.jarvis.core.capability

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CapabilityControlPlaneTest {
    private class Backend(
        override val backendId: String,
        override val capabilities: Set<Capability>
    ) : CapabilityBackend {
        override suspend fun execute(request: CapabilityRequest): CapabilityResult =
            CapabilityResult.Success(request.requestId, "ok")
    }

    @Test
    fun registry_prefers_higher_priority_backend() {
        val registry = CapabilityRegistry()
        val capability = Capability("test.action", "test")
        registry.register(Backend("low", setOf(capability)), priority = 1)
        registry.register(Backend("high", setOf(capability)), priority = 2)
        assertThat(registry.resolve("test.action")?.backendId).isEqualTo("high")
    }

    @Test
    fun high_risk_requires_confirmation() = runTest {
        val registry = CapabilityRegistry()
        registry.register(Backend("test", setOf(Capability("test.high", "test", Capability.RiskLevel.HIGH))))
        val executor = CapabilityExecutor(registry)
        val rejected = executor.execute(CapabilityRequest("test.high", requestId = "1"))
        assertThat(rejected).isInstanceOf(CapabilityResult.Rejected::class.java)
        assertThat((rejected as CapabilityResult.Rejected).code).isEqualTo(CapabilityResult.RejectionCode.CONFIRMATION_REQUIRED)
    }

    @Test
    fun confirmed_high_risk_executes() = runTest {
        val registry = CapabilityRegistry()
        registry.register(Backend("test", setOf(Capability("test.high", "test", Capability.RiskLevel.HIGH))))
        val executor = CapabilityExecutor(registry)
        val result = executor.execute(CapabilityRequest("test.high", requestId = "2", confirmationGranted = true))
        assertThat(result).isInstanceOf(CapabilityResult.Success::class.java)
    }
}
