package com.jarvis.core.capability

interface CapabilityBackend {
    val backendId: String
    val capabilities: Set<Capability>
    suspend fun execute(request: CapabilityRequest): CapabilityResult
}
