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
    fun resolves_direct_device_and_media_controls() {
        assertThat(VoiceCommandRouter.resolve("turn flashlight on"))
            .isEqualTo(VoiceRoute("torch", mapOf("state" to "on")))
        assertThat(VoiceCommandRouter.resolve("pause music"))
            .isEqualTo(VoiceRoute("media_control", mapOf("action" to "pause")))
        assertThat(VoiceCommandRouter.resolve("next song"))
            .isEqualTo(VoiceRoute("media_control", mapOf("action" to "next")))
        assertThat(VoiceCommandRouter.resolve("set volume to 75 percent"))
            .isEqualTo(VoiceRoute("set_volume", mapOf("stream" to "music", "level" to 75)))
    }

    @Test
    fun resolves_bounded_android_navigation() {
        assertThat(VoiceCommandRouter.resolve("go home"))
            .isEqualTo(VoiceRoute("device_navigation", mapOf("action" to "home")))
        assertThat(VoiceCommandRouter.resolve("open recent apps"))
            .isEqualTo(VoiceRoute("device_navigation", mapOf("action" to "recents")))
    }
}
