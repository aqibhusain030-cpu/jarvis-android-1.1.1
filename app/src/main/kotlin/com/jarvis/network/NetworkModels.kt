package com.jarvis.network

import java.time.Instant

data class NetworkService(
    val port: Int,
    val transport: String,
    val state: String,
    val service: String? = null,
    val evidence: Map<String, String> = emptyMap()
)

data class NetworkAsset(
    val assetId: String,
    val address: String?,
    val hostname: String?,
    val openServices: List<NetworkService> = emptyList(),
    val firstSeen: Instant = Instant.now(),
    val lastSeen: Instant = Instant.now(),
    val scopeId: String
)

data class TargetProfile(
    val target: String,
    val identity: Map<String, String>,
    val exposure: List<NetworkService>,
    val evidence: List<String>,
    val confidence: Double,
    val timestamp: Instant = Instant.now()
)

sealed interface NetworkEvent {
    val timestamp: Instant
    val scopeId: String
    data class HostDiscovered(override val scopeId: String, val host: String, override val timestamp: Instant = Instant.now()) : NetworkEvent
    data class PortDiscovered(override val scopeId: String, val host: String, val port: Int, val transport: String, override val timestamp: Instant = Instant.now()) : NetworkEvent
    data class ServiceDiscovered(override val scopeId: String, val host: String, val service: String, val port: Int, override val timestamp: Instant = Instant.now()) : NetworkEvent
    data class TlsObserved(override val scopeId: String, val host: String, val port: Int, val subject: String?, val issuer: String?, override val timestamp: Instant = Instant.now()) : NetworkEvent
    data class HttpObserved(override val scopeId: String, val host: String, val port: Int, val statusCode: Int, override val timestamp: Instant = Instant.now()) : NetworkEvent
}

class NetworkEventBus {
    private val listeners = java.util.concurrent.CopyOnWriteArrayList<suspend (NetworkEvent) -> Unit>()
    fun subscribe(listener: suspend (NetworkEvent) -> Unit) { listeners += listener }
    suspend fun publish(event: NetworkEvent) { for (listener in listeners) listener(event) }
}
