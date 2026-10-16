package com.jarvis.friday.security

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class SecurityLabTest {
    private fun registry(): SecurityScopeRegistry =
        SecurityScopeRegistry(SecurityScopeProofSigner())

    @Test
    fun unauthorized_scope_is_rejected() {
        val registry = registry()
        val scope = SecurityScope(
            label = "test",
            target = "test.example",
            authorizedByUser = false,
            expiresAt = Instant.now().plusSeconds(60)
        )

        assertThat(registry.enroll(scope)).isFalse()
        assertThat(registry.activeScopes()).isEmpty()
    }

    @Test
    fun expired_scope_is_not_authorized() {
        val registry = registry()
        val scope = SecurityScope(
            label = "expired-test",
            target = "test.example",
            authorizedByUser = true,
            expiresAt = Instant.now().minusSeconds(1)
        )

        assertThat(registry.enroll(scope)).isFalse()
        assertThat(
            registry.isAuthorized(
                scope.id,
                "test.example",
                "network.tcp_probe",
                SecurityRisk.R2_ENUMERATION
            )
        ).isFalse()
    }

    @Test
    fun active_scope_is_limited_to_its_target_and_allowed_capabilities() {
        val registry = registry()
        val scope = SecurityScope(
            label = "authorized-test",
            target = "example.com",
            authorizedByUser = true,
            expiresAt = Instant.now().plusSeconds(60)
        )

        assertThat(registry.enroll(scope)).isTrue()
        assertThat(
            registry.isAuthorized(
                scope.id,
                "example.com",
                "network.tcp_probe",
                SecurityRisk.R2_ENUMERATION
            )
        ).isTrue()
        assertThat(
            registry.isAuthorized(
                scope.id,
                "other.example",
                "network.tcp_probe",
                SecurityRisk.R2_ENUMERATION
            )
        ).isFalse()
        assertThat(
            registry.isAuthorized(
                scope.id,
                "example.com",
                "not-allowed",
                SecurityRisk.R1_PASSIVE
            )
        ).isFalse()
    }

    @Test
    fun exploit_simulator_never_executes() {
        val result = ExploitSimulator().run(SimulatedScenario.SSRF, "authorized-lab")

        assertThat(result.blocked).isTrue()
        assertThat(result.steps.last().result).contains("No external target")
    }

    @Test
    fun monitoring_requires_explicit_consent() = runTest {
        val monitor = InMemoryConsentMonitor()

        assertThat(monitor.enable(false)).isFalse()
        assertThat(monitor.enabled).isFalse()
        assertThat(monitor.enable(true)).isTrue()
        monitor.record(MonitorEvent(source = MonitorSource.APP_EVENTS, summary = "test"))

        assertThat(monitor.recent()).hasSize(1)
    }
}
