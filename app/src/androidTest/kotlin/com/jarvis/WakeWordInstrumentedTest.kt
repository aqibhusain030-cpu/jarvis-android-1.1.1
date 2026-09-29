package com.jarvis

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Real device/emulator smoke test for the activity startup path. */
@RunWith(AndroidJUnit4::class)
class WakeWordInstrumentedTest {
    @Test
    fun mainActivityLaunchesCleanly() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(MainActivity::class.java, activity::class.java)
            }
        }
    }
}
