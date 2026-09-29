package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.config.ConfigRepository
import com.jarvis.config.JarvisConfig
import com.jarvis.security.SecurityPolicy
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test

class SecurityPolicyTest {
    private val config: ConfigRepository = mockk()
    private val current = MutableStateFlow(JarvisConfig())
    private val policy = SecurityPolicy(config)

    init {
        every { config.current } returns current
    }

    @Test
    fun smsRequiresConfirmation() {
        assertThat(policy.requiresConfirmation("send_sms")).isTrue()
        assertThat(policy.requiresConfirmation("make_call")).isTrue()
        assertThat(policy.requiresConfirmation("file_delete")).isTrue()
        assertThat(policy.requiresConfirmation("http_post")).isTrue()
    }

    @Test
    fun batteryDoesNotRequire() {
        assertThat(policy.requiresConfirmation("battery_status")).isFalse()
    }

    @Test
    fun smsRequiresPinWhenEnabled() {
        current.value = current.value.copy(pinRequiredForHighRisk = true)

        assertThat(policy.requiresPin("send_sms")).isTrue()
        assertThat(policy.requiresPin("battery_status")).isFalse()
    }
}