package com.jarvis.core.capability

import java.util.concurrent.ConcurrentHashMap

class CapabilityRegistry {
    private data class Binding(val backend: CapabilityBackend, val priority: Int)
    private val bindings = ConcurrentHashMap<String, Binding>()

    fun register(backend: CapabilityBackend, priority: Int = 0) {
        backend.capabilities.forEach { capability ->
            bindings.compute(capability.id) { _, current ->
                if (current == null || priority > current.priority) Binding(backend, priority) else current
            }
        }
    }

    fun resolve(capabilityId: String): CapabilityBackend? = bindings[capabilityId]?.backend
    fun capability(capabilityId: String): Capability? = bindings[capabilityId]?.backend?.capabilities?.firstOrNull { it.id == capabilityId }
    fun all(): List<Capability> = bindings.values.flatMap { it.backend.capabilities }.distinctBy { it.id }.sortedBy { it.id }
    fun backendIds(): List<String> = bindings.values.map { it.backend.backendId }.distinct().sorted()
}
