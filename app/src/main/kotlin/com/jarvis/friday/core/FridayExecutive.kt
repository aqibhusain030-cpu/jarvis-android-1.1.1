package com.jarvis.friday.core

class FridayExecutive {
    fun decide(command: FridayCommand, compromised: Boolean = false): FridayDecision {
        if (compromised && FridayDirectives.ISOLATE_ON_COMPROMISE)
            return FridayDecision.Refuse(command, "Execution isolated because the control plane reports compromise.")
        if (command.risk == FridayRisk.CRITICAL || command.confirmationRequired)
            return FridayDecision.Confirm(command, "Explicit user confirmation is required.")
        return FridayDecision.Execute(command)
    }

    fun prioritize(goals: List<PriorityGoal>): List<PriorityGoal> =
        goals.sortedWith(
            compareByDescending<PriorityGoal> { it.priority }
                .thenBy { it.deadlineEpochMs ?: Long.MAX_VALUE }
                .thenBy { it.id }
        )
}
