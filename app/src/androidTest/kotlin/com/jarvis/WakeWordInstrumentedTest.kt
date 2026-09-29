package com.jarvis

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Manual smoke test: verifies the app context initializes without crashing. */
@RunWith(AndroidJUnit4::class)
class WakeWordInstrumentedTest {
    @Test fun contextBootsCleanly() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(ctx.packageName == "com.jarvis")
    }
}
