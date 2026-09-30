package com.jarvis.friday.security

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class SecurityLabTest {
    @Test fun unauthorized_scope_is_rejected() {
        val registry = SecurityScopeRegistry()
        assertThat(
            registry.enroll(
                SecurityScope(
                    label = "test",
                    target = "test",
                    authorizedByUser = false,
                    expiresAt = Instant.now().plusSeconds(60)
                )
            )
        ).isFalse()
    }

    @Test fun expired_scope_is_not_authorized() {
        val registry = SecurityScopeRegistry()
        val scope = SecurityScope(
            label = "test",
            target = "test",
            authorizedByUser = true,
            expiresAt = Instant.now().minusSeconds(1)
        )
        assertThat(registry.enroll(scope)).isFalse()
        assertThat(registry.isAuthorized("test")).isFalse()
    }

    @Test fun exploit_simulator_never_executes() {
        val result = ExploitSimulator().run(SimulatedScenario.SSRF, "authorized-lab")
        assertThat(result.blocked).isTrue()
        assertThat(result.steps.last().result).contains("No external target")
    }

    @Test fun monitoring_requires_explicit_consent() = runTest {
        val monitor = InMemoryConsentMonitor()
        assertThat(monitor.enable(false)).isFalse()
        assertThat(monitor.enabled).isFalse()
        assertThat(monitor.enable(true)).isTrue()
        monitor.record(MonitorEvent(source = MonitorSource.APP_EVENTS, summary = "test"))
        assertThat(monitor.recent()).hasSize(1)
    }
}
