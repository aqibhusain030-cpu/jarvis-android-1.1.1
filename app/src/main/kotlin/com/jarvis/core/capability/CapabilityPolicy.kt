package com.jarvis.core.capability

class CapabilityPolicy(
    private val confirmationProvider: ConfirmationProvider
) {
    fun validate(
        capability: Capability,
        request: CapabilityRequest
    ): CapabilityResult? {
        if (capability.risk == Capability.RiskLevel.CRITICAL &&
            !request.confirmationGranted
        ) {
            return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.CONFIRMATION_REQUIRED,
                "Confirmation is required for this action."
            )
        }

        if (capability.risk == Capability.RiskLevel.HIGH &&
            !request.confirmationGranted &&
            confirmationProvider.requiresConfirmation(capability)
        ) {
            return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.CONFIRMATION_REQUIRED,
                "Confirmation is required before continuing."
            )
        }

        return null
    }

    interface ConfirmationProvider {
        fun requiresConfirmation(capability: Capability): Boolean
    }
}

class DefaultCapabilityPolicy : CapabilityPolicy.ConfirmationProvider {
    override fun requiresConfirmation(capability: Capability): Boolean =
        capability.risk == Capability.RiskLevel.HIGH ||
            capability.risk == Capability.RiskLevel.CRITICAL
}
