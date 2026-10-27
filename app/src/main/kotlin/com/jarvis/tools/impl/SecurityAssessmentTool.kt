package com.jarvis.tools.impl

import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.network.NetworkEngine
import com.jarvis.security.assessment.FindingVerifier
import com.jarvis.tools.Confirmation
import com.jarvis.tools.ParamSpec
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import com.jarvis.tools.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-confirmed assessment for one exact target.
 * A one-target, five-minute scope is created for this call and always revoked.
 * CIDRs, wildcards and unbounded port lists are not accepted.
 */
@Singleton
class SecurityAssessmentTool @Inject constructor(
    private val scopes: SecurityScopeRegistry,
    private val network: NetworkEngine,
    private val verifier: FindingVerifier
) : Tool {
    override val spec = ToolSpec(
        name = "security_assessment",
        descriptionHi = "आपके अधिकृत होस्ट पर सीमित TCP पोर्ट जाँच चलाएँ",
        descriptionEn = "With your confirmation, check up to 32 TCP ports on one host you own or are authorized to assess.",
        parameters = mapOf(
            "target" to ParamSpec("string", true, "Exact hostname or IP; no URL, CIDR, or wildcard"),
            "mode" to ParamSpec("string", false, "scan or verify_closed; defaults to scan"),
            "ports" to ParamSpec("list", false, "TCP port numbers for mode=scan"),
            "port" to ParamSpec("int", false, "Single TCP port for mode=verify_closed"),
            "finding_id" to ParamSpec("string", false, "Finding identifier for mode=verify_closed")
        ),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 10,
        timeoutMs = 20_000
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult = withContext(Dispatchers.IO) {
        val target = args["target"] as? String
            ?: return@withContext ToolResult.Err("target is required", "bad_arg")
        val mode = (args["mode"] as? String ?: "scan").trim().lowercase(Locale.ROOT)
        val action = when (mode) {
            "scan" -> SecurityScopeRegistry.ACTION_PORT_SCAN
            "verify_closed" -> SecurityScopeRegistry.ACTION_FINDING_VERIFY
            else -> return@withContext ToolResult.Err("mode must be scan or verify_closed", "bad_arg")
        }

        val ports = when (mode) {
            "scan" -> {
                val raw = args["ports"] as? List<*>
                    ?: return@withContext ToolResult.Err("ports must be a list of integers", "bad_arg")
                if (raw.isEmpty() || raw.size > NetworkEngine.MAX_PORTS_PER_SCAN) {
                    return@withContext ToolResult.Err("Provide between 1 and 32 ports.", "bad_arg")
                }
                val parsed = mutableListOf<Int>()
                for (value in raw) {
                    val number = value as? Number
                        ?: return@withContext ToolResult.Err("Every port must be an integer", "bad_arg")
                    val port = number.toInt()
                    if (number.toDouble() != port.toDouble() || port !in 1..65535) {
                        return@withContext ToolResult.Err("Ports must be integers from 1 to 65535", "bad_arg")
                    }
                    parsed.add(port)
                }
                parsed.distinct()
            }
            else -> {
                val number = args["port"] as? Number
                    ?: return@withContext ToolResult.Err("port is required for verify_closed", "bad_arg")
                val port = number.toInt()
                if (number.toDouble() != port.toDouble() || port !in 1..65535) {
                    return@withContext ToolResult.Err("port must be an integer from 1 to 65535", "bad_arg")
                }
                if ((args["finding_id"] as? String).isNullOrBlank()) {
                    return@withContext ToolResult.Err("finding_id is required for verify_closed", "bad_arg")
                }
                listOf(port)
            }
        }

        val scopeId = "assessment_" + UUID.randomUUID().toString().replace("-", "")
        val granted = scopes.grant(
            scopeId = scopeId,
            targets = setOf(target),
            allowedActions = setOf(action),
            maxRisk = SecurityRisk.R3_VALIDATION,
            expiresAtEpochMillis = System.currentTimeMillis() + ONE_SHOT_SCOPE_MS,
            explicitlyConfirmed = true
        )
        if (!granted) {
            return@withContext ToolResult.Err("Could not establish an exact-target authorization scope.", "scope_denied")
        }

        try {
            if (mode == "scan") {
                val result = network.scan(
                    scopeId = scopeId,
                    target = target,
                    ports = ports,
                    action = action,
                    risk = SecurityRisk.R3_VALIDATION
                )
                if (!result.authorized || result.error != null) {
                    ToolResult.Err(result.error ?: "Security assessment was not authorized", "scan_denied")
                } else {
                    ToolResult.Ok(
                        mapOf(
                            "target" to result.target,
                            "requestedPorts" to result.requestedPorts,
                            "openPorts" to result.openPorts.sorted(),
                            "closedPorts" to result.closedPorts.sorted(),
                            "filteredPorts" to result.filteredPorts.sorted(),
                            "errorPorts" to result.errorPorts.sorted(),
                            "complete" to result.isComplete,
                            "durationMs" to result.durationMs,
                            "note" to "A timeout or scan error is not evidence that a port is closed."
                        )
                    )
                }
            } else {
                val verification = verifier.verifyPortClosed(
                    scopeId = scopeId,
                    target = target,
                    port = ports.single(),
                    findingId = args["finding_id"] as String
                )
                ToolResult.Ok(
                    mapOf(
                        "findingId" to verification.findingId,
                        "status" to verification.status.name,
                        "evidence" to verification.evidence
                    )
                )
            }
        } finally {
            scopes.revoke(scopeId)
        }
    }

    companion object {
        private const val ONE_SHOT_SCOPE_MS = 5L * 60L * 1000L
    }
}
