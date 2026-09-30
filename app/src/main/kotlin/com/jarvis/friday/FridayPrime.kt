package com.jarvis.friday

import com.jarvis.friday.action.ActionResult
import com.jarvis.friday.action.FridayActionGateway
import com.jarvis.friday.audit.FridayAuditEvent
import com.jarvis.friday.audit.FridayAuditSink
import com.jarvis.friday.core.FridayCommand
import com.jarvis.friday.core.FridayDecision
import com.jarvis.friday.core.FridayExecutive

class FridayPrime(
    private val executive: FridayExecutive,
    private val actionGateway: FridayActionGateway,
    private val audit: FridayAuditSink
) {
    suspend fun process(command: FridayCommand, compromised: Boolean = false): FridayDecision {
        val decision = executive.decide(command, compromised)
        val outcome = when (decision) {
            is FridayDecision.Execute -> when (val result = actionGateway.execute(command)) {
                is ActionResult.Accepted -> result.message
                is ActionResult.Rejected -> result.message
                is ActionResult.Failed -> result.message
            }
            is FridayDecision.Confirm -> decision.reason
            is FridayDecision.Refuse -> decision.reason
        }
        audit.append(FridayAuditEvent(commandId = command.id, event = "executive_decision", risk = command.risk.name, outcome = outcome))
        return decision
    }
}
