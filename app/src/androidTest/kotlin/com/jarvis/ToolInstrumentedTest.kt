package com.jarvis

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.tools.impl.BatteryTool
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolInstrumentedTest {
    @Test fun batteryToolReportsValue() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val r = BatteryTool(ctx).invoke(emptyMap())
        assertNotNull(r)
    }
}
