package com.jarvis.tools.impl

import com.jarvis.core.capability.CapabilityExecutor
import com.jarvis.core.capability.CapabilityRequest
import com.jarvis.core.capability.CapabilityResult
import com.jarvis.tools.Confirmation
import com.jarvis.tools.ParamSpec
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import com.jarvis.tools.ToolSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityCapabilityTool @Inject constructor(
    private val executor: CapabilityExecutor
) : Tool {
    private val allowed = setOf(
        "security.scope.enroll",
        "security.assess.app",
        "security.simulate.exploit",
        "security.monitor.enable",
        "security.monitor.disable",
        "security.monitor.status",
        "security.monitor.events",
        "network.dns_resolve",
        "network.tcp_probe",
        "network.port_scan",
        "network.service_detect",
        "network.tls_inspect",
        "network.inventory",
        "network.target_profile"
    )

    override val spec = ToolSpec(
        name = "security_capability",
        descriptionHi = "अनुमोदित security capability चलाएँ",
        descriptionEn = "Run a scoped security capability through the authorization and capability control plane",
        parameters = mapOf(
            "capability" to ParamSpec("string", true, "Allowed capability identifier"),
            "arguments_json" to ParamSpec("string", false, "JSON object containing capability arguments", "{}")
        ),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 30,
        timeoutMs = 60_000
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val capability = (args["capability"] as? String)?.trim().orEmpty()
        if (capability !in allowed) return ToolResult.Err("Security capability is not exposed.", "capability_denied")
        val raw = (args["arguments_json"] as? String).orEmpty().ifBlank { "{}" }
        val json = runCatching { Json.parseToJsonElement(raw) as? JsonObject }.getOrNull()
            ?: return ToolResult.Err("arguments_json must be a JSON object.", "bad_args")
        val arguments = json.mapValues { (_, value) -> value.jsonPrimitive.content }
        val request = CapabilityRequest(
            capabilityId = capability,
            arguments = arguments,
            requestId = java.util.UUID.randomUUID().toString(),
            source = CapabilityRequest.Source.ASSISTANT,
            confirmationGranted = true
        )
        return when (val result = executor.execute(request)) {
            is CapabilityResult.Success -> ToolResult.Ok(result.data)
            is CapabilityResult.Rejected -> ToolResult.Err(result.message, result.code.name.lowercase())
            is CapabilityResult.Failed -> ToolResult.Err(result.message, result.code.name.lowercase())
        }
    }
}
