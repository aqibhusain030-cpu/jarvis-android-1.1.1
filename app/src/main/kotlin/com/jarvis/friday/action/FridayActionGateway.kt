package com.jarvis.friday.action

import com.jarvis.friday.core.FridayCommand

interface FridayActionGateway { suspend fun execute(command: FridayCommand): ActionResult }

sealed interface ActionResult {
    data class Accepted(val message: String) : ActionResult
    data class Rejected(val message: String) : ActionResult
    data class Failed(val message: String, val recoverable: Boolean = true) : ActionResult
}

class SimulationActionGateway : FridayActionGateway {
    override suspend fun execute(command: FridayCommand): ActionResult =
        ActionResult.Accepted("Simulation accepted: " + command.text)
}
