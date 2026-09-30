package com.jarvis.friday.security

data class SecurityFinding(
    val id: String,
    val severity: Severity,
    val title: String,
    val evidence: String,
    val remediation: String
) {
    enum class Severity { INFO, LOW, MEDIUM, HIGH, CRITICAL }
}

data class SecurityAssessment(
    val target: String,
    val findings: List<SecurityFinding>,
    val score: Int
)

class SecurityAssessmentEngine {
    fun assessManifest(
        target: String,
        cleartextDisabled: Boolean,
        allowBackupDisabled: Boolean,
        accessibilityServiceExported: Boolean,
        excessivePackageVisibility: Boolean
    ): SecurityAssessment {
        val findings = buildList {
            if (!cleartextDisabled) add(
                SecurityFinding(
                    "APP-CLEAR-001",
                    SecurityFinding.Severity.HIGH,
                    "Cleartext traffic enabled",
                    "Application permits non-TLS network traffic.",
                    "Disable cleartext traffic and require HTTPS."
                )
            )
            if (!allowBackupDisabled) add(
                SecurityFinding(
                    "APP-BACKUP-001",
                    SecurityFinding.Severity.MEDIUM,
                    "Application backup enabled",
                    "Application data may be exposed through backup paths.",
                    "Disable backup for sensitive applications or explicitly scope backup content."
                )
            )
            if (accessibilityServiceExported) add(
                SecurityFinding(
                    "APP-ACCESS-001",
                    SecurityFinding.Severity.HIGH,
                    "Accessibility service exported",
                    "The accessibility service is externally addressable.",
                    "Keep the service non-exported and protected by BIND_ACCESSIBILITY_SERVICE."
                )
            )
            if (excessivePackageVisibility) add(
                SecurityFinding(
                    "APP-PACKAGE-001",
                    SecurityFinding.Severity.MEDIUM,
                    "Excessive package visibility",
                    "The application declares broad package visibility.",
                    "Use the narrowest package visibility needed for the feature."
                )
            )
        }

        val penalty = findings.sumOf {
            when (it.severity) {
                SecurityFinding.Severity.INFO -> 0
                SecurityFinding.Severity.LOW -> 5
                SecurityFinding.Severity.MEDIUM -> 12
                SecurityFinding.Severity.HIGH -> 22
                SecurityFinding.Severity.CRITICAL -> 40
            }
        }

        return SecurityAssessment(
            target = target,
            findings = findings,
            score = (100 - penalty).coerceIn(0, 100)
        )
    }
}
