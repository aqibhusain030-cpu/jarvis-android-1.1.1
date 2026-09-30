package com.jarvis.core.capability

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface CapabilityBackend {
    val backendId: String
    val capabilities: Set<Capability>
    fun supports(capabilityId: String): Boolean =
        capabilities.any { it.id == capabilityId }
    suspend fun execute(request: CapabilityRequest): CapabilityResult
}

abstract class IoCapabilityBackend(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CapabilityBackend {
    protected suspend fun <T> io(block: () -> T): T = withContext(dispatcher, block)
}
