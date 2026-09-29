package com.jarvis.tools.impl

import com.jarvis.autonomy.GoalEngine
import com.jarvis.tools.*
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalTool @Inject constructor(private val goals: Lazy<GoalEngine>) : Tool {
    override val spec = ToolSpec(
        name = "create_goal",
        descriptionHi = "लंबा लक्ष्य बनाएँ",
        descriptionEn = "Create a long-running autonomous goal",
        parameters = mapOf(
            "title" to ParamSpec("string", true, "Goal title"),
            "details" to ParamSpec("string", true, "Plain-language description"),
            "priority" to ParamSpec("int", false, "1..5", "3")
        ),
        confirmation = Confirmation.VOICE,
        rateLimitPerHour = 20
    )

    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val title = args["title"] as? String ?: return ToolResult.Err("title required", "bad_arg")
        val desc = args["details"] as? String ?: title
        val pr = (args["priority"] as? Number)?.toInt() ?: 3
        val id = goals.get().create(title, desc, pr)
        if (id.isBlank()) {
            return ToolResult.Err("max active goals reached", "goal_limit")
        }
        return ToolResult.Ok("goal $id created")
    }
}
