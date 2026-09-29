package com.jarvis.autonomy

import com.jarvis.brain.PythonRuntime
import com.jarvis.logging.JarvisLogger
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Turns a natural-language goal into a list of [PlanStep]s via the LLM. */
@Singleton
class Planner @Inject constructor(
    private val python: PythonRuntime,
    private val logger: JarvisLogger
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun plan(title: String, description: String): List<PlanStep> = try {
        python.startIfNeeded()
        val out = python.call("plan_goal", title, description)?.toString() ?: "[]"
        json.decodeFromString(ListSerializer(PlanStep.serializer()), out)
    } catch (t: Throwable) {
        logger.e(TAG, "plan failed", t)
        listOf(PlanStep(id = "s1", description = title, status = StepStatus.PENDING))
    }

    companion object { private const val TAG = "Planner" }
}
