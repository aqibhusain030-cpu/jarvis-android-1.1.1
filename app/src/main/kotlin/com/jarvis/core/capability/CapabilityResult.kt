package com.jarvis.core.capability

sealed interface CapabilityResult {
    val requestId: String
    data class Success(override val requestId: String, val message: String, val data: Map<String, String> = emptyMap()) : CapabilityResult
    data class Rejected(override val requestId: String, val code: RejectionCode, val message: String) : CapabilityResult
    data class Failed(override val requestId: String, val code: FailureCode, val message: String, val recoverable: Boolean = true) : CapabilityResult
    enum class RejectionCode { UNKNOWN_CAPABILITY, POLICY_DENIED, CONFIRMATION_REQUIRED, MISSING_PERMISSION, INVALID_ARGUMENTS, UNSUPPORTED }
    enum class FailureCode { EXECUTION_FAILED, ACTIVITY_NOT_FOUND, BACKEND_ERROR }
}
