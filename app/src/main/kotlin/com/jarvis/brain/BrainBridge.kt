package com.jarvis.brain

import com.jarvis.logging.JarvisLogger
import com.jarvis.memory.MemoryStore
import com.jarvis.voice.LanguageDetector
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BrainReply(
    val text: String,
    val language: String,
    val toolCalls: List<String> = emptyList()
)

/**
 * Kotlin façade over the Python Brain. Ensures [BrainBootstrap] has run,
 * then serializes context and deserializes the structured reply.
 */
@Singleton
class BrainBridge @Inject constructor(
    private val python: PythonRuntime,
    private val bootstrap: BrainBootstrap,
    private val memory: MemoryStore,
    private val lang: LanguageDetector,
    private val logger: JarvisLogger
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun respond(userText: String): BrainReply {
        bootstrap.bootstrap()
        python.startIfNeeded()
        val detected = lang.explicitOverride(userText) ?: lang.detect(userText)
        val context = memory.recentContext(limit = 12)
        val lessons = memory.relevantLessons(userText, limit = 5).map {
            "lesson: tool=" + it.tool + "; error=" + it.errorCode + "; correction=" + it.correction + "; confidence=" + it.confidence
        }
        val payload = json.encodeToString(
            ContextEnvelope.serializer(),
            ContextEnvelope(
                userText = userText,
                detectedLanguage = when (detected) {
                    LanguageDetector.Lang.HI -> "hi"
                    LanguageDetector.Lang.EN -> "en"
                    LanguageDetector.Lang.HINGLISH -> "hinglish"
                },
                recentMessages = context,
                lessons = lessons
            )
        )
        return try {
            val out = python.call("respond", payload)?.toString() ?: ""
            if (out.isBlank()) {
                BrainReply(
                    text = fallback(detected),
                    language = if (detected == LanguageDetector.Lang.EN) "en" else "hi"
                )
            } else {
                json.decodeFromString(BrainReply.serializer(), out)
            }
        } catch (t: Throwable) {
            logger.e(TAG, "brain.respond failed", t)
            BrainReply(text = fallback(detected), language = "hi")
        }
    }

    private fun fallback(lang: LanguageDetector.Lang): String =
        if (lang == LanguageDetector.Lang.EN)
            "Sir, I'm having trouble thinking clearly right now. Please try again."
        else
            "क्षमा करें सर, मैं अभी उत्तर नहीं दे पा रहा।"

    @Serializable
    data class ContextEnvelope(
        val userText: String,
        val detectedLanguage: String,
        val recentMessages: List<String>,
        val lessons: List<String> = emptyList()
    )

    companion object {
        private const val TAG = "BrainBridge"
    }
}
