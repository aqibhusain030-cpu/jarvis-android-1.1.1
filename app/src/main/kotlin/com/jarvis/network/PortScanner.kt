package com.jarvis.network

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

data class PortScanResult(val host: String, val scannedPorts: List<Int>, val openPorts: List<Int>, val scopeId: String)

@Singleton
class PortScanner @Inject constructor(
    private val probe: TcpProbe,
    private val scopes: SecurityScopeRegistry
) {
    suspend fun scan(scopeId: String, host: String, ports: List<Int>): PortScanResult {
        val uniquePorts = ports.distinct().filter { it in 1..65535 }.take(256)
        require(uniquePorts.isNotEmpty())
        check(scopes.isAuthorized(scopeId, host, "port_scan", SecurityRisk.R2_ENUMERATION))
        val semaphore = Semaphore(16)
        val rows = coroutineScope {
            uniquePorts.map { port -> async { semaphore.withPermit { port to probe.probe(host, port) } } }.awaitAll()
        }
        return PortScanResult(host, uniquePorts, rows.filter { it.second }.map { it.first }, scopeId)
    }
}
