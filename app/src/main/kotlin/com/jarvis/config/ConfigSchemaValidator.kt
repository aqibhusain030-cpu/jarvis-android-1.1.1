package com.jarvis.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Strict versioned configuration gate inspired by strongly typed configuration systems. */
object ConfigSchemaValidator {
    private const val SCHEMA_VERSION = 1

    private val rootKeys = setOf("schema_version", "agent", "voice", "llm", "stt", "tts", "autonomy", "safety", "rate_limits", "paths", "cloud")
    private val agentKeys = setOf("name", "user_name", "default_language", "auto_language_detect", "address_style", "max_tool_rounds", "max_context_tokens", "max_output_tokens", "temperature")
    private val voiceKeys = setOf("wake_words", "wake_threshold", "tts_voice_hi", "tts_voice_en", "tts_speed", "tts_pitch", "barge_in", "vad_silence_ms", "max_command_seconds")
    private val llmKeys = setOf("provider", "model", "context_size", "threads", "gpu_layers", "remote_enabled", "remote_base_url", "remote_model", "remote_timeout_ms")
    private val sttKeys = setOf("provider", "model", "language", "threads")
    private val ttsKeys = setOf("provider", "fallback")
    private val autonomyKeys = setOf("enabled", "battery_poll_seconds", "notification_poll_seconds", "morning_briefing_hour", "morning_briefing_minute", "nightly_summary_hour", "max_active_goals", "max_tool_calls_per_goal")
    private val safetyKeys = setOf("communication_require_confirmation", "file_delete_requires_confirmation", "http_post_requires_confirmation", "uninstall_requires_confirmation", "shell_tool_enabled", "notification_cloud_ok", "cloud_fallback_enabled", "pin_required_for_high_risk", "kill_phrase_enabled")
    private val rateKeys = setOf("sms_per_hour", "calls_per_hour", "camera_per_hour", "location_per_hour", "http_per_hour")
    private val pathKeys = setOf("models_dir", "audio_dir", "logs_dir", "audit_dir", "exports_dir")
    private val cloudKeys = setOf("provider")

    fun validate(raw: String) {
        val root = Json.parseToJsonElement(raw).jsonObject
        require(root["schema_version"]?.jsonPrimitive?.intOrNull == SCHEMA_VERSION) { "Unsupported config schema version" }
        checkKeys(root, rootKeys, "root")
        requireObject(root, "agent").also { checkKeys(it, agentKeys, "agent") }
        requireObject(root, "voice").also { checkKeys(it, voiceKeys, "voice") }
        requireObject(root, "llm").also { checkKeys(it, llmKeys, "llm") }
        requireObject(root, "stt").also { checkKeys(it, sttKeys, "stt") }
        requireObject(root, "tts").also { checkKeys(it, ttsKeys, "tts") }
        requireObject(root, "autonomy").also { checkKeys(it, autonomyKeys, "autonomy") }
        requireObject(root, "safety").also { checkKeys(it, safetyKeys, "safety") }
        requireObject(root, "rate_limits").also { checkKeys(it, rateKeys, "rate_limits") }
        requireObject(root, "paths").also { checkKeys(it, pathKeys, "paths") }
        requireObject(root, "cloud").also { checkKeys(it, cloudKeys, "cloud") }

        val agent = root["agent"]!!.jsonObject
        val voice = root["voice"]!!.jsonObject
        val llm = root["llm"]!!.jsonObject
        val stt = root["stt"]!!.jsonObject
        val autonomy = root["autonomy"]!!.jsonObject

        requireString(agent, "name", 1, 32)
        requireString(agent, "user_name", 1, 64)
        requireOneOf(agent, "default_language", setOf("hi", "en", "hinglish"))
        requireBoolean(agent, "auto_language_detect")
        requireOneOf(agent, "address_style", setOf("respectful", "neutral"))
        requireIntRange(agent, "max_tool_rounds", 1, 8)
        requireIntRange(agent, "max_context_tokens", 512, 32768)
        requireIntRange(agent, "max_output_tokens", 32, 4096)
        requireDoubleRange(agent, "temperature", 0.0, 2.0)

        requireArrayOfStrings(voice, "wake_words", 1, 16)
        requireDoubleRange(voice, "wake_threshold", 0.0, 1.0)
        requireString(voice, "tts_voice_hi", 1, 128)
        requireString(voice, "tts_voice_en", 1, 128)
        requireDoubleRange(voice, "tts_speed", 0.25, 4.0)
        requireDoubleRange(voice, "tts_pitch", 0.25, 2.0)
        requireBoolean(voice, "barge_in")
        requireIntRange(voice, "vad_silence_ms", 100, 5000)
        requireIntRange(voice, "max_command_seconds", 1, 120)

        requireOneOf(llm, "provider", setOf("local"))
        requireString(llm, "model", 1, 256)
        requireIntRange(llm, "context_size", 512, 32768)
        requireIntRange(llm, "threads", 1, 64)
        requireIntRange(llm, "gpu_layers", 0, 256)
        requireBoolean(llm, "remote_enabled")
        requireString(llm, "remote_base_url", 0, 2048)
        requireString(llm, "remote_model", 1, 256)
        requireIntRange(llm, "remote_timeout_ms", 5000, 300000)

        requireOneOf(stt, "provider", setOf("local"))
        requireString(stt, "model", 1, 256)
        requireOneOf(stt, "language", setOf("auto", "hi", "en"))
        requireIntRange(stt, "threads", 1, 32)

        requireBoolean(autonomy, "enabled")
        requireIntRange(autonomy, "battery_poll_seconds", 30, 86400)
        requireIntRange(autonomy, "notification_poll_seconds", 30, 86400)
        requireIntRange(autonomy, "morning_briefing_hour", 0, 23)
        requireIntRange(autonomy, "morning_briefing_minute", 0, 59)
        requireIntRange(autonomy, "nightly_summary_hour", 0, 23)
        requireIntRange(autonomy, "max_active_goals", 1, 20)
        requireIntRange(autonomy, "max_tool_calls_per_goal", 1, 1000)
    }

