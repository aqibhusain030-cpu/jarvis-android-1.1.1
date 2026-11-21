package com.jarvis.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.service.AudioCapture
import com.jarvis.service.VoiceActivityDetector
import com.jarvis.voice.LanguageDetector
import com.jarvis.voice.SpeechRecognizerBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TranscriptionState(
    val listening: Boolean = false,
    val transcript: String = "",
    val language: String = "",
    val error: String = ""
)

/**
 * Direct offline ASR test path. It records with Android AudioRecord, segments
 * one complete utterance with local VAD, then calls whisper.cpp JNI directly.
 * It never invokes Android SpeechRecognizer, keyboard dictation, the cloud,
 * the LLM, or TTS.
 */
@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val audio: AudioCapture,
    private val vad: VoiceActivityDetector,
    private val recognizer: SpeechRecognizerBridge,
    private val languageDetector: LanguageDetector
) : ViewModel() {
    private val mutableState = MutableStateFlow(TranscriptionState())
    val state: StateFlow<TranscriptionState> = mutableState.asStateFlow()
    private var job: Job? = null

    fun setError(message: String) {
        mutableState.value = mutableState.value.copy(error = message)
    }

    fun transcribe() {
        if (job?.isActive == true || mutableState.value.listening) return
        job = viewModelScope.launch {
            mutableState.value = TranscriptionState(listening = true)
            try {
                check(!audio.isRunning()) {
                    "Another JARVIS microphone session is active. Stop it before using the transcription test."
                }
                val pcm = withContext(Dispatchers.IO) { vad.captureUtterance(audio) }
                if (pcm.isEmpty()) {
                    mutableState.value = TranscriptionState(
                        error = "No clear speech was detected. Move closer to the microphone and try again."
                    )
                    return@launch
                }
                val text = withContext(Dispatchers.Default) { recognizer.transcribe(pcm).trim() }
                if (text.isBlank()) {
                    mutableState.value = TranscriptionState(
                        error = "Whisper returned no transcript. Check that the verified ggml-small.bin model is installed."
                    )
                    return@launch
                }
                val detected = languageDetector.detect(text)
                val languageName = when (detected) {
                    LanguageDetector.Lang.HI -> "Hindi"
                    LanguageDetector.Lang.EN -> "English"
                    LanguageDetector.Lang.HINGLISH -> "Hindi + English"
                    LanguageDetector.Lang.AR -> "Arabic"
                }
                mutableState.value = TranscriptionState(
                    transcript = text,
                    language = languageName
                )
            } catch (t: Throwable) {
                mutableState.value = TranscriptionState(
                    error = t.message?.take(300) ?: "Transcription failed."
                )
            } finally {
                runCatching { audio.stop() }
                if (mutableState.value.listening) {
                    mutableState.value = mutableState.value.copy(listening = false)
                }
            }
        }
    }

    fun clear() {
        if (job?.isActive != true) mutableState.value = TranscriptionState()
    }

    override fun onCleared() {
        runCatching { audio.stop() }
        super.onCleared()
    }
}
