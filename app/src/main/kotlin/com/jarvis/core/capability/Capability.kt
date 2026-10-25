package com.jarvis.core.capability

data class Capability(
    val id: String,
    val description: String,
    val risk: RiskLevel = RiskLevel.LOW,
    val requiresAccessibility: Boolean = false,
    val requiresPackageVisibility: Boolean = false
) { enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL } }
