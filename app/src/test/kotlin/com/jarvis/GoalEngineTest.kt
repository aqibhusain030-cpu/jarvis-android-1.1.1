package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.autonomy.*
import org.junit.Test

class GoalEngineTest {
    @Test fun goalDefaults() {
        val g = Goal(id = "g1", title = "Wake me at 6", description = "alarm + brief")
        assertThat(g.status).isEqualTo(GoalStatus.PENDING)
        assertThat(g.priority).isEqualTo(3)
        assertThat(g.maxRetries).isEqualTo(3)
    }
    @Test fun planStepDefaults() {
        val s = PlanStep(id = "s1", description = "check battery")
        assertThat(s.status).isEqualTo(StepStatus.PENDING)
        assertThat(s.attempts).isEqualTo(0)
    }
}