    private fun checkKeys(obj: JsonObject, allowed: Set<String>, where: String) {
        val unknown = obj.keys - allowed
        require(unknown.isEmpty()) { "Unknown config key(s) in " + where + ": " + unknown.sorted() }
    }

    private fun requireObject(root: JsonObject, key: String): JsonObject =
        root[key]?.takeIf { it is JsonObject }?.jsonObject ?: error("Config section '" + key + "' must be an object")

    private fun requirePrimitive(obj: JsonObject, key: String): JsonPrimitive =
        obj[key]?.takeIf { it is JsonPrimitive }?.jsonPrimitive ?: error("Config key '" + key + "' must be a primitive")

    private fun requireString(obj: JsonObject, key: String, min: Int, max: Int) {
        val value = requirePrimitive(obj, key).content
        require(value.length in min..max) { "Config key '" + key + "' length out of range" }
    }

    private fun requireBoolean(obj: JsonObject, key: String) {
        require(requirePrimitive(obj, key).booleanOrNull != null) { "Config key '" + key + "' must be boolean" }
    }

    private fun requireIntRange(obj: JsonObject, key: String, min: Int, max: Int) {
        val value = requirePrimitive(obj, key).intOrNull ?: error("Config key '" + key + "' must be an integer")
        require(value in min..max) { "Config key '" + key + "' out of range" }
    }

    private fun requireDoubleRange(obj: JsonObject, key: String, min: Double, max: Double) {
        val value = requirePrimitive(obj, key).doubleOrNull ?: error("Config key '" + key + "' must be numeric")
        require(value.isFinite() && value in min..max) { "Config key '" + key + "' out of range" }
    }

    private fun requireOneOf(obj: JsonObject, key: String, allowed: Set<String>) {
        val value = requirePrimitive(obj, key).content
        require(value in allowed) { "Config key '" + key + "' has unsupported value" }
    }

    private fun requireArrayOfStrings(obj: JsonObject, key: String, min: Int, max: Int) {
        val array = obj[key]?.takeIf { it is JsonArray }?.jsonArray ?: error("Config key '" + key + "' must be an array")
        require(array.size in min..max) { "Config key '" + key + "' size out of range" }
        require(array.all { it is JsonPrimitive && it.jsonPrimitive.isString }) { "Config key '" + key + "' must contain only strings" }
    }
}
