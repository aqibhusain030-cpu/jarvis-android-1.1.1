package com.jarvis.friday

import com.google.common.truth.Truth.assertThat
import com.jarvis.friday.action.SimulationActionGateway
import com.jarvis.friday.audit.FridayAuditSink
import com.jarvis.friday.audit.FridayAuditEvent
import com.jarvis.friday.core.*
import com.jarvis.friday.perception.SensorSample
import com.jarvis.friday.perception.TacticalWorldModel
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FridayArchitectureTest {
    @Test fun critical_commands_require_confirmation() {
        val command = FridayCommand("critical", risk = FridayRisk.CRITICAL)
        assertThat(FridayExecutive().decide(command)).isInstanceOf(FridayDecision.Confirm::class.java)
    }
    @Test fun compromised_control_plane_refuses_execution() {
        val command = FridayCommand("test")
        assertThat(FridayExecutive().decide(command, true)).isInstanceOf(FridayDecision.Refuse::class.java)
    }
    @Test fun goals_are_deterministically_prioritized() {
        val goals = listOf(PriorityGoal(objective="secondary",priority=1), PriorityGoal(objective="primary",priority=10))
        assertThat(FridayExecutive().prioritize(goals).first().objective).isEqualTo("primary")
    }
    @Test fun world_model_fuses_unique_actors_and_hazards() {
        val world = TacticalWorldModel().fuse(listOf(
            SensorSample.Actors(listOf(TacticalActor("a", TacticalActor.ActorRole.ALLY, .9))),
            SensorSample.Actors(listOf(TacticalActor("a", TacticalActor.ActorRole.ALLY, .8), TacticalActor("b", TacticalActor.ActorRole.CIVILIAN, .99))),
            SensorSample.Hazards(listOf("fire", "fire"))
        ))
        assertThat(world.actors).hasSize(2)
        assertThat(world.hazards).containsExactly("fire")
    }
    @Test fun prime_audits_decision() = runTest {
        val audit = object : FridayAuditSink {
            var count = 0
            override suspend fun append(event: FridayAuditEvent) { count++ }
        }
        FridayPrime(FridayExecutive(), SimulationActionGateway(), audit).process(FridayCommand("status"))
        assertThat(audit.count).isEqualTo(1)
    }
}
