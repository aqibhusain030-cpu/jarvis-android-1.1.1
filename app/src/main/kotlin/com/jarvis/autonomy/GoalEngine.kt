package com.jarvis.autonomy

import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.memory.MemoryStore
import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan → execute → reflect loop for long-running goals.
 * Enforces maxActiveGoals, maxRetries per step, maxRuntimeSeconds, maxToolCallsPerGoal.
 * Failed steps that still have retries are reset to PENDING; exhausted steps block the goal.
 */
@Singleton
class GoalEngine @Inject constructor(
    private val dispatcher: ToolDispatcher,
    private val planner: Planner,
    private val memory: MemoryStore,
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val scope: CoroutineScope
) {
    private val createMutex = Mutex()
    private val activeCount = AtomicInteger(0)

    suspend fun create(title: String, description: String, priority: Int): String {
        val max = config.current.value.maxActiveGoals.coerceAtLeast(1)
        return createMutex.withLock {
            val running = memory.runningGoals().size
            if (running >= max) {
                logger.w(TAG, "maxActiveGoals=$max reached; refusing new goal")
                return@withLock ""
            }
            val id = UUID.randomUUID().toString()
            val plan = planner.plan(title, description)
            val goal = Goal(
                id = id, title = title, description = description,
                priority = priority.coerceIn(1, 5), plan = plan, status = GoalStatus.PENDING
            )
            memory.putGoal(goal)
            activeCount.incrementAndGet()
            scope.launch {
                try { run(goal) }
                finally { activeCount.updateAndGet { (it - 1).coerceAtLeast(0) } }
            }
            id
        }
    }

    suspend fun resumePersisted() {
        createMutex.withLock {
            val max = config.current.value.maxActiveGoals.coerceAtLeast(1)
            val running = memory.runningGoals().sortedByDescending { it.priority }
            for (g in running.take(max)) {
                logger.i(TAG, "Resuming goal ${g.id}")
                activeCount.incrementAndGet()
                scope.launch {
                    try { run(g) }
                    finally { activeCount.updateAndGet { (it - 1).coerceAtLeast(0) } }
                }
            }
            if (running.size > max) {
                logger.w(TAG, "${running.size - max} persisted goals deferred by maxActiveGoals=$max")
            }
        }
    }

    private suspend fun run(goal: Goal) {
        var calls = 0
        var consecutiveFailures = 0
        val start = System.currentTimeMillis()
        goal.status = GoalStatus.RUNNING
        memory.putGoal(goal)

        while (true) {
            if (goal.status == GoalStatus.CANCELLED || goal.status == GoalStatus.PAUSED) return
            if (calls >= config.current.value.maxToolCallsPerGoal ||
                (System.currentTimeMillis() - start) / 1000 > goal.maxRuntimeSeconds
            ) {
                goal.status = GoalStatus.FAILED
                memory.putGoal(goal)
                return
            }
            val step = goal.plan.firstOrNull { it.status == StepStatus.PENDING } ?: break
            val ok = executeStep(goal, step)
            calls++
            if (!ok) {
                consecutiveFailures++
                if (consecutiveFailures >= CONSECUTIVE_FAILURE_LIMIT ||
                    step.attempts >= goal.maxRetries
                ) {
                    goal.status = GoalStatus.BLOCKED
                    memory.putGoal(goal)
                    return
                }
                // Reset for retry so the loop finds it as PENDING again.
                step.status = StepStatus.PENDING
                memory.putGoal(goal)
            } else {
                consecutiveFailures = 0
            }
        }

        // Only DONE when every step succeeded.
        val allDone = goal.plan.all { it.status == StepStatus.DONE }
        goal.status = if (allDone) GoalStatus.DONE else GoalStatus.FAILED
        goal.updatedAt = System.currentTimeMillis()
        memory.putGoal(goal)
        logger.i(TAG, "Goal finished: ${goal.id} status=${goal.status}")
    }

    private suspend fun executeStep(goal: Goal, step: PlanStep): Boolean {
        step.status = StepStatus.RUNNING
        step.startedAt = System.currentTimeMillis()
        step.attempts++
        memory.putGoal(goal)
        val tool = step.tool
        if (tool.isNullOrBlank()) {
            step.status = StepStatus.DONE
            step.result = "no tool"
            return true
        }
        return when (val r = dispatcher.invoke(tool, step.args)) {
            is ToolResult.Ok -> {
                step.status = StepStatus.DONE
                step.result = r.data?.toString()
                step.finishedAt = System.currentTimeMillis()
                memory.putGoal(goal)
                true
            }
            is ToolResult.Err -> {
                step.status = StepStatus.FAILED
                step.result = r.message
                memory.putGoal(goal)
                val backoffSec = (1L shl (step.attempts - 1).coerceAtMost(8)).coerceAtMost(300)
                delay(backoffSec * 1000)
                false
            }
        }
    }

    fun pause(id: String) {
        scope.launch { memory.updateGoalStatus(id, GoalStatus.PAUSED) }
    }

    fun cancel(id: String) {
        scope.launch { memory.updateGoalStatus(id, GoalStatus.CANCELLED) }
    }

    fun resume(id: String) {
        scope.launch {
            createMutex.withLock {
                val max = config.current.value.maxActiveGoals.coerceAtLeast(1)
                if (activeCount.get() >= max) {
                    logger.w(TAG, "maxActiveGoals=$max reached; refusing resume id=$id")
                    return@withLock
                }
                val g = memory.getGoal(id)?.takeIf { it.status != GoalStatus.DONE && it.status != GoalStatus.CANCELLED } ?: return@withLock
                activeCount.incrementAndGet()
                g.status = GoalStatus.RUNNING
                memory.putGoal(g)
                scope.launch {
                    try { run(g) }
                    finally { activeCount.updateAndGet { (it - 1).coerceAtLeast(0) } }
                }
            }
        }
    }

    companion object {
        private const val TAG = "GoalEngine"
        private const val CONSECUTIVE_FAILURE_LIMIT = 5
    }
}
