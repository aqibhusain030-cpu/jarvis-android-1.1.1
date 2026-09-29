package com.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.jarvis.brain.PythonRuntime
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.memory.MemoryStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Application entry point. Initialization only; microphone capture is user-started. */
@HiltAndroidApp
class JarvisApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob())

    @Inject lateinit var config: ConfigRepository
    @Inject lateinit var memory: MemoryStore
    @Inject lateinit var python: PythonRuntime
    @Inject lateinit var logger: JarvisLogger

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        appScope.launch {
            runCatching { config.seedFromAssetIfEmpty() }
                .onFailure { logger.e("Config", "seed failed", it) }
            runCatching { memory.open() }
                .onFailure { logger.e("Memory", "open failed", it) }
            runCatching { python.startIfNeeded() }
                .onFailure { logger.e("Python", "start failed", it) }

        }
    }


    companion object {
        const val CH_FOREGROUND = "jarvis_foreground"
        const val CH_ALERTS = "jarvis_alerts"
        const val CH_GOALS = "jarvis_goals"

        fun createNotificationChannels(ctx: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_FOREGROUND, "JARVIS Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Always-on wake-word listening" }
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_ALERTS, "JARVIS Alerts", NotificationManager.IMPORTANCE_HIGH)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_GOALS, "JARVIS Goals", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
