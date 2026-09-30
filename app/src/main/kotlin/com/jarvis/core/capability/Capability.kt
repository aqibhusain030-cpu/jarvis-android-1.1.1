package com.jarvis.core.capability

data class Capability(
    val id: String,
    val description: String,
    val risk: RiskLevel = RiskLevel.LOW,
    val requiresForeground: Boolean = false,
    val requiresAccessibility: Boolean = false,
    val requiresPackageVisibility: Boolean = false,
    val aliases: Set<String> = emptySet()
) {
    init {
        require(id.isNotBlank()) { "Capability id must not be blank." }
        require(description.isNotBlank()) { "Capability description must not be blank." }
    }

    enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
}
