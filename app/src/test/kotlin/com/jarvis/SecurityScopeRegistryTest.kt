package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import org.junit.Test

class SecurityScopeRegistryTest {
    private fun grant(
        registry: SecurityScopeRegistry,
        scopeId: String = "test_scope",
        targets: Set<String> = setOf("192.168.1.20"),
        actions: Set<String> = setOf(SecurityScopeRegistry.ACTION_PORT_SCAN),
        maxRisk: SecurityRisk = SecurityRisk.R3_VALIDATION,
        confirmed: Boolean = true,
        expiry: Long = System.currentTimeMillis() + 60_000
    ) = registry.grant(scopeId, targets, actions, maxRisk, expiry, confirmed)

    @Test fun refusesGrantWithoutExplicitConfirmation() {
        val registry = SecurityScopeRegistry()
        assertThat(grant(registry, confirmed = false)).isFalse()
        assertThat(registry.isAuthorized("test_scope", "192.168.1.20", "port_scan", SecurityRisk.R0_OBSERVATION)).isFalse()
    }

    @Test fun grantMatchesOnlyExactTargetAndAction() {
        val registry = SecurityScopeRegistry()
        assertThat(grant(registry)).isTrue()
        assertThat(registry.isAuthorized("test_scope", "192.168.1.20", "port_scan", SecurityRisk.R3_VALIDATION)).isTrue()
        assertThat(registry.isAuthorized("test_scope", "192.168.1.21", "port_scan", SecurityRisk.R1_LOW)).isFalse()
        assertThat(registry.isAuthorized("test_scope", "192.168.1.20", "shell", SecurityRisk.R0_OBSERVATION)).isFalse()
    }

    @Test fun grantCannotExceedRiskOrExpiry() {
        val registry = SecurityScopeRegistry()
        assertThat(grant(registry, maxRisk = SecurityRisk.R1_LOW)).isTrue()
        assertThat(registry.isAuthorized("test_scope", "192.168.1.20", "port_scan", SecurityRisk.R3_VALIDATION)).isFalse()
        assertThat(grant(registry, scopeId = "expired", expiry = System.currentTimeMillis() - 1)).isFalse()
    }

    @Test fun rejectsWildcardAndCidrTargets() {
        val registry = SecurityScopeRegistry()
        assertThat(grant(registry, targets = setOf("192.168.1.0/24"))).isFalse()
        assertThat(grant(registry, targets = setOf("*.example.com"))).isFalse()
    }

    @Test fun revokeImmediatelyRemovesGrant() {
        val registry = SecurityScopeRegistry()
        assertThat(grant(registry)).isTrue()
        assertThat(registry.revoke("test_scope")).isTrue()
        assertThat(registry.isAuthorized("test_scope", "192.168.1.20", "port_scan", SecurityRisk.R0_OBSERVATION)).isFalse()
    }
}
