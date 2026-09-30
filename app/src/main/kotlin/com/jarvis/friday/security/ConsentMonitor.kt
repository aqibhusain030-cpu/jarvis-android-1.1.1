package com.jarvis.friday.security

import java.time.Instant
import java.util.UUID

enum class MonitorSource { APP_EVENTS, NOTIFICATIONS, ACCESSIBILITY_EVENTS, NETWORK_TELEMETRY }

data class MonitorEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Instant = Instant.now(),
    val source: MonitorSource,
    val summary: String
)

interface ConsentMonitor {
    val enabled: Boolean
    fun enable(userConfirmed: Boolean): Boolean
    fun disable()
    suspend fun record(event: MonitorEvent)
    suspend fun recent(limit: Int = 50): List<MonitorEvent>
}

class InMemoryConsentMonitor : ConsentMonitor {
    private val events = ArrayDeque<MonitorEvent>()
    @Volatile private var active = false
    override val enabled: Boolean get() = active

    override fun enable(userConfirmed: Boolean): Boolean {
        if (!userConfirmed) return false
        synchronized(this) { active = true }
        return true
    }
    override fun disable() { synchronized(this) { active = false } }

    override suspend fun record(event: MonitorEvent) {
        synchronized(this) {
            if (!active) return
            events.addLast(event)
            while (events.size > 2000) events.removeFirst()
        }
    }
    override suspend fun recent(limit: Int): List<MonitorEvent> =
        synchronized(this) { events.takeLast(limit.coerceIn(1, 2000)) }
}
