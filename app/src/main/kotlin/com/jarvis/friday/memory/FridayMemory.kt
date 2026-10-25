package com.jarvis.friday.memory

import java.time.Instant

interface FridayMemory {
    suspend fun remember(event: MemoryEvent)
    suspend fun recall(query: String, limit: Int = 20): List<MemoryEvent>
}

data class MemoryEvent(
    val id: String,
    val timestamp: Instant,
    val type: Type,
    val summary: String,
    val sensitivity: Sensitivity = Sensitivity.NORMAL
) {
    enum class Type { COMMAND, OBSERVATION, DECISION, OUTCOME, SYSTEM }
    enum class Sensitivity { NORMAL, SENSITIVE, RESTRICTED }
}

class InMemoryFridayMemory : FridayMemory {
    private val events = ArrayDeque<MemoryEvent>()
    override suspend fun remember(event: MemoryEvent) {
        synchronized(this) {
            events.addLast(event)
            while (events.size > 5000) events.removeFirst()
        }
    }
    override suspend fun recall(query: String, limit: Int): List<MemoryEvent> =
        synchronized(this) {
            events.asSequence()
                .filter { it.summary.contains(query, true) }
                .toList()
                .takeLast(limit.coerceIn(1, 5000))
        }
}
