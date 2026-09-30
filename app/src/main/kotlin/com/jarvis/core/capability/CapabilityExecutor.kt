package com.jarvis.core.capability

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

class CapabilityExecutor(
    private val registry: CapabilityRegistry,
    private val policyProvider: CapabilityPolicy = DefaultCapabilityPolicy()
) {
    suspend fun execute(request: CapabilityRequest): CapabilityResult {
        val backend = registry.resolve(request.capabilityId)
            ?: return CapabilityResult.Rejected(request.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unknown capability: " + request.capabilityId)
        val capability = registry.capability(request.capabilityId)
            ?: return CapabilityResult.Rejected(request.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Capability metadata is unavailable.")
        if (!policyProvider.allows(capability, request))
            return CapabilityResult.Rejected(request.requestId, CapabilityResult.RejectionCode.POLICY_DENIED, "Capability denied by policy.")
        if (policyProvider.requiresConfirmation(capability) && !request.confirmationGranted)
            return CapabilityResult.Rejected(request.requestId, CapabilityResult.RejectionCode.CONFIRMATION_REQUIRED, "Explicit confirmation is required.")
        return try {
            withTimeout(capability.timeoutMs.coerceIn(1_000L, 120_000L)) { backend.execute(request) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            CapabilityResult.Failed(request.requestId, CapabilityResult.FailureCode.EXECUTION_FAILED, e.message ?: "Android security policy rejected the operation.")
        } catch (e: Exception) {
            CapabilityResult.Failed(request.requestId, CapabilityResult.FailureCode.BACKEND_ERROR, e.message ?: "Capability backend failed.")
        }
    }
}
