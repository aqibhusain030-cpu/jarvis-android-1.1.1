package com.jarvis.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS façade with per-sentence streaming for low latency. Uses Android
 * TextToSpeech with hi-IN / en-IN voices. Interruptible by [stop].
 */
@Singleton
class TextToSpeechBridge @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val config: ConfigRepository,
    private val logger: JarvisLogger
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var utteranceSeq = 0L
    private val completions = mutableMapOf<String, CompletableDeferred<Unit>>()

    fun init() {
        if (tts != null) return
        tts = TextToSpeech(ctx, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) { logger.e(TAG, "TTS init failed"); return }
        val t = tts ?: return
        t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                id?.let { completions.remove(it)?.complete(Unit) }
            }
            @Deprecated("deprecated") override fun onError(id: String?) {
                id?.let { completions.remove(it)?.complete(Unit) }
            }
            override fun onError(id: String?, errorCode: Int) {
                id?.let { completions.remove(it)?.complete(Unit) }
            }
        })
    }

    suspend fun speak(text: String) {
        if (text.isBlank()) return
        init()
        waitReady()
        val locale = pickLocale(text)
        tts?.language = locale
        tts?.setSpeechRate(config.current.value.ttsSpeed)
        tts?.setPitch(config.current.value.ttsPitch)
        for (sentence in splitSentences(text)) {
            val id = "u${utteranceSeq++}"
            val done = CompletableDeferred<Unit>()
            completions[id] = done
            tts?.speak(sentence, TextToSpeech.QUEUE_ADD, Bundle(), id)
            done.await()
        }
    }

    fun stop() {
        tts?.stop()
        completions.values.forEach { it.complete(Unit) }
        completions.clear()
    }

    fun shutdown() { runCatching { tts?.shutdown() }; tts = null; ready = false }

    private suspend fun waitReady() {
        var i = 0
        while (!ready && i++ < 50) delay(100)
    }

    private fun pickLocale(text: String): Locale {
        val devanagari = text.count { it.code in 0x0900..0x097F }
        return if (devanagari > text.length / 10) Locale("hi", "IN")
        else Locale("en", "IN")
    }

    private fun splitSentences(text: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (c in text) {
            sb.append(c)
            if (c == '.' || c == '!' || c == '?' || c == '।' || c == '\n') {
                val s = sb.toString().trim()
                if (s.isNotEmpty()) out += s
                sb.clear()
            }
        }
        val tail = sb.toString().trim()
        if (tail.isNotEmpty()) out += tail
        return if (out.isEmpty()) listOf(text) else out
    }
    companion object { private const val TAG = "TTS" }
}
