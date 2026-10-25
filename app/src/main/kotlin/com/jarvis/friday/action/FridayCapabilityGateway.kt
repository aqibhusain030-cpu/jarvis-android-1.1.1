package com.jarvis.friday.action

import com.jarvis.core.capability.CapabilityExecutor
import com.jarvis.core.capability.CapabilityRequest
import com.jarvis.core.capability.CapabilityResult
import com.jarvis.friday.core.FridayCommand

class FridayCapabilityGateway(
    private val executor: CapabilityExecutor
) : FridayActionGateway {
    override suspend fun execute(command: FridayCommand): ActionResult {
        val capabilityId = command.capabilityId
            ?: return ActionResult.Rejected("No capability was bound to this command.")

        val result = executor.execute(
            CapabilityRequest(
                capabilityId = capabilityId,
                arguments = command.arguments,
                requestId = command.id,
                source = CapabilityRequest.Source.ASSISTANT,
                confirmationGranted = command.confirmationGranted
            )
        )

        return when (result) {
            is CapabilityResult.Success -> ActionResult.Accepted(result.message)
            is CapabilityResult.Rejected -> ActionResult.Rejected(result.message)
            is CapabilityResult.Failed -> ActionResult.Failed(result.message, result.recoverable)
        }
    }
}
