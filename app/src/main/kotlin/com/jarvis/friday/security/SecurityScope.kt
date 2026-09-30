package com.jarvis.friday.security

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

enum class SecurityRisk {
    R0_INFORMATION,
    R1_PASSIVE,
    R2_ENUMERATION,
    R3_VALIDATION,
    R4_CONSEQUENTIAL,
    R5_MODIFICATION,
    R6_ACTUATION
}

enum class SecurityEnvironment {
    AUTHORIZED_LAB,
    STAGING,
    PRODUCTION_REVIEW
}

object SecurityScopeDefaults {
    val SAFE_CAPABILITIES: Set<String> = setOf(
        "dns_resolve",
        "tcp_probe",
        "udp_probe",
        "network_discover",
        "port_scan",
        "service_detect",
        "tls_inspect",
        "http_inspect",
        "route_inspect",
        "network_inventory",
        "target_profile",
        "vulnerability_assess",
        "finding_verify",
        "network_monitor",
        "websec_analyze",
        "mobile_security_audit"
    )
}

/**
 * A local, cryptographically integrity-protected authorization scope.
 *
 * Enrollment is still an explicit operator action. The proof protects the
 * scope record from silent in-process mutation; it is not a substitute for
 * legal ownership/authorization of a remote asset.
 */
data class SecurityScope(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val target: String,
    val authorizedByUser: Boolean,
    val expiresAt: Instant,
    val allowedCapabilities: Set<String> = SecurityScopeDefaults.SAFE_CAPABILITIES,
    val maxRisk: SecurityRisk = SecurityRisk.R3_VALIDATION,
    val environment: SecurityEnvironment = SecurityEnvironment.AUTHORIZED_LAB,
    val authorizationProof: String = ""
) {
    fun active(now: Instant = Instant.now()): Boolean =
        authorizedByUser && now.isBefore(expiresAt)

    fun allows(capabilityId: String, risk: SecurityRisk, now: Instant = Instant.now()): Boolean =
        active(now) &&
            capabilityId in allowedCapabilities &&
            risk.ordinal <= maxRisk.ordinal

    fun canonicalPayload(): String = listOf(
        id,
        label,
        target,
        expiresAt.toString(),
        allowedCapabilities.toList().sorted().joinToString(","),
        maxRisk.name,
        environment.name
    ).joinToString("|")
}

@javax.inject.Singleton
class SecurityScopeProofSigner @javax.inject.Inject constructor() {
    private val keyBytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
    private val key = SecretKeySpec(keyBytes, "HmacSHA256")

    fun sign(scope: SecurityScope): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
            mac.doFinal(scope.canonicalPayload().toByteArray(StandardCharsets.UTF_8))
        )
    }

    fun verify(scope: SecurityScope): Boolean {
        if (scope.authorizationProof.isBlank()) return false
        return java.security.MessageDigest.isEqual(
            sign(scope).toByteArray(StandardCharsets.UTF_8),
            scope.authorizationProof.toByteArray(StandardCharsets.UTF_8)
        )
    }
}

class SecurityScopeRegistry @javax.inject.Inject constructor(
    private val signer: SecurityScopeProofSigner
) {
    private val scopes = LinkedHashMap<String, SecurityScope>()

    @Synchronized
    fun enroll(scope: SecurityScope): Boolean {
        if (!scope.authorizedByUser || scope.label.isBlank() || scope.target.isBlank()) return false
        if (!scope.active()) return false
        val signed = scope.copy(authorizationProof = signer.sign(scope))
        scopes[signed.id] = signed
        return true
    }

    @Synchronized
    fun isAuthorized(
        scopeId: String,
        target: String,
        capabilityId: String,
        risk: SecurityRisk,
        now: Instant = Instant.now()
    ): Boolean {
        val scope = scopes[scopeId] ?: return false
        if (!signer.verify(scope)) return false
        return scope.allows(capabilityId, risk, now) && TargetMatcher.matches(scope.target, target)
    }

    @Synchronized
    fun activeScopes(now: Instant = Instant.now()): List<SecurityScope> =
        scopes.values.filter { signer.verify(it) && it.active(now) }.toList()

    @Synchronized
    fun get(scopeId: String): SecurityScope? =
        scopes[scopeId]?.takeIf(signer::verify)

    @Synchronized
    fun revoke(scopeId: String) {
        scopes.remove(scopeId)
    }
}

object TargetMatcher {
    fun matches(scopeTarget: String, requestedTarget: String): Boolean {
        val scope = normalize(scopeTarget) ?: return false
        val requested = normalize(requestedTarget) ?: return false
        if (scope == requested) return true
        if (scope.startsWith("*.")) {
            val suffix = scope.removePrefix("*.")
            return requested == suffix || requested.endsWith(".$suffix")
        }
        if (scope.contains("/") && isIpv4Cidr(scope)) {
            val host = requested.substringBefore('/').removePrefix("https://").removePrefix("http://")
            return ipv4InCidr(host.substringBefore('/'), scope)
        }
        return false
    }

    private fun normalize(value: String): String? {
        val raw = value.trim().removeSuffix("/")
        if (raw.isBlank()) return null
        return runCatching {
            val uri = if (raw.contains("://")) java.net.URI(raw) else null
            val host = uri?.host ?: raw.substringBefore('/').substringBefore(':')
            host.trim().removePrefix("[").removeSuffix("]").lowercase()
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun isIpv4Cidr(value: String): Boolean =
        value.count { it == '/' } == 1 &&
            value.substringAfter('/').toIntOrNull()?.let { it in 0..32 } == true

    private fun ipv4InCidr(ip: String, cidr: String): Boolean {
        val parts = ip.split('.').map { it.toLongOrNull() ?: return false }
        val maskBits = cidr.substringAfter('/').toIntOrNull() ?: return false
        if (parts.size != 4 || parts.any { it !in 0..255 }) return false
        val ipValue = parts.fold(0L) { acc, part -> (acc shl 8) or part }
        val networkParts = cidr.substringBefore('/').split('.').map { it.toLongOrNull() ?: return false }
        if (networkParts.size != 4 || networkParts.any { it !in 0..255 }) return false
        val networkValue = networkParts.fold(0L) { acc, part -> (acc shl 8) or part }
        val mask = if (maskBits == 0) 0L else (0xFFFF_FFFFL shl (32 - maskBits)) and 0xFFFF_FFFFL
        return (ipValue and mask) == (networkValue and mask)
    }
}
