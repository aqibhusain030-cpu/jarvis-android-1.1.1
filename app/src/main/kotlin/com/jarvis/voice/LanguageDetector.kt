package com.jarvis.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Script-aware language classification for Hindi, English, Hinglish and Arabic.
 * Whisper performs acoustic language detection; this class classifies its final
 * transcript so the local brain can answer in the language actually spoken.
 */
@Singleton
class LanguageDetector @Inject constructor() {

    enum class Lang { HI, EN, HINGLISH, AR }

    fun detect(text: String): Lang {
        if (text.isBlank()) return Lang.HI

        var devanagari = 0
        var arabic = 0
        var latin = 0
        for (character in text) {
            val cp = character.code
            when {
                cp in 0x0900..0x097F -> devanagari++
                cp in 0x0600..0x06FF ||
                    cp in 0x0750..0x077F ||
                    cp in 0x08A0..0x08FF ||
                    cp in 0xFB50..0xFDFF ||
                    cp in 0xFE70..0xFEFF ||
                    cp in 0x1EE00..0x1EEFF -> arabic++
                character in 'a'..'z' || character in 'A'..'Z' -> latin++
            }
        }

        // Arabic script is an unambiguous signal even when the speaker code-switches
        // into English words. Whisper itself chooses the transcription language.
        if (arabic > 0) return Lang.AR
        if (devanagari == 0 && latin > 0) return Lang.EN

        val scriptTotal = devanagari + latin
        if (scriptTotal == 0) return Lang.HI
        val devanagariRatio = devanagari.toDouble() / scriptTotal

        return when {
            devanagariRatio > 0.85 -> Lang.HI
            devanagariRatio in 0.10..0.85 && latin > 0 -> Lang.HINGLISH
            devanagariRatio > 0.5 -> Lang.HI
            else -> Lang.EN
        }
    }

    fun explicitOverride(text: String): Lang? {
        val lower = text.lowercase()
        return when {
            "reply in english" in lower || "अंग्रेज़ी में जवाब" in text -> Lang.EN
            "reply in hindi" in lower || "हिंदी में जवाब" in text || "हिन्दी में जवाब" in text -> Lang.HI
            "reply in arabic" in lower || "بالعربية" in text || "رد بالعربية" in text -> Lang.AR
            else -> null
        }
    }
}
