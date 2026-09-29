package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.memory.MemoryStore
import com.jarvis.security.AuditLogger
import com.jarvis.security.KillSwitch
import com.jarvis.security.SecurityPolicy
import com.jarvis.tools.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ToolDispatcherTest {
    private lateinit var registry: ToolRegistry
    private lateinit var policy: SecurityPolicy
    private lateinit var confirmation: ConfirmationManager
    private lateinit var rate: RateLimiter
    private lateinit var audit: AuditLogger
    private lateinit var config: ConfigRepository
    private lateinit var log: JarvisLogger
    private lateinit var kill: KillSwitch
    private lateinit var killState: MutableStateFlow<Boolean>
    private lateinit var dispatcher: ToolDispatcher
    private lateinit var unlock: com.jarvis.security.SecurityUnlockManager
    private lateinit var permissionGate: PermissionGate
    private lateinit var memory: MemoryStore

    private class FakeTool(override val spec: ToolSpec, val result: ToolResult) : Tool {
        override suspend fun invoke(args: Map<String, Any?>) = result
    }

    @Before
    fun setUp() {
        registry = ToolRegistry()
        policy = mockk(relaxed = true)
        confirmation = mockk(relaxed = true)
        rate = mockk(relaxed = true)
        audit = mockk(relaxed = true)
        config = mockk(relaxed = true)
        log = mockk(relaxed = true)
        kill = mockk(relaxed = true)
        killState = MutableStateFlow(false)
        every { kill.isKilled } returns killState
        unlock = mockk(relaxed = true)
        permissionGate = mockk(relaxed = true)
        memory = mockk(relaxed = true)
        every { permissionGate.missing(any()) } returns emptyList()
        every { unlock.consume() } returns true
        every { policy.toolAllowed(any()) } returns true
        every { policy.requiresConfirmation(any()) } returns false
        every { policy.requiresPin(any()) } returns false
        every { policy.networkAllowed(any(), any()) } returns true
        every { rate.allow(any(), any()) } returns true
        io.mockk.coEvery { confirmation.ask(any()) } returns true
        io.mockk.coEvery { confirmation.ask(any(), any()) } returns true
        dispatcher = ToolDispatcher(
            registry,
            policy,
            confirmation,
            unlock,
            rate,
            audit,
            config,
            kill,
            log,
            permissionGate,
            memory
        )
    }

    @Test
    fun unknownToolIsError() = runTest {
        val r = dispatcher.invoke("does_not_exist", emptyMap())
        assertThat(r).isInstanceOf(ToolResult.Err::class.java)
    }

    @Test
    fun successfulToolReturnsOk() = runTest {
        registry.register(
            FakeTool(ToolSpec("battery_status", "बैटरी", "Battery"), ToolResult.Ok("42%"))
        )
        val r = dispatcher.invoke("battery_status", emptyMap())
        assertThat(r).isInstanceOf(ToolResult.Ok::class.java)
        assertThat((r as ToolResult.Ok).data).isEqualTo("42%")
    }

    @Test
    fun cancelledConfirmationReturnsErr() = runTest {
        every { policy.requiresConfirmation("send_sms") } returns true
        registry.register(
            FakeTool(
                ToolSpec("send_sms", "SMS", "SMS", parameters = mapOf("number" to ParamSpec("string")), confirmation = Confirmation.VOICE),
                ToolResult.Ok("sent")
            )
        )
        io.mockk.coEvery { confirmation.ask(any()) } returns false
        io.mockk.coEvery { confirmation.ask(any(), any()) } returns false
        val r = dispatcher.invoke("send_sms", mapOf("number" to "123"))
        assertThat(r).isInstanceOf(ToolResult.Err::class.java)
        assertThat((r as ToolResult.Err).code).isEqualTo("cancelled")
    }

    @Test
    fun killSwitchBlocks() = runTest {
        killState.value = true

        registry.register(
            FakeTool(
                ToolSpec("make_call", "कॉल", "Call"),
                ToolResult.Ok("called")
            )
        )
        val r = dispatcher.invoke("make_call", emptyMap())

        assertThat(r).isInstanceOf(ToolResult.Err::class.java)
        assertThat((r as ToolResult.Err).code).isEqualTo("killed")
    }

    @Test
    fun policyConfirmationIsAuthoritative() = runTest {
        // Even if ToolSpec says NONE, policy requiring confirmation must ask.
        every { policy.requiresConfirmation("http_get") } returns true
        registry.register(
            FakeTool(ToolSpec("http_get", "HTTP", "HTTP", parameters = mapOf("url" to ParamSpec("string")), confirmation = Confirmation.NONE), ToolResult.Ok("ok"))
        )
        io.mockk.coEvery { confirmation.ask(any()) } returns false
        io.mockk.coEvery { confirmation.ask(any(), any()) } returns false
        val r = dispatcher.invoke("http_get", mapOf("url" to "https://example.com"))
        assertThat((r as ToolResult.Err).code).isEqualTo("cancelled")
    }

    @Test
    fun networkDeniedByPolicy() = runTest {
        every { policy.networkAllowed("http_get", any()) } returns false
        registry.register(
            FakeTool(ToolSpec("http_get", "HTTP", "HTTP", parameters = mapOf("url" to ParamSpec("string"))), ToolResult.Ok("ok"))
        )
        val r = dispatcher.invoke("http_get", mapOf("url" to "https://evil.example"))
        assertThat((r as ToolResult.Err).code).isEqualTo("network_denied")
    }

    @Test
    fun unknownParameterIsRejectedBeforeAuthorization() = runTest {
        registry.register(
            FakeTool(
                ToolSpec(
                    "set_volume", "Volume", "Volume",
                    parameters = mapOf("level" to ParamSpec("int"))
                ),
                ToolResult.Ok("ok")
            )
        )
        val r = dispatcher.invoke("set_volume", mapOf("level" to 50, "unexpected" to true))
        assertThat((r as ToolResult.Err).code).isEqualTo("bad_args")
    }

    @Test
    fun wrongParameterTypeIsRejected() = runTest {
        registry.register(
            FakeTool(
                ToolSpec(
                    "set_volume", "Volume", "Volume",
                    parameters = mapOf("level" to ParamSpec("int"))
                ),
                ToolResult.Ok("ok")
            )
        )
        val r = dispatcher.invoke("set_volume", mapOf("level" to "50"))
        assertThat((r as ToolResult.Err).code).isEqualTo("bad_args")
    }

    @Test
    fun missingRequiredParameterIsRejected() = runTest {
        registry.register(
            FakeTool(
                ToolSpec(
                    "send_sms", "SMS", "SMS",
                    parameters = mapOf(
                        "number" to ParamSpec("string"),
                        "text" to ParamSpec("string")
                    )
                ),
                ToolResult.Ok("ok")
            )
        )
        val r = dispatcher.invoke("send_sms", mapOf("number" to "123"))
        assertThat((r as ToolResult.Err).code).isEqualTo("bad_args")
    }
}
