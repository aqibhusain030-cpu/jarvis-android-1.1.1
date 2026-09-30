package com.jarvis.core.capability

data class CapabilityRequest(
    val capabilityId: String,
    val arguments: Map<String, String> = emptyMap(),
    val requestId: String,
    val source: Source = Source.ASSISTANT,
    val confirmationGranted: Boolean = false
) { enum class Source { ASSISTANT, USER, SYSTEM, AUTOMATION } }
