package com.jarvis.command

import com.jarvis.tools.ToolArgsCodec
import java.security.MessageDigest
import java.util.UUID

data class ActionPlan(
    val requestId: String,
    val originalCommand: String,
    val language: String,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val actions: List<PlannedAction>
) {
    val hash: String by lazy(LazyThreadSafetyMode.PUBLICATION) { canonicalHash() }
    data class PlannedAction(val actionId: String, val toolName: String, val args: Map<String, Any?>, val expectedEffect: String)
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean = nowMs >= expiresAtMs
    private fun canonicalHash(): String {
        val canonical = buildString {
            append(requestId).append('|').append(originalCommand).append('|').append(language).append('|')
            append(createdAtMs).append('|').append(expiresAtMs).append('|')
            actions.forEach {
                append(it.actionId).append('|').append(it.toolName).append('|')
                append(ToolArgsCodec.canonical(it.args)).append('|').append(it.expectedEffect).append(';')
            }
        }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
    companion object {
        private const val TTL_MS = 60_000L
        fun create(originalCommand: String, language: String, actions: List<PlannedAction>, nowMs: Long = System.currentTimeMillis()) =
            ActionPlan(UUID.randomUUID().toString(), originalCommand, language, nowMs, nowMs + TTL_MS, actions.toList())
    }
}
