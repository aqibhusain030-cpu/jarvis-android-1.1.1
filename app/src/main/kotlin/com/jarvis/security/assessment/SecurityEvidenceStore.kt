package com.jarvis.security.assessment

import java.time.Instant
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class SecurityEvidence(
    val evidenceId: String,
    val assessmentId: String,
    val target: String,
    val timestamp: Instant = Instant.now(),
    val capability: String,
    val observation: String,
    val confidence: Float,
    val integrityHash: String
)

@Singleton
class SecurityEvidenceStore @Inject constructor() {
    private val evidence = ConcurrentHashMap<String, SecurityEvidence>()

    fun record(
        assessmentId: String,
        target: String,
        capability: String,
        observation: String,
        confidence: Float
    ): SecurityEvidence {
        val id = java.util.UUID.randomUUID().toString()
        val canonical = listOf(id, assessmentId, target, capability, observation, confidence.toString()).joinToString("|")
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return SecurityEvidence(id, assessmentId, target, capability = capability, observation = observation, confidence = confidence.coerceIn(0f, 1f), integrityHash = hash)
            .also { evidence[id] = it }
    }

    fun get(id: String): SecurityEvidence? = evidence[id]
    fun forAssessment(assessmentId: String): List<SecurityEvidence> =
        evidence.values.filter { it.assessmentId == assessmentId }.sortedBy { it.timestamp }
}
