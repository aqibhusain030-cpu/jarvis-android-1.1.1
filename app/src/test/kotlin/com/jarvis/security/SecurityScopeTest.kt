package com.jarvis.security

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.security.*
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SecurityScopeTest {
    @Test
    fun wildcardMatchesSubdomainButNotUnrelatedHost() {
        assertThat(TargetMatcher.matches("*.example.com", "api.example.com")).isTrue()
        assertThat(TargetMatcher.matches("*.example.com", "evil-example.com")).isFalse()
    }

    @Test
    fun cidrMatchesOnlyAddressesInsideNetwork() {
        assertThat(TargetMatcher.matches("192.168.1.0/24", "192.168.1.50")).isTrue()
        assertThat(TargetMatcher.matches("192.168.1.0/24", "192.168.2.50")).isFalse()
    }

    @Test
    fun expiredScopeIsNotAuthorized() {
        val signer = SecurityScopeProofSigner()
        val registry = SecurityScopeRegistry(signer)
        val scope = SecurityScope(
            label = "lab",
            target = "192.168.1.0/24",
            authorizedByUser = true,
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        assertThat(registry.enroll(scope)).isTrue()
        assertThat(registry.isAuthorized(scope.id, "192.168.1.50", "port_scan", SecurityRisk.R2_ENUMERATION)).isTrue()
    }
}
