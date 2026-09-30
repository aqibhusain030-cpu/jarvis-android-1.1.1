package com.jarvis.friday.action

import com.jarvis.friday.core.FridayCommand
import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolResult

class FridayToolGateway(private val dispatcher: ToolDispatcher) : FridayActionGateway {
    override suspend fun execute(command: FridayCommand): ActionResult {
        val name = command.capabilityId
            ?.let { com.jarvis.friday.core.FridayOperationCatalog.operations.firstOrNull { op -> op.id == it }?.existingTool }
            ?: return ActionResult.Rejected("No JARVIS tool is bound to this command.")

        val result = dispatcher.invoke(
            name = name,
            args = command.arguments.mapValues { it.value }
        )

        return when (result) {
            is ToolResult.Ok -> ActionResult.Accepted("Operation completed.")
            is ToolResult.Err -> ActionResult.Failed(result.message, result.code != "permission_missing")
        }
    }
}
