package com.jarvis.friday.security

/** Ordered risk levels used by the authorization scope registry. */
enum class SecurityRisk(val level: Int) {
    R0_OBSERVATION(0),
    R1_LOW(1),
    R2_MODERATE(2),
    R3_VALIDATION(3)
}
