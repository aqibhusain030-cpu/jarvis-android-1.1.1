package com.jarvis.friday.core

import java.time.Instant
import java.util.UUID

enum class FridayRisk { LOW, MEDIUM, HIGH, CRITICAL }
enum class FridayMode { NORMAL, AUTONOMOUS, DEGRADED, ISOLATED, EMERGENCY }
enum class IdentityRole { PRIMARY, SECONDARY, OBSERVER }

data class FridayIdentity(
    val name: String = "F.R.I.D.A.Y.",
    val expansion: String = "Female Replacement Intelligent Digital Assistant Youth",
    val designation: String = "TACTICAL_ASSISTANT",
    val version: Int = 1,
    val role: IdentityRole = IdentityRole.PRIMARY
)

data class PriorityGoal(
    val id: String = UUID.randomUUID().toString(),
    val objective: String,
    val priority: Int,
    val risk: FridayRisk = FridayRisk.LOW,
    val deadlineEpochMs: Long? = null
)

data class TacticalActor(
    val id: String,
    val role: ActorRole,
    val confidence: Double,
    val location: Vector3? = null
) {
    enum class ActorRole { USER, ALLY, CIVILIAN, UNKNOWN, HOSTILE }
}

data class Vector3(val x: Double, val y: Double, val z: Double)

data class TacticalWorld(
    val timestamp: Instant = Instant.now(),
    val actors: List<TacticalActor> = emptyList(),
    val hazards: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val sourceCount: Int = 0
)

data class VitalSnapshot(
    val timestamp: Instant = Instant.now(),
    val heartRateBpm: Int? = null,
    val batteryPercent: Int? = null,
    val armorIntegrityPercent: Int? = null,
    val powerPercent: Int? = null,
    val confidence: Double = 0.0
)

data class FridayCommand(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val risk: FridayRisk = FridayRisk.LOW,
    val confirmationRequired: Boolean = false,
    val source: String = "voice"
)

sealed interface FridayDecision {
    data class Execute(val command: FridayCommand) : FridayDecision
    data class Confirm(val command: FridayCommand, val reason: String) : FridayDecision
    data class Refuse(val command: FridayCommand, val reason: String) : FridayDecision
}

data class FridayState(
    val identity: FridayIdentity = FridayIdentity(),
    val mode: FridayMode = FridayMode.NORMAL,
    val world: TacticalWorld = TacticalWorld(),
    val vitals: VitalSnapshot = VitalSnapshot(),
    val goals: List<PriorityGoal> = emptyList(),
    val sequence: Long = 0
)
