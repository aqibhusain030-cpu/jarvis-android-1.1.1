package com.jarvis.tools.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppTaskToolTest {
    @Test
    fun exposes_bounded_voice_task_contract() {
        val spec = AppTaskTool().spec
        assertThat(spec.parameters.keys).containsAtLeast("package", "steps", "max_steps")
        assertThat(spec.confirmation).isEqualTo(com.jarvis.tools.Confirmation.VOICE)
        assertThat(spec.timeoutMs).isEqualTo(30_000L)
    }
}
