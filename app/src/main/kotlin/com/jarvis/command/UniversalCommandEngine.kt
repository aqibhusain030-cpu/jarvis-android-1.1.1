package com.jarvis.command

import com.jarvis.brain.BrainBridge
import com.jarvis.logging.JarvisLogger
import com.jarvis.memory.MemoryStore
import com.jarvis.tools.ConfirmationManager
import com.jarvis.tools.ToolArgsCodec
import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolRegistry
import com.jarvis.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

sealed class CommandExecutionResult {
    data class Completed(val spokenText: String, val results: List<ToolResult>) : CommandExecutionResult()
    data class Conversation(val spokenText: String) : CommandExecutionResult()
    data class Failed(val spokenText: String, val code: String) : CommandExecutionResult()
}

@Singleton
class UniversalCommandEngine @Inject constructor(
    private val brain: BrainBridge,
    private val registry: ToolRegistry,
    private val dispatcher: ToolDispatcher,
    private val confirmation: ConfirmationManager,
    private val memory: MemoryStore,
    private val logger: JarvisLogger
) {
    suspend fun handle(userText: String): CommandExecutionResult {
        val draft = brain.planCommand(userText)
        if (draft.actions.isEmpty()) return CommandExecutionResult.Conversation(brain.respond(userText).text)
        if (draft.actions.size > MAX_ACTIONS) {
            return CommandExecutionResult.Failed("Sir, the plan contains too many actions.", "plan_too_large")
        }

        val ids = HashSet<String>()
        val actions = ArrayList<ActionPlan.PlannedAction>(draft.actions.size)
        for (item in draft.actions) {
            val id = item.id.trim()
            val name = item.tool.trim()
            if (id.isEmpty() || !ids.add(id)) {
                return CommandExecutionResult.Failed("Sir, the command plan is invalid.", "invalid_action_id")
            }
            val tool = registry.get(name)
                ?: return CommandExecutionResult.Failed("Sir, capability '$name' is not installed.", "unknown_tool")
            val args = item.args.mapValues { ToolArgsCodec.elementToAny(it.value) }
            dispatcher.preflight(name, args)?.let { return CommandExecutionResult.Failed(it.message, it.code) }
            actions += ActionPlan.PlannedAction(
                actionId = id,
                toolName = tool.spec.name,
                args = args,
                expectedEffect = item.expectedEffect.ifBlank { tool.spec.descriptionEn }
            )
        }

        val plan = ActionPlan.create(userText, draft.language, actions)
        if (!dispatcher.lockPlan(
                plan.hash,
                plan.expiresAtMs,
                plan.actions.map {
                    ToolDispatcher.LockedAction(
                        it.actionId, it.toolName, ToolArgsCodec.canonical(it.args)
                    )
                }
            )
        ) {
            return CommandExecutionResult.Failed("Sir, I could not lock the plan.", "plan_lock_failed")
        }

        val confirmed = try {
            confirmation.ask(prompt(plan))
        } catch (t: Throwable) {
            logger.e(TAG, "confirmation failed", t)
            false
        }
        if (!confirmed) {
            dispatcher.discardPlan(plan.hash)
            memory.recordMistake("command_plan", "authorization", "cancelled", "User cancelled or confirmation timed out")
            return CommandExecutionResult.Failed(cancelled(draft.language), "cancelled")
        }
        if (!dispatcher.confirmPlan(plan.hash)) {
            dispatcher.discardPlan(plan.hash)
            return CommandExecutionResult.Failed("Sir, the confirmed plan is no longer valid.", "plan_invalid")
        }

        val results = ArrayList<ToolResult>(actions.size)
        for (action in actions) {
            val result = dispatcher.invokeLockedPlanAction(plan.hash, action.actionId, action.toolName, action.args)
            results += result
            if (result is ToolResult.Err) {
                dispatcher.discardPlan(plan.hash)
                return CommandExecutionResult.Failed(
                    "Sir, execution stopped at ${action.actionId}: ${result.message}",
                    result.code
                )
            }
        }
        dispatcher.discardPlan(plan.hash)
        return CommandExecutionResult.Completed(
            if (actions.size == 1) "Done, sir." else "Done, sir. Executed ${actions.size} confirmed actions.",
            results
        )
    }

    private fun prompt(plan: ActionPlan): String {
        val steps = plan.actions.mapIndexed { i, action ->
            val args = action.args.entries.joinToString(", ") { "${it.key}=${sanitize(it.value)}" }
            "${i + 1}) ${action.expectedEffect}${if (args.isBlank()) "" else " [$args]"}"
        }
        return "Sir, I will execute this plan: ${steps.joinToString("; ")}. Confirm?"
    }

    private fun sanitize(value: Any?): String =
        value?.toString()?.let { if (it.length > 180) it.take(177) + "..." else it } ?: "null"

    private fun cancelled(language: String): String =
        if (language == "en") "Understood, sir. I did not execute the plan."
        else "समझ गया सर। मैंने यह plan execute नहीं किया।"

    companion object {
        private const val TAG = "UniversalCommandEngine"
        private const val MAX_ACTIONS = 8
    }
}
