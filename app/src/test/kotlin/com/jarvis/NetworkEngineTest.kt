package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.network.NetworkEngine
import com.jarvis.network.PortState
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket

class NetworkEngineTest {
    @Test fun scanFailsClosedWithoutScope() = runBlocking {
        val result = NetworkEngine(SecurityScopeRegistry()).scan("missing_scope", "127.0.0.1", listOf(80))
        assertThat(result.authorized).isFalse()
        assertThat(result.error).contains("does not authorize")
        assertThat(result.stateFor(80)).isEqualTo(PortState.NOT_SCANNED)
    }

    @Test fun scanReportsListeningPortAsOpen() = runBlocking {
        val registry = SecurityScopeRegistry()
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        try {
            val port = server.localPort
            assertThat(registry.grant(
                scopeId = "loopback_test",
                targets = setOf("127.0.0.1"),
                allowedActions = setOf(SecurityScopeRegistry.ACTION_PORT_SCAN),
                maxRisk = SecurityRisk.R3_VALIDATION,
                expiresAtEpochMillis = System.currentTimeMillis() + 60_000,
                explicitlyConfirmed = true
            )).isTrue()

            val result = NetworkEngine(registry).scan("loopback_test", "127.0.0.1", listOf(port))
            assertThat(result.authorized).isTrue()
            assertThat(result.stateFor(port)).isEqualTo(PortState.OPEN)
            assertThat(result.openPorts).contains(port)
        } finally {
            server.close()
        }
    }

    @Test fun scanRejectsUnboundedPortLists() = runBlocking {
        val registry = SecurityScopeRegistry()
        assertThat(registry.grant(
            scopeId = "bounded_test",
            targets = setOf("127.0.0.1"),
            allowedActions = setOf(SecurityScopeRegistry.ACTION_PORT_SCAN),
            maxRisk = SecurityRisk.R3_VALIDATION,
            expiresAtEpochMillis = System.currentTimeMillis() + 60_000,
            explicitlyConfirmed = true
        )).isTrue()

        val result = NetworkEngine(registry).scan(
            "bounded_test", "127.0.0.1",
            (1..(NetworkEngine.MAX_PORTS_PER_SCAN + 1)).toList()
        )
        assertThat(result.authorized).isFalse()
        assertThat(result.error).contains("limited to")
    }
}
