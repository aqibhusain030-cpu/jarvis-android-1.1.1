package com.jarvis.friday.audit

import java.time.Instant

interface FridayAuditSink { suspend fun append(event: FridayAuditEvent) }

data class FridayAuditEvent(
    val timestamp: Instant = Instant.now(),
    val commandId: String,
    val event: String,
    val risk: String,
    val outcome: String
)

class InMemoryFridayAudit : FridayAuditSink {
    private val events = ArrayDeque<FridayAuditEvent>()
    override suspend fun append(event: FridayAuditEvent) {
        synchronized(this) {
            events.addLast(event)
            while (events.size > 5000) events.removeFirst()
        }
    }
    fun size(): Int = synchronized(this) { events.size }
}
