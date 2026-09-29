package com.jarvis.tools

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.security.AuditLogger
import com.jarvis.security.KillSwitch
import com.jarvis.security.SecurityPolicy
import com.jarvis.security.SecurityUnlockManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for every tool invocation.
 *
 * Authorization order is fixed:
 * kill switch -> existence -> parameter schema -> policy allow -> permissions ->
 * network policy -> rate limit -> serialized unlock/confirmation -> timeout -> audit.
 */
@Singleton
class ToolDispatcher @Inject constructor(
    private val registry: ToolRegistry,
    private val policy: SecurityPolicy,
    private val confirmation: ConfirmationManager,
    private val unlock: SecurityUnlockManager,
    private val rateLimiter: RateLimiter,
    private val audit: AuditLogger,
    private val config: ConfigRepository,
    private val killSwitch: KillSwitch,
    private val logger: JarvisLogger,
    private val permissionGate: PermissionGate
) {
    private val authorizationMutex = Mutex()

    suspend fun invoke(name: String, args: Map<String, Any?>): ToolResult {
        if (killSwitch.isKilled.value && name !in SAFE_WHILE_KILLED) {
            return ToolResult.Err("Kill switch active", "killed")
        }

        val tool = registry.get(name)
            ?: return ToolResult.Err("Unknown tool: $name", "unknown_tool")

        val validation = validateArgs(tool.spec, args)
        if (validation != null) {
            audit.log(name, args, validation.toJson(), false)
            return validation
        }

        if (!policy.toolAllowed(name)) {
            return ToolResult.Err("Tool disabled by policy: $name", "disabled")
        }

        val missingPermissions = permissionGate.missing(tool.spec)
        if (missingPermissions.isNotEmpty()) {
            return ToolResult.Err(
                "Missing permission(s): ${missingPermissions.joinToString(", ")}",
                "permission_missing"
            )
        }

        if (name in NETWORK_TOOLS && !policy.networkAllowed(name, args)) {
            return ToolResult.Err("Network blocked by policy for $name", "network_denied")
        }

        if (!rateLimiter.allow(name, tool.spec.rateLimitPerHour)) {
            return ToolResult.Err("Rate limit exceeded for $name", "rate_limited")
        }

        val needsConfirm = policy.requiresConfirmation(name) ||
            tool.spec.confirmation != Confirmation.NONE
        val needsPin = policy.requiresPin(name)

        if (needsPin || needsConfirm) {
            val authError = authorizationMutex.withLock {
                if (needsPin && !unlock.consume()) {
                    audit.log(name, args, "authentication_required", false)
                    ToolResult.Err(
                        "High-risk action requires biometric/device-credential unlock in Settings.",
                        "authentication_required"
                    )
                } else if (needsConfirm && !confirmation.ask(confirmPrompt(name, args))) {
                    audit.log(name, args, "cancelled", false)
                    ToolResult.Err("User cancelled", "cancelled")
                } else {
                    null
                }
            }
            if (authError != null) return authError
        }

        val result = try {
            withTimeout(tool.spec.timeoutMs) { tool.invoke(args) }
        } catch (t: TimeoutCancellationException) {
            logger.w(TAG, "tool $name timed out")
            ToolResult.Err("Tool timed out", "timeout")
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Exception) {
            logger.e(TAG, "tool $name failed", t)
            ToolResult.Err(t.message ?: "error", "exception")
        }

        if (tool.spec.audit) audit.log(name, args, result.toJson(), true)
        return result
    }

    private fun validateArgs(spec: ToolSpec, args: Map<String, Any?>): ToolResult.Err? {
        val unknown = args.keys - spec.parameters.keys
        if (unknown.isNotEmpty()) {
            return ToolResult.Err(
                "Unknown parameter(s): ${unknown.sorted().joinToString(", ")}",
                "bad_args"
            )
        }

        for ((name, param) in spec.parameters) {
            if (param.required && !args.containsKey(name)) {
                return ToolResult.Err("Missing required parameter: $name", "bad_args")
            }
            if (!args.containsKey(name)) continue
            val value = args[name]
            if (value == null) {
                if (param.required) return ToolResult.Err("$name cannot be null", "bad_args")
                continue
            }
            if (!matchesType(param.type, value)) {
                return ToolResult.Err(
                    "Invalid type for $name: expected ${param.type}",
                    "bad_args"
                )
            }
        }
        return null
    }

    private fun matchesType(type: String, value: Any): Boolean = when (type.lowercase()) {
        "string" -> value is String
        "int", "integer" -> value is Byte || value is Short || value is Int || value is Long
        "float", "double", "number" -> value is Number
        "bool", "boolean" -> value is Boolean
        "list", "array" -> value is List<*>
        "object", "map" -> value is Map<*, *>
        "any" -> true
        else -> false
    }

    private fun confirmPrompt(name: String, args: Map<String, Any?>): String {
        val detail = args.entries.take(3).joinToString(", ") { "${it.key}=${it.value}" }
        return if (detail.isNotBlank())
            "सर, क्या मैं $name ($detail) करूँ? हाँ कहें। / Sir, run $name ($detail)? Say yes."
        else
            "सर, क्या मैं $name करूँ? हाँ कहें। / Sir, should I run $name? Say yes to confirm."
    }

    companion object {
        private const val TAG = "ToolDispatcher"
        private val SAFE_WHILE_KILLED = setOf("speak", "healthcheck", "audit_log_read")
        private val NETWORK_TOOLS = setOf("http_get", "http_post", "get_weather", "open_url")
    }
}
