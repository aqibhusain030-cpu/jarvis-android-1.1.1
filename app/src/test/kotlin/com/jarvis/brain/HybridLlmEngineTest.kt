package com.jarvis.brain

import com.jarvis.logging.JarvisLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class HybridLlmEngineTest {
    @Test
    fun fallsBackToLocalWhenRemoteReturnsEmpty() {
        val local = mockk<LocalLlmEngine>()
        val remote = mockk<RemoteLlmEngine>()
        val logger = mockk<JarvisLogger>(relaxed = true)

        coEvery { remote.generate(any(), any(), any()) } returns ""
        every { local.loadIfNeeded() } returns true
        every { local.generate(any(), any(), any()) } returns "local answer"

        val engine = HybridLlmEngine(local, remote, logger)
        assertEquals("local answer", engine.generate("hello"))
    }
}
