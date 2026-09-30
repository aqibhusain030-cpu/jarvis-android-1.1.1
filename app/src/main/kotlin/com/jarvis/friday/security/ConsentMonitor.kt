package com.jarvis.friday.security

import java.time.Instant
import java.util.UUID

enum class MonitorSource {
    APP_EVENTS,
    NOTIFICATIONS,
    ACCESSIBILITY_EVENTS,
    NETWORK_TELEMETRY
}

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
    @Volatile
    private var active = false

    override val enabled: Boolean
        get() = active

    @Synchronized
    override fun enable(userConfirmed: Boolean): Boolean {
        if (!userConfirmed) return false
        active = true
        return true
    }

    @Synchronized
    override fun disable() {
        active = false
    }

    @Synchronized
    override suspend fun record(event: MonitorEvent) {
        if (!active) return
        events.addLast(event)
        while (events.size > 2000) events.removeFirst()
    }

    @Synchronized
    override suspend fun recent(limit: Int): List<MonitorEvent> =
        events.takeLast(limit.coerceIn(1, 2000))
}
