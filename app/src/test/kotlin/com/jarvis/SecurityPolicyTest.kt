package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.config.ConfigRepository
import com.jarvis.security.SecurityPolicy
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class SecurityPolicyTest {
    private val config: ConfigRepository = mockk(relaxed = true)
    private val policy = SecurityPolicy(config)

    @Test fun smsRequiresConfirmation() {
        assertThat(policy.requiresConfirmation("send_sms")).isTrue()
        assertThat(policy.requiresConfirmation("make_call")).isTrue()
        assertThat(policy.requiresConfirmation("file_delete")).isTrue()
        assertThat(policy.requiresConfirmation("http_post")).isTrue()
    }
    @Test fun batteryDoesNotRequire() {
        assertThat(policy.requiresConfirmation("battery_status")).isFalse()
    }
    @Test fun smsRequiresPinWhenEnabled() {
        every { config.current.value.pinRequiredForHighRisk } returns true
        assertThat(policy.requiresPin("send_sms")).isTrue()
        assertThat(policy.requiresPin("battery_status")).isFalse()
    }
}
