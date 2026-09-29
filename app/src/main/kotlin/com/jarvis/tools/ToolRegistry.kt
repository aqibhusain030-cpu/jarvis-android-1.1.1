package com.jarvis.tools

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor() {
    private val tools = linkedMapOf<String, Tool>()
    fun register(tool: Tool) { tools[tool.spec.name] = tool }
    fun get(name: String): Tool? = tools[name]
    fun all(): Collection<Tool> = tools.values
    fun specs(): List<ToolSpec> = tools.values.map { it.spec }
}
