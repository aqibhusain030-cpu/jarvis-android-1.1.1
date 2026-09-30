package com.jarvis.network

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkEngine @Inject constructor(
    private val dns: DnsResolver,
    private val scanner: PortScanner,
    private val fingerprint: ServiceFingerprinter,
    private val scopes: SecurityScopeRegistry,
    private val inventory: NetworkInventory,
    private val events: NetworkEventBus
) {
    suspend fun resolve(scopeId: String, host: String): List<String> {
        check(scopes.isAuthorized(scopeId, host, "dns_resolve", SecurityRisk.R0_INFORMATION))
        return dns.resolve(host).also { it.forEach { address -> events.publish(NetworkEvent.HostDiscovered(scopeId, address)) } }
    }

    suspend fun scan(scopeId: String, host: String, ports: List<Int>): PortScanResult {
        val result = scanner.scan(scopeId, host, ports)
        result.openPorts.forEach { port ->
            val service = fingerprint.identify(port)
            inventory.recordPort(scopeId, host, NetworkService(port, "tcp", "open", service))
            events.publish(NetworkEvent.PortDiscovered(scopeId, host, port, "tcp"))
            events.publish(NetworkEvent.ServiceDiscovered(scopeId, host, service, port))
        }
        return result
    }
}
