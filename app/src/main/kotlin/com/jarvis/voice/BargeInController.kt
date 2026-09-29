package com.jarvis.voice

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates barge-in: while JARVIS is speaking, if the user starts talking
 * the TTS is stopped. Actual acoustic detection reuses the VAD; this class
 * just owns the state machine.
 */
@Singleton
class BargeInController @Inject constructor(
    private val tts: TextToSpeechBridge
) {
    @Volatile private var armed = false
    fun armBargeIn() { armed = true }
    fun disarm() { armed = false }
    fun onUserSpeech() { if (armed) { tts.stop(); disarm() } }
}
