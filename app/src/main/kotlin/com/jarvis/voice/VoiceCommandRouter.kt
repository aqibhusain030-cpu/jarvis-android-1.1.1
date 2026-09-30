package com.jarvis.voice

import com.jarvis.tools.ToolDispatcher
import com.jarvis.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class VoiceRoute(
    val tool: String,
    val args: Map<String, Any?> = emptyMap()
)

data class VoiceRouteResult(
    val tool: String,
    val reply: String
)

@Singleton
class VoiceCommandRouter @Inject constructor(
    private val dispatcher: ToolDispatcher
) {
    suspend fun execute(text: String): VoiceRouteResult? {
        val route = resolve(text) ?: return null
        val result = withContext(Dispatchers.Default) {
            dispatcher.invoke(route.tool, route.args)
        }
        return VoiceRouteResult(route.tool, formatReply(route, result))
    }

    private fun formatReply(route: VoiceRoute, result: ToolResult): String = when (result) {
        is ToolResult.Err -> "Sir, ${result.message}"
        is ToolResult.Ok -> {
            if (route.tool == "get_news") formatNews(result.data)
            else result.data?.toString()?.takeIf { it.isNotBlank() } ?: "Sir, done."
        }
    }

    private fun formatNews(data: Any?): String {
        val payload = data as? Map<*, *> ?: return "Sir, the live news feed returned no readable headlines."
        val articles = payload["articles"] as? List<*> ?: return "Sir, there are no matching headlines right now."
        if (articles.isEmpty()) return "Sir, there are no matching headlines right now."

        val lines = articles.take(5).mapNotNull { item ->
            val row = item as? Map<*, *> ?: return@mapNotNull null
            val title = row["title"]?.toString()?.trim().orEmpty()
            val source = row["source"]?.toString()?.trim().orEmpty()
            if (title.isBlank()) null else if (source.isBlank()) title else "$title, from $source"
        }
        return if (lines.isEmpty()) {
            "Sir, the live news feed returned no readable headlines."
        } else {
            "Sir, here are the latest headlines. " + lines.mapIndexed { i, line ->
                "${i + 1}. $line."
            }.joinToString(" ")
        }
    }

    companion object {
        fun resolve(raw: String): VoiceRoute? {
            val text = normalize(raw)
            if (text.isBlank()) return null

            newsRoute(text)?.let { return it }

            when {
                text.matches(Regex("^(?:go )?back$|^peeche jao$|^पीछे जाओ$")) ->
                    return VoiceRoute("device_navigation", mapOf("action" to "back"))
                text.matches(Regex("^(?:go )?home$|^home jao$|^होम जाओ$")) ->
                    return VoiceRoute("device_navigation", mapOf("action" to "home"))
                text.matches(Regex("^(?:open )?(?:recent apps|recents)$|^haal ke apps$|^हाल के ऐप(?:्स)?$")) ->
                    return VoiceRoute("device_navigation", mapOf("action" to "recents"))
                text.matches(Regex("^(?:open )?(?:notifications|notification shade)$|^notifications kholo$|^नोटिफिकेशन खोलो$")) ->
                    return VoiceRoute("device_navigation", mapOf("action" to "notifications"))
                text.matches(Regex("^(?:open )?(?:quick settings)$|^quick settings kholo$|^क्विक सेटिंग्स खोलो$")) ->
                    return VoiceRoute("device_navigation", mapOf("action" to "quick_settings"))
                text.matches(Regex("^(?:take|capture) (?:a )?screenshot$|^screenshot (?:lo|lena)$|^स्क्रीनशॉट लो$")) ->
                    return VoiceRoute("screenshot")
            }

            val app = APP_ENGLISH.matchEntire(text)
                ?: APP_HINGLISH.matchEntire(text)
                ?: APP_HINDI.matchEntire(text)
            if (app != null) {
                val target = cleanTarget(app.groupValues[1])
                if (target.isNotBlank() && target !in setOf("the", "an", "a", "app", "application")) {
                    return VoiceRoute("open_app", mapOf("target" to target))
                }
            }

            return null
        }

        private fun newsRoute(text: String): VoiceRoute? {
            if (HEADLINE_ONLY.matches(text)) {
                return VoiceRoute("get_news")
            }

            val topic = NEWS_ABOUT.matchEntire(text)?.groupValues?.get(1)
                ?: NEWS_HINGLISH.matchEntire(text)?.groupValues?.get(1)
                ?: NEWS_ENGLISH_SUFFIX.matchEntire(text)?.groupValues?.get(1)
                ?: return null

            val cleaned = cleanTopic(topic)
            return if (cleaned.isBlank()) VoiceRoute("get_news")
            else VoiceRoute("get_news", mapOf("query" to cleaned))
        }

        private fun cleanTopic(value: String): String =
            value.replace(Regex("\\b(?:please|sir|latest|today|today's|news|headlines|khabar|khabrein)\\b"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(256)

        private fun cleanTarget(value: String): String =
            value.replace(Regex("^(?:the|my)\\s+"), "")
                .replace(Regex("\\s+(?:app|application)$"), "")
                .trim()
                .take(128)

        private fun normalize(value: String): String =
            value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

        private val HEADLINE_ONLY = Regex(
            "^(?:(?:give|show|tell) me )?(?:the )?(?:latest |today(?:'s )?|breaking )?(?:news|headlines|khabar|khabrein|खबर|खबरें|समाचार)$"
        )
        private val NEWS_ABOUT = Regex(
            "^(?:give|show|tell) (?:me )?(?:the )?(?:latest |today(?:'s )?|breaking )?(?:news|headlines) (?:about|on|for|regarding) (.+)$"
        )
        private val NEWS_HINGLISH = Regex(
            "^(.+?) (?:ki|ke|ka) (?:latest )?(?:news|khabar|khabrein)$"
        )
        private val NEWS_ENGLISH_SUFFIX = Regex(
            "^(.+?) (?:latest )?(?:news|headlines)$"
        )
        private val APP_ENGLISH = Regex(
            "^(?:open|launch|start|run|show) (.+)$"
        )
        private val APP_HINGLISH = Regex(
            "^(.+?) (?:khol|kholo|kholna|open karo)$"
        )
        private val APP_HINDI = Regex(
            "^(.+?) (?:खोलो|खोलना|ओपन करो)$"
        )
    }
}
