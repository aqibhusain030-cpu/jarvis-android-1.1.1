package com.jarvis.core.capability

import com.jarvis.friday.core.FridayOperationCatalog
import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolRegistry

/**
 * Adapts the mature JARVIS ToolDispatcher into the canonical FRIDAY capability plane.
 * The dispatcher remains authoritative for Android permissions, network policy,
 * confirmation, rate limits, kill switch, timeout and audit.
 */
class ToolCapabilityBackend(
    private val registry: ToolRegistry,
    private val dispatcher: ToolDispatcher
) : CapabilityBackend {

    override val backendId: String = "jarvis.tools"
    override val capabilities: Set<Capability> = FridayOperationCatalog.operations
        .filter { it.supported && it.existingTool != null && registry.get(it.existingTool) != null }
        .map { Capability(it.id, it.description, mapRisk(it.risk)) }
        .toSet()

    override suspend fun execute(request: CapabilityRequest): CapabilityResult {
        val operation = FridayOperationCatalog.operations.firstOrNull { it.id == request.capabilityId }
            ?: return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY,
                "Unknown FRIDAY operation."
            )
        val toolName = operation.existingTool
            ?: return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.UNSUPPORTED,
                "This FRIDAY operation has no Android implementation."
            )
        val tool = registry.get(toolName)
            ?: return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.UNSUPPORTED,
                "Android implementation is unavailable."
            )

        val args = coerceArguments(tool.spec.parameters, request.arguments)
            ?: return CapabilityResult.Rejected(
                request.requestId,
                CapabilityResult.RejectionCode.INVALID_ARGUMENTS,
                "One or more FRIDAY arguments have an invalid type."
            )

        return when (val result = dispatcher.invoke(toolName, args)) {
            is com.jarvis.tools.ToolResult.Ok ->
                CapabilityResult.Success(request.requestId, "Operation completed.", mapOf("result" to (result.data?.toString() ?: "")))
            is com.jarvis.tools.ToolResult.Err ->
                CapabilityResult.Failed(request.requestId, CapabilityResult.FailureCode.EXECUTION_FAILED, result.message, result.code != "permission_missing")
        }
    }

    private fun coerceArguments(
        specs: Map<String, com.jarvis.tools.ParamSpec>,
        raw: Map<String, String>
    ): Map<String, Any?>? {
        if (raw.keys.any { it !in specs.keys }) return null
        val converted = linkedMapOf<String, Any?>()
        for ((name, value) in raw) {
            val convertedValue: Any? = when (specs[name]?.type?.lowercase()) {
                "string", null -> value
                "int", "integer" -> value.toIntOrNull() ?: return null
                "long" -> value.toLongOrNull() ?: return null
                "float" -> value.toFloatOrNull() ?: return null
                "double", "number" -> value.toDoubleOrNull() ?: return null
                "bool", "boolean" -> value.toBooleanStrictOrNull() ?: return null
                else -> value
            }
            converted[name] = convertedValue
        }
        return converted
    }

    private fun mapRisk(risk: com.jarvis.friday.core.FridayRisk): Capability.RiskLevel = when (risk) {
        com.jarvis.friday.core.FridayRisk.LOW -> Capability.RiskLevel.LOW
        com.jarvis.friday.core.FridayRisk.MEDIUM -> Capability.RiskLevel.MEDIUM
        com.jarvis.friday.core.FridayRisk.HIGH -> Capability.RiskLevel.HIGH
        com.jarvis.friday.core.FridayRisk.CRITICAL -> Capability.RiskLevel.CRITICAL
    }
}
