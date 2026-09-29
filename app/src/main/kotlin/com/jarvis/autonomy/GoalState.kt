package com.jarvis.autonomy

import kotlinx.serialization.Serializable

enum class GoalStatus { PENDING, RUNNING, BLOCKED, DONE, FAILED, CANCELLED, PAUSED }
enum class StepStatus { PENDING, RUNNING, DONE, FAILED, SKIPPED }

@Serializable
data class PlanStep(
    val id: String,
    val description: String,
    val tool: String? = null,
    val args: Map<String, String> = emptyMap(),
    var status: StepStatus = StepStatus.PENDING,
    var result: String? = null,
    var attempts: Int = 0,
    var startedAt: Long? = null,
    var finishedAt: Long? = null
)

@Serializable
data class Trigger(val type: String, val spec: String)

@Serializable
data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val language: String = "hi",
    val priority: Int = 3,
    val deadline: Long? = null,
    var status: GoalStatus = GoalStatus.PENDING,
    var plan: List<PlanStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var evidence: List<String> = emptyList(),
    val triggers: List<Trigger> = emptyList(),
    val checkIntervalSeconds: Long? = null,
    val maxRetries: Int = 3,
    val maxRuntimeSeconds: Long = 86_400,
    val confirmationRequired: Boolean = true
)
