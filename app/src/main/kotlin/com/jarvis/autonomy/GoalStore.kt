package com.jarvis.autonomy

import com.jarvis.memory.MemoryStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalStore @Inject constructor(private val memory: MemoryStore) {
    suspend fun save(goal: Goal) = memory.putGoal(goal)
    suspend fun running(): List<Goal> = memory.runningGoals()
    suspend fun all(): List<Goal> = memory.allGoals()
    suspend fun get(id: String): Goal? = memory.getGoal(id)
    suspend fun updateStatus(id: String, status: GoalStatus) =
        memory.updateGoalStatus(id, status)
}
