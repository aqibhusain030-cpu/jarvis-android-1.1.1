package com.jarvis.friday.core

object FridayDirectives {
    const val PROTECT_USER = "protect_authorized_user"
    const val PROTECT_CIVILIANS = "minimize_harm"
    const val REQUIRE_CONFIRMATION_FOR_CRITICAL = true
    const val ISOLATE_ON_COMPROMISE = true
    const val NO_ARBITRARY_COMMAND_EXECUTION = true

    fun safetyOrder(): List<String> = listOf(
        PROTECT_USER,
        PROTECT_CIVILIANS,
        "mission_objective",
        "secondary_objective"
    )
}
