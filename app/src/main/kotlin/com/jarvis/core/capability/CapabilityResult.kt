package com.jarvis.core.capability

sealed interface CapabilityResult {
    data class Success(
        val requestId: String,
        val message: String,
        val data: Map<String, String> = emptyMap()
    ) : CapabilityResult

    data class Rejected(
        val requestId: String,
        val code: RejectionCode,
        val message: String
    ) : CapabilityResult

    data class Failed(
        val requestId: String,
        val code: FailureCode,
        val message: String,
        val recoverable: Boolean = true
    ) : CapabilityResult

    enum class RejectionCode {
        UNKNOWN_CAPABILITY, INVALID_ARGUMENTS, CONFIRMATION_REQUIRED,
        POLICY_DENIED, MISSING_PERMISSION, UNSUPPORTED
    }

    enum class FailureCode {
        BACKEND_UNAVAILABLE, ACTIVITY_NOT_FOUND, PACKAGE_NOT_FOUND,
        SECURITY_EXCEPTION, EXECUTION_FAILED, INTERNAL_ERROR
    }
}
