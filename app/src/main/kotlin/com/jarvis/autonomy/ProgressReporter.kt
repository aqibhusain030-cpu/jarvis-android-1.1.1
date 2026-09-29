package com.jarvis.autonomy

import com.jarvis.voice.TextToSpeechBridge
import javax.inject.Inject
import javax.inject.Singleton

/** Emits progress updates for long-running goals. */
@Singleton
class ProgressReporter @Inject constructor(private val tts: TextToSpeechBridge) {
    @Volatile private var lastReport = 0L

    suspend fun maybeReport(goal: Goal, stepIndex: Int, stepsTotal: Int) {
        val now = System.currentTimeMillis()
        val should = stepIndex == stepsTotal || (now - lastReport) > 120_000
        if (!should) return
        lastReport = now
        val msg = when (goal.language) {
            "en" -> "Working on ${goal.title}, step $stepIndex of $stepsTotal."
            else -> "${goal.title} पर काम कर रहा हूँ, चरण $stepIndex / $stepsTotal।"
        }
        tts.speak(msg)
    }
}
