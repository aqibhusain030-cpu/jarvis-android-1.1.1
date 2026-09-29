package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.config.ConfigRepository
import com.jarvis.security.SecurityPolicy
import io.mockk.mockk
import org.junit.Test

class SecurityPolicyHardeningTest {
    private val config: ConfigRepository = mockk(relaxed = true)
    private val policy = SecurityPolicy(config)

    @Test fun openUrlRejectsLoopback() {
        assertThat(policy.isAllowedUrl("https://127.0.0.1/admin")).isFalse()
    }

    @Test fun openUrlRejectsNonHttps() {
        assertThat(policy.isAllowedUrl("http://example.com")).isFalse()
    }

    @Test fun browserUrlRejectsPrivateIpv4() {
        assertThat(policy.isSafeExternalUrl("https://192.168.1.1/")).isFalse()
    }

    @Test fun browserUrlRejectsUserInfo() {
        assertThat(policy.isSafeExternalUrl("https://user@example.com/")).isFalse()
    }
}
