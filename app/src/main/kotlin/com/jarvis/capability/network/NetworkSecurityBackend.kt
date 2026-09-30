package com.jarvis.capability.network

import com.jarvis.core.capability.*
import com.jarvis.friday.security.SecurityRisk
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.network.*
import com.jarvis.security.web.WebSecurityAnalyzer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSecurityBackend @Inject constructor(
    private val scopes: SecurityScopeRegistry,
    private val engine: NetworkEngine,
    private val tls: TlsInspector,
    private val inventory: NetworkInventory,
    private val tcp: TcpProbe,
    private val fingerprint: ServiceFingerprinter,
    private val operator: OperatorAuthorization,
    private val web: WebSecurityAnalyzer
) : CapabilityBackend {
    override val backendId = "friday.network"
    override val capabilities = setOf(
        Capability("network.dns_resolve", "Resolve DNS within an authorized security scope"),
        Capability("network.tcp_probe", "Check TCP connectivity within an authorized security scope", Capability.RiskLevel.MEDIUM),
        Capability("network.port_scan", "Enumerate bounded TCP ports within an authorized security scope", Capability.RiskLevel.HIGH, timeoutMs = 45_000L),
        Capability("network.service_detect", "Identify services from authorized port observations", Capability.RiskLevel.MEDIUM),
        Capability("network.tls_inspect", "Inspect TLS metadata within an authorized security scope", Capability.RiskLevel.MEDIUM),
        Capability("network.inventory", "Read the authorized network asset inventory"),
        Capability("network.target_profile", "Build a structured profile from authorized observations", Capability.RiskLevel.MEDIUM),
        Capability("websec.analyze", "Analyze safe web security headers and cookies within an authorized scope", Capability.RiskLevel.HIGH, timeoutMs = 30_000L)
    )

    override suspend fun execute(r: CapabilityRequest): CapabilityResult {
        val scopeId = r.arguments["scope_id"].orEmpty()
        val target = r.arguments["target"].orEmpty()
        if (scopeId.isBlank() || target.isBlank()) return invalid(r)
        return when (r.capabilityId) {
            "network.dns_resolve" -> {
                checkScope(scopeId, target, "dns_resolve", SecurityRisk.R0_INFORMATION)?.let { return it }
                val addresses = engine.resolve(scopeId, target)
                CapabilityResult.Success(r.requestId, "DNS resolution completed.", mapOf("addresses" to addresses.joinToString(",")))
            }
            "network.tcp_probe" -> {
                val port = r.arguments["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return invalid(r)
                checkScope(scopeId, target, "tcp_probe", SecurityRisk.R2_ENUMERATION)?.let { return it }
                if (!operator.consume()) return CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.POLICY_DENIED, "Fresh operator authorization is required for active network probing.")
                val open = tcp.probe(target, port)
                CapabilityResult.Success(r.requestId, "TCP probe completed.", mapOf("host" to target, "port" to port.toString(), "open" to open.toString()))
            }
            "network.port_scan" -> {
                val ports = r.arguments["ports"].orEmpty().split(",").mapNotNull { it.trim().toIntOrNull() }
                if (ports.isEmpty()) return invalid(r)
                checkScope(scopeId, target, "port_scan", SecurityRisk.R2_ENUMERATION)?.let { return it }
                val result = engine.scan(scopeId, target, ports)
                CapabilityResult.Success(r.requestId, "Port scan completed.", mapOf(
                    "host" to target,
                    "scanned" to result.scannedPorts.joinToString(","),
                    "open" to result.openPorts.joinToString(",")
                ))
            }
            "network.service_detect" -> {
                val port = r.arguments["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return invalid(r)
                checkScope(scopeId, target, "service_detect", SecurityRisk.R2_ENUMERATION)?.let { return it }
                val service = fingerprint.identify(port)
                CapabilityResult.Success(r.requestId, "Service identification completed.", mapOf("port" to port.toString(), "service" to service))
            }
            "network.tls_inspect" -> {
                val port = r.arguments["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 443
                checkScope(scopeId, target, "tls_inspect", SecurityRisk.R1_PASSIVE)?.let { return it }
                val result = tls.inspect(target, port)
                CapabilityResult.Success(r.requestId, "TLS inspection completed.", mapOf(
                    "protocol" to result.protocol,
                    "cipher" to result.cipherSuite,
                    "subject" to (result.subject ?: ""),
                    "issuer" to (result.issuer ?: "")
                ))
            }
            "network.inventory" -> {
                if (!scopes.activeScopes().any { it.id == scopeId }) return denied(r)
                val rows = inventory.all(scopeId)
                CapabilityResult.Success(r.requestId, "Network inventory retrieved.", mapOf(
                    "count" to rows.size.toString(),
                    "assets" to rows.joinToString("\n") { it.address + "|" + it.openServices.joinToString(",") { s -> s.port.toString() + "/" + s.service } }
                ))
            }
            "websec.analyze" -> {
                if (!scopes.isAuthorized(scopeId, target, "websec_analyze", SecurityRisk.R3_VALIDATION)) return denied(r)
                val assessment = web.analyze(scopeId, target)
                CapabilityResult.Success(r.requestId, "Web security analysis completed.", mapOf(
                    "status" to assessment.statusCode.toString(),
                    "findings" to assessment.findings.joinToString("\n") { it.id + "|" + it.severity + "|" + it.title }
                ))
            }
            "network.target_profile" -> {
                if (!scopes.isAuthorized(scopeId, target, "target_profile", SecurityRisk.R1_PASSIVE)) return denied(r)
                val asset = inventory.all(scopeId).firstOrNull { it.address == target || it.hostname == target }
                CapabilityResult.Success(r.requestId, "Target profile generated.", mapOf(
                    "target" to target,
                    "services" to (asset?.openServices?.joinToString(",") { it.port.toString() + "/" + (it.service ?: "unknown") } ?: ""),
                    "confidence" to if (asset == null) "0.2" else "0.8"
                ))
            }
            else -> CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unknown network capability.")
        }
    }

    private fun checkScope(scopeId: String, target: String, capability: String, risk: SecurityRisk): CapabilityResult.Rejected? =
        if (scopes.isAuthorized(scopeId, target, capability, risk)) null else denied(
            CapabilityRequest(capabilityId = capability, arguments = mapOf("scope_id" to scopeId, "target" to target), requestId = UUID.randomUUID().toString(), source = CapabilityRequest.Source.USER)
        )

    private fun invalid(r: CapabilityRequest) = CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.INVALID_ARGUMENTS, "Required network arguments are missing or invalid.")
    private fun denied(r: CapabilityRequest) = CapabilityResult.Rejected(r.requestId, CapabilityResult.RejectionCode.POLICY_DENIED, "Target is outside the active authorized security scope.")
}
