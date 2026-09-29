package com.jarvis.tools

import com.jarvis.config.ConfigRepository
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Sliding-window limiter; counts calls in the last 3600 s per tool. */
@Singleton
class RateLimiter @Inject constructor(private val config: ConfigRepository) {
    private val history = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun allow(tool: String, perHour: Int): Boolean {
        if (perHour <= 0) return true
        val now = System.currentTimeMillis()
        val q = history.getOrPut(tool) { ArrayDeque() }
        synchronized(q) {
            while (true) {
                val first = q.peekFirst() ?: break
                if (now - first <= WINDOW_MS) break
                q.removeFirst()
            }
            if (q.size >= perHour) return false
            q.addLast(now)
            return true
        }
    }

    companion object {
        private const val WINDOW_MS = 3_600_000L
    }
}
