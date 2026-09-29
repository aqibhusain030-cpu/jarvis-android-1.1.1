package com.jarvis.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-utterance language classifier for Hindi / English / Hinglish.
 * Rules-based to avoid a model dependency; deliberately conservative.
 */
@Singleton
class LanguageDetector @Inject constructor() {

    enum class Lang { HI, EN, HINGLISH }

    fun detect(text: String): Lang {
        if (text.isBlank()) return Lang.HI
        val dev = text.count { it.code in 0x0900..0x097F }
        val latin = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val total = (dev + latin).coerceAtLeast(1)
        val devRatio = dev.toDouble() / total
        return when {
            dev == 0 && latin > 0 -> Lang.EN
            devRatio > 0.85 -> Lang.HI
            devRatio in 0.10..0.60 && latin > 0 -> Lang.HINGLISH
            devRatio > 0.5 -> Lang.HI
            else -> Lang.EN
        }
    }

    fun explicitOverride(text: String): Lang? {
        val l = text.lowercase()
        return when {
            "reply in english" in l || "अंग्रेज़ी में जवाब" in text -> Lang.EN
            "reply in hindi" in l || "हिंदी में जवाब" in text || "हिन्दी में जवाब" in text -> Lang.HI
            else -> null
        }
    }
}
