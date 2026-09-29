package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.command.ActionPlan
import com.jarvis.tools.ToolArgsCodec
import org.junit.Test

class ActionPlanTest {
    @Test fun sameInputsHaveStableCanonicalArguments() {
        val a = ToolArgsCodec.canonical(mapOf("z" to 1, "a" to "x"))
        val b = ToolArgsCodec.canonical(mapOf("a" to "x", "z" to 1))
        assertThat(a).isEqualTo(b)
    }

    @Test fun planHashChangesWhenArgumentsChange() {
        val first = ActionPlan.create(
            "send it",
            "en",
            listOf(ActionPlan.PlannedAction("a", "send_sms", mapOf("body" to "one"), "send"))
        )
        val second = first.copy(
            actions = listOf(ActionPlan.PlannedAction("a", "send_sms", mapOf("body" to "two"), "send"))
        )
        assertThat(first.hash).isNotEqualTo(second.hash)
    }

    @Test fun expiredPlanIsRejectedByClock() {
        val plan = ActionPlan.create("x", "en", emptyList(), nowMs = 1000L)
        assertThat(plan.isExpired(61000L)).isTrue()
        assertThat(plan.isExpired(59999L)).isFalse()
    }
}
