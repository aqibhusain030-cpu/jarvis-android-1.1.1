package com.jarvis.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight watchdog. Every 60 s calls [onUnhealthy] if the voice loop
 * hasn't pinged in the last 3 minutes. The service bumps the heartbeat after
 * each successful loop iteration.
 */
class JarvisWatchdog(
    private val scope: CoroutineScope,
    @Suppress("UNUSED_PARAMETER") private val ctx: Context,
    private val onUnhealthy: () -> Unit
) {
    @Volatile private var lastBeat = System.currentTimeMillis()
    private var job: Job? = null
    fun beat() { lastBeat = System.currentTimeMillis() }
    fun start() {
        job = scope.launch {
            while (isActive) {
                delay(60_000)
                if (System.currentTimeMillis() - lastBeat > 180_000) {
                    onUnhealthy(); lastBeat = System.currentTimeMillis()
                }
            }
        }
    }
    fun stop() { job?.cancel() }
}
