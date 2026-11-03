package com.jarvis.core.capability

import java.util.UUID

data class CapabilityRequest(
    val capabilityId: String,
    val arguments: Map<String, String> = emptyMap(),
    val requestId: String = UUID.randomUUID().toString(),
    val source: Source = Source.ASSISTANT,
    val confirmationGranted: Boolean = false
) {
    init {
        require(capabilityId.isNotBlank()) { "capabilityId must not be blank." }
        require(requestId.isNotBlank()) { "requestId must not be blank." }
    }

    enum class Source { ASSISTANT, USER_INTERFACE, AUTOMATION, INTERNAL }
}
