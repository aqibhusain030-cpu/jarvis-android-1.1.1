package com.jarvis.command

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PlannedActionDraft(
    val id: String,
    val tool: String,
    val args: Map<String, JsonElement> = emptyMap(),
    val expectedEffect: String = ""
)

@Serializable
data class CommandDraft(
    val understood: String = "",
    val language: String = "en",
    val actions: List<PlannedActionDraft> = emptyList()
)
