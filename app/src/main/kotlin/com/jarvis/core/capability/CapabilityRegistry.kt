package com.jarvis.core.capability

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CapabilityRegistry {
    private data class Entry(val priority: Int, val backend: CapabilityBackend)

    private val mutex = Mutex()
    private val entries = mutableListOf<Entry>()

    suspend fun register(backend: CapabilityBackend, priority: Int = 0) {
        require(backend.backendId.isNotBlank()) { "Backend id must not be blank." }
        mutex.withLock {
            entries.removeAll { it.backend.backendId == backend.backendId }
            entries += Entry(priority, backend)
            entries.sortWith(
                compareByDescending<Entry> { it.priority }.thenBy { it.backend.backendId }
            )
        }
    }

    suspend fun unregister(backendId: String): Boolean = mutex.withLock {
        val before = entries.size
        entries.removeAll { it.backend.backendId == backendId }
        entries.size != before
    }

    suspend fun find(capabilityId: String): CapabilityBackend? = mutex.withLock {
        entries.firstOrNull { it.backend.supports(capabilityId) }?.backend
    }

    suspend fun describe(capabilityId: String): Capability? = mutex.withLock {
        entries.asSequence()
            .flatMap { it.backend.capabilities.asSequence() }
            .firstOrNull { it.id == capabilityId }
    }

    suspend fun allCapabilities(): List<Capability> = mutex.withLock {
        entries.flatMap { it.backend.capabilities }
            .distinctBy { it.id }
            .sortedBy { it.id }
    }
}
