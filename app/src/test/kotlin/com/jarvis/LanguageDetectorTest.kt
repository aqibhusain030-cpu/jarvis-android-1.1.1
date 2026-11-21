package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.voice.LanguageDetector
import org.junit.Test

class LanguageDetectorTest {
    private val d = LanguageDetector()

    @Test fun pureEnglish() {
        assertThat(d.detect("Set a timer for five minutes")).isEqualTo(LanguageDetector.Lang.EN)
    }
    @Test fun pureHindi() {
        assertThat(d.detect("पाँच मिनट का टाइमर लगाओ")).isEqualTo(LanguageDetector.Lang.HI)
    }
    @Test fun hinglishIsDetected() {
        assertThat(d.detect("मेरा फ़ोन silent कर do")).isEqualTo(LanguageDetector.Lang.HINGLISH)
    }
    @Test fun pureArabic() {
        assertThat(d.detect("اضبط مؤقتاً لمدة خمس دقائق")).isEqualTo(LanguageDetector.Lang.AR)
    }
    @Test fun arabicWithEnglishCodeSwitching() {
        assertThat(d.detect("افتح YouTube من فضلك")).isEqualTo(LanguageDetector.Lang.AR)
    }
    @Test fun englishOverride() {
        assertThat(d.explicitOverride("Reply in English please")).isEqualTo(LanguageDetector.Lang.EN)
    }
    @Test fun arabicOverride() {
        assertThat(d.explicitOverride("Please reply in Arabic")).isEqualTo(LanguageDetector.Lang.AR)
    }
    @Test fun hindiOverride() {
        assertThat(d.explicitOverride("अंग्रेज़ी में जवाब दो")).isEqualTo(LanguageDetector.Lang.EN)
    }
    @Test fun emptyDefaultsToHindi() {
        assertThat(d.detect("")).isEqualTo(LanguageDetector.Lang.HI)
    }
}
