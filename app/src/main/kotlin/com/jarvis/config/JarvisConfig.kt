package com.jarvis.config

import kotlinx.serialization.Serializable

@Serializable
data class JarvisConfig(
    val coreEnabled: Boolean = true,
    val userName: String = "Sir",
    val defaultLanguage: String = "hi",
    val autoLanguageDetect: Boolean = true,
    val wakeThreshold: Float = 0.5f,
    val vadSilenceMs: Int = 800,
    val maxCommandSeconds: Int = 20,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val llmModel: String = "qwen2.5-3b-instruct-q4_k_m.gguf",
    val llmContextSize: Int = 4096,
    val llmThreads: Int = 4,
    val sttModel: String = "ggml-small.bin",
    val sttLanguage: String = "auto",
    val sttThreads: Int = 4,
    val maxActiveGoals: Int = 10,
    val maxToolCallsPerGoal: Int = 200,
    val communicationRequireConfirmation: Boolean = true,
    val fileDeleteRequiresConfirmation: Boolean = true,
    val httpPostRequiresConfirmation: Boolean = true,
    val uninstallRequiresConfirmation: Boolean = true,
    val shellEnabled: Boolean = false,
    val pinRequiredForHighRisk: Boolean = true,
    val cloudFallbackEnabled: Boolean = false,
    val remoteInferenceEnabled: Boolean = false,
    val remoteInferenceBaseUrl: String = "",
    val remoteInferenceModel: String = "JARVIS-2T-MoE",
    val remoteInferenceTimeoutMs: Int = 60_000,
    val killPhraseEnabled: Boolean = true
)
