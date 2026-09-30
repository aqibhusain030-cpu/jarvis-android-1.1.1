package com.jarvis.config

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Parses the JSON config asset into a [JarvisConfig]. */
object Defaults {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): JarvisConfig {
        ConfigSchemaValidator.validate(raw)
        val obj = json.parseToJsonElement(raw).jsonObject
        val agent = obj["agent"]?.jsonObject
        val voice = obj["voice"]?.jsonObject
        val llm = obj["llm"]?.jsonObject
        val stt = obj["stt"]?.jsonObject
        val safety = obj["safety"]?.jsonObject
        val autonomy = obj["autonomy"]?.jsonObject

        fun str(m: JsonObject?, k: String, d: String) =
            m?.get(k)?.jsonPrimitive?.content ?: d
        fun f(m: JsonObject?, k: String, d: Float) =
            m?.get(k)?.jsonPrimitive?.content?.toFloatOrNull() ?: d
        fun i(m: JsonObject?, k: String, d: Int) =
            m?.get(k)?.jsonPrimitive?.content?.toIntOrNull() ?: d
        fun b(m: JsonObject?, k: String, d: Boolean) =
            m?.get(k)?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: d

        return JarvisConfig(
            userName = str(agent, "user_name", "Sir"),
            defaultLanguage = str(agent, "default_language", "hi"),
            wakeThreshold = f(voice, "wake_threshold", 0.5f),
            vadSilenceMs = i(voice, "vad_silence_ms", 800),
            maxCommandSeconds = i(voice, "max_command_seconds", 20),
            ttsSpeed = f(voice, "tts_speed", 1.0f),
            ttsPitch = f(voice, "tts_pitch", 1.0f),
            llmModel = str(llm, "model", "Qwen2.5-3B-Instruct-Q4_K_M.gguf"),
            llmContextSize = i(llm, "context_size", 4096),
            llmThreads = i(llm, "threads", 4),
            cloudFallbackEnabled = b(safety, "cloud_fallback_enabled", false),
            remoteInferenceEnabled = b(llm, "remote_enabled", false),
            remoteInferenceBaseUrl = str(llm, "remote_base_url", ""),
            remoteInferenceModel = str(llm, "remote_model", "JARVIS-2T-MoE"),
            remoteInferenceTimeoutMs = i(llm, "remote_timeout_ms", 60000),
            sttModel = str(stt, "model", "ggml-small.bin"),
            sttLanguage = str(stt, "language", "auto"),
            sttThreads = i(stt, "threads", 4),
            communicationRequireConfirmation = b(safety, "communication_require_confirmation", true),
            fileDeleteRequiresConfirmation = b(safety, "file_delete_requires_confirmation", true),
            httpPostRequiresConfirmation = b(safety, "http_post_requires_confirmation", true),
            uninstallRequiresConfirmation = b(safety, "uninstall_requires_confirmation", true),
            shellEnabled = b(safety, "shell_tool_enabled", false),
            pinRequiredForHighRisk = b(safety, "pin_required_for_high_risk", true),
            killPhraseEnabled = b(safety, "kill_phrase_enabled", true),
            maxActiveGoals = i(autonomy, "max_active_goals", 10),
            maxToolCallsPerGoal = i(autonomy, "max_tool_calls_per_goal", 200)
        )
    }
}
