package com.jarvis.core.capability

import kotlinx.coroutines.CancellationException

class CapabilityExecutor(
    private val registry: CapabilityRegistry,
    private val policyProvider: CapabilityPolicy
) {
    suspend fun execute(request: CapabilityRequest): CapabilityResult {
        val capability = registry.describe(request.capabilityId)
            ?: return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY,
                "Unknown capability."
            )

        if (request.arguments.any { (key, value) ->
                key.isBlank() || value.length > MAX_ARGUMENT_LENGTH
            }) {
            return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.INVALID_ARGUMENTS,
                "Invalid capability arguments."
            )
        }

        policyProvider.validate(capability, request)?.let { return it }

        val backend = registry.find(request.capabilityId)
            ?: return CapabilityResult.Failed(
                request.requestId,
                CapabilityResult.FailureCode.BACKEND_UNAVAILABLE,
                "Capability backend is unavailable."
            )

        return try {
            backend.execute(request)
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            CapabilityResult.Failed(
                request.requestId,
                CapabilityResult.FailureCode.SECURITY_EXCEPTION,
                "Android denied this operation.",
                recoverable = false
            )
        } catch (_: Throwable) {
            CapabilityResult.Failed(
                request.requestId,
                CapabilityResult.FailureCode.EXECUTION_FAILED,
                "The operation could not be completed."
            )
        }
    }

    private companion object { const val MAX_ARGUMENT_LENGTH = 4096 }
}
