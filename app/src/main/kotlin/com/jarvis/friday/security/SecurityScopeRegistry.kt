package com.jarvis.friday.security

import java.net.Inet6Address
import java.net.InetAddress
import java.net.IDN
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Short-lived, exact-target authorization grants for security assessment.
 * Grants are memory-only and fail closed. Wildcards, CIDRs, URLs and paths
 * are intentionally unsupported.
 */
@Singleton
class SecurityScopeRegistry @Inject constructor() {
    private data class Entry(
        val targets: Set<String>,
        val actions: Set<String>,
        val maxRisk: SecurityRisk,
        val expiresAtEpochMillis: Long
    )

    private val scopes = ConcurrentHashMap<String, Entry>()

    /**
     * Register a grant only after the app's confirmation flow has confirmed
     * the exact target and operation. Grants last at most 24 hours.
     */
    fun grant(
        scopeId: String,
        targets: Set<String>,
        allowedActions: Set<String>,
        maxRisk: SecurityRisk,
        expiresAtEpochMillis: Long,
        explicitlyConfirmed: Boolean
    ): Boolean {
        val id = scopeId.trim()
        if (!explicitlyConfirmed || !SCOPE_ID.matches(id)) return false
        if (targets.isEmpty() || targets.size > MAX_TARGETS_PER_SCOPE) return false

        val now = System.currentTimeMillis()
        if (expiresAtEpochMillis <= now ||
            expiresAtEpochMillis - now > MAX_SCOPE_DURATION_MS
        ) return false

        val canonicalTargets = linkedSetOf<String>()
        for (target in targets) {
            canonicalTargets.add(canonicalizeTarget(target) ?: return false)
        }

        val canonicalActions = allowedActions.map { it.trim().lowercase(Locale.ROOT) }.toSet()
        if (canonicalActions.isEmpty() || !SUPPORTED_ACTIONS.containsAll(canonicalActions)) return false

        scopes[id] = Entry(canonicalTargets, canonicalActions, maxRisk, expiresAtEpochMillis)
        return true
    }

    fun revoke(scopeId: String): Boolean = scopes.remove(scopeId.trim()) != null

    fun isAuthorized(
        scopeId: String,
        target: String,
        action: String,
        risk: SecurityRisk
    ): Boolean {
        val id = scopeId.trim()
        val entry = scopes[id] ?: return false
        if (entry.expiresAtEpochMillis <= System.currentTimeMillis()) {
            scopes.remove(id, entry)
            return false
        }

        val canonicalTarget = canonicalizeTarget(target) ?: return false
        val canonicalAction = action.trim().lowercase(Locale.ROOT)
        return canonicalTarget in entry.targets &&
            canonicalAction in entry.actions &&
            risk.level <= entry.maxRisk.level
    }

    /** Canonicalize a literal host/IP without performing DNS resolution. */
    fun canonicalizeTarget(target: String): String? {
        var value = target.trim()
        if (value.isEmpty() || value.length > MAX_INPUT_LENGTH) return null
        if (value.any(Char::isWhitespace) || value.any { it in "/\\@?#*" } ||
            value.contains("://") || value.contains('%')
        ) return null

        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length - 1)
        }

        if (value.contains(':')) {
            return try {
                (InetAddress.getByName(value) as? Inet6Address)
                    ?.hostAddress?.substringBefore('%')?.lowercase(Locale.ROOT)
            } catch (_: Exception) { null }
        }

        if (value.all { it.isDigit() || it == '.' }) {
            val octets = value.split('.')
            if (octets.size != 4) return null
            val numbers = octets.map { it.toIntOrNull() ?: return null }
            if (numbers.any { it !in 0..255 }) return null
            return numbers.joinToString(".")
        }

        value = value.trimEnd('.')
        if (value.isEmpty()) return null
        return try {
            val ascii = IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
            if (ascii.length > 253) return null
            val labels = ascii.split('.')
            if (labels.any { label ->
                    label.isEmpty() || label.length > 63 ||
                        label.startsWith('-') || label.endsWith('-')
                }
            ) return null
            ascii
        } catch (_: IllegalArgumentException) { null }
    }

    companion object {
        const val ACTION_PORT_SCAN = "port_scan"
        const val ACTION_FINDING_VERIFY = "finding_verify"
        const val MAX_TARGETS_PER_SCOPE = 16
        const val MAX_SCOPE_DURATION_MS = 24L * 60L * 60L * 1000L
        private const val MAX_INPUT_LENGTH = 512
        private val SCOPE_ID = Regex("[A-Za-z0-9_-]{1,64}")
        private val SUPPORTED_ACTIONS = setOf(ACTION_PORT_SCAN, ACTION_FINDING_VERIFY)
    }
}
