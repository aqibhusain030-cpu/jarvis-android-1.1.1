package com.jarvis.core.capability

interface CapabilityPolicy {
    fun allows(capability: Capability, request: CapabilityRequest): Boolean
    fun requiresConfirmation(capability: Capability): Boolean
}

class DefaultCapabilityPolicy : CapabilityPolicy {
    override fun allows(capability: Capability, request: CapabilityRequest): Boolean =
        !(capability.risk == Capability.RiskLevel.CRITICAL && request.source != CapabilityRequest.Source.USER)

    override fun requiresConfirmation(capability: Capability): Boolean =
        capability.risk == Capability.RiskLevel.HIGH || capability.risk == Capability.RiskLevel.CRITICAL
}
