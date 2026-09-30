package com.jarvis.friday.action

import com.jarvis.friday.core.FridayCommand
import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolResult

/**
 * Routes FRIDAY operations through the existing JARVIS ToolDispatcher.
 * This preserves the repository's established permission, confirmation,
 * rate-limit, network-policy, timeout and audit controls.
 */
class FridayToolGateway(
    private val dispatcher: ToolDispatcher
) : FridayActionGateway {

    override suspend fun execute(command: FridayCommand): ActionResult {
        val name = command.toolName
            ?: return ActionResult.Rejected("No JARVIS tool is bound to this command.")

        val result = dispatcher.invoke(
            name = name,
            args = command.arguments.mapValues { (_, value) -> value }
        )

        return when (result) {
            is ToolResult.Ok -> ActionResult.Accepted("Operation completed.")
            is ToolResult.Err -> ActionResult.Failed(result.message, result.code != "permission_missing")
        }
    }
}
