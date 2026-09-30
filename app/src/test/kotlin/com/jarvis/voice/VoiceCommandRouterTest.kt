package com.jarvis.voice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VoiceCommandRouterTest {
    @Test
    fun resolves_common_app_launch_commands() {
        assertThat(VoiceCommandRouter.resolve("open WhatsApp"))
            .isEqualTo(VoiceRoute("open_app", mapOf("target" to "whatsapp")))
        assertThat(VoiceCommandRouter.resolve("WhatsApp kholo"))
            .isEqualTo(VoiceRoute("open_app", mapOf("target" to "whatsapp")))
    }

    @Test
    fun resolves_live_news_commands() {
        assertThat(VoiceCommandRouter.resolve("latest news"))
            .isEqualTo(VoiceRoute("get_news"))
        assertThat(VoiceCommandRouter.resolve("latest news about artificial intelligence"))
            .isEqualTo(
                VoiceRoute(
                    "get_news",
                    mapOf("query" to "artificial intelligence")
                )
            )
        assertThat(VoiceCommandRouter.resolve("India ki latest news"))
            .isEqualTo(VoiceRoute("get_news", mapOf("query" to "india")))
    }

    @Test
    fun resolves_bounded_android_navigation() {
        assertThat(VoiceCommandRouter.resolve("go home"))
            .isEqualTo(VoiceRoute("device_navigation", mapOf("action" to "home")))
        assertThat(VoiceCommandRouter.resolve("open recent apps"))
            .isEqualTo(VoiceRoute("device_navigation", mapOf("action" to "recents")))
    }
}
