package com.jarvis.tools.impl

import com.jarvis.memory.MemoryStore
import com.jarvis.tools.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryTool @Inject constructor(private val store: MemoryStore) : Tool {
    override val spec = ToolSpec(
        name = "save_memory",
        descriptionHi = "जानकारी याद रखें",
        descriptionEn = "Persist a fact, preference, or note",
        parameters = mapOf(
            "key" to ParamSpec("string", true, "Short key"),
            "value" to ParamSpec("string", true, "Value to store"),
            "category" to ParamSpec("string", false, "fact|pref|person|place|project", "fact"))
    )
    override suspend fun invoke(args: Map<String, Any?>): ToolResult {
        val k = (args["key"] as? String)?.trim()?.takeIf { it.isNotBlank() } ?: return ToolResult.Err("key required", "bad_arg")
        val v = (args["value"] as? String)?.takeIf { it.isNotBlank() } ?: return ToolResult.Err("value required", "bad_arg")
        if (k.length > 128) return ToolResult.Err("key too long", "bad_arg")
        if (v.length > 10000) return ToolResult.Err("value too long", "bad_arg")
        val c = (args["category"] as? String ?: "fact").lowercase()
        if (c !in setOf("fact", "pref", "person", "place", "project")) return ToolResult.Err("invalid category", "bad_arg")
        store.putMemory(k, v, c)
        return ToolResult.Ok("remembered $k")
    }
}
