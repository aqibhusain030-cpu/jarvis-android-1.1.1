package com.jarvis.network

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

enum class PortState { OPEN, CLOSED, FILTERED, ERROR, NOT_SCANNED }

/** Result of bounded TCP connection checks. A timeout is unknown, never CLOSED. */
data class PortScanResult(
    val target: String,
    val requestedPorts: List<Int>,
    val openPorts: Set<Int> = emptySet(),
    val closedPorts: Set<Int> = emptySet(),
    val filteredPorts: Set<Int> = emptySet(),
    val errorPorts: Set<Int> = emptySet(),
    val durationMs: Long = 0L,
    val authorized: Boolean,
    val completed: Boolean,
    val error: String? = null
) {
    val isComplete: Boolean
        get() = authorized && completed &&
            filteredPorts.isEmpty() && errorPorts.isEmpty() && error == null

    fun stateFor(port: Int): PortState = when (port) {
        in openPorts -> PortState.OPEN
        in closedPorts -> PortState.CLOSED
        in filteredPorts -> PortState.FILTERED
        in errorPorts -> PortState.ERROR
        else -> PortState.NOT_SCANNED
    }
}

/**
 * Exact-target TCP checker. Every scan requires a matching unexpired scope,
 * and is limited to one host and 32 distinct ports.
 */
@Singleton
class NetworkEngine @Inject constructor(
    private val scopes: SecurityScopeRegistry
) {
    suspend fun scan(
        scopeId: String,
        target: String,
        ports: List<Int>,
        action: String = SecurityScopeRegistry.ACTION_PORT_SCAN,
        risk: SecurityRisk = SecurityRisk.R3_VALIDATION
    ): PortScanResult {
        val startedAt = System.nanoTime()
        val host = scopes.canonicalizeTarget(target)
            ?: return failure(target, ports, "Invalid target host.", startedAt)

        val uniquePorts = ports.distinct()
        if (uniquePorts.isEmpty()) {
            return failure(host, uniquePorts, "At least one TCP port is required.", startedAt)
        }
        if (uniquePorts.size > MAX_PORTS_PER_SCAN) {
            return failure(host, uniquePorts, "A scan is limited to $MAX_PORTS_PER_SCAN ports.", startedAt)
        }
        if (uniquePorts.any { it !in 1..65535 }) {
            return failure(host, uniquePorts, "Ports must be integers from 1 to 65535.", startedAt)
        }
        if (!scopes.isAuthorized(scopeId, host, action, risk)) {
            return failure(
                host, uniquePorts,
                "Security scope does not authorize this target and operation.",
                startedAt
            )
        }

        return withContext(Dispatchers.IO) {
            val open = linkedSetOf<Int>()
            val closed = linkedSetOf<Int>()
            val filtered = linkedSetOf<Int>()
            val errors = linkedSetOf<Int>()

            val address = try {
                InetAddress.getAllByName(host).firstOrNull()
                    ?: return@withContext failure(
                        host, uniquePorts, "Target could not be resolved.", startedAt, authorized = true
                    )
            } catch (_: IOException) {
                return@withContext failure(
                    host, uniquePorts, "Target resolution failed; no port state was inferred.",
                    startedAt, authorized = true
                )
            } catch (_: SecurityException) {
                return@withContext failure(
                    host, uniquePorts, "Target resolution was denied; no port state was inferred.",
                    startedAt, authorized = true
                )
            }

            for (port in uniquePorts) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS)
                    }
                    open.add(port)
                } catch (_: ConnectException) {
                    closed.add(port)
                } catch (_: SocketTimeoutException) {
                    filtered.add(port)
                } catch (_: NoRouteToHostException) {
                    filtered.add(port)
                } catch (_: IOException) {
                    errors.add(port)
                } catch (_: SecurityException) {
                    errors.add(port)
                }
            }

            PortScanResult(
                target = host,
                requestedPorts = uniquePorts,
                openPorts = open,
                closedPorts = closed,
                filteredPorts = filtered,
                errorPorts = errors,
                durationMs = elapsedMs(startedAt),
                authorized = true,
                completed = true
            )
        }
    }

    private fun failure(
        target: String,
        ports: List<Int>,
        message: String,
        startedAt: Long,
        authorized: Boolean = false
    ) = PortScanResult(
        target = target,
        requestedPorts = ports.distinct(),
        durationMs = elapsedMs(startedAt),
        authorized = authorized,
        completed = false,
        error = message
    )

    private fun elapsedMs(startedAt: Long): Long =
        ((System.nanoTime() - startedAt).coerceAtLeast(0L)) / 1_000_000L

    companion object {
        const val MAX_PORTS_PER_SCAN = 32
        private const val CONNECT_TIMEOUT_MS = 600
    }
}
