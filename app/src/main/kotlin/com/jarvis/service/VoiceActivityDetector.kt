package com.jarvis.service

import com.jarvis.config.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Energy-based utterance VAD for 16 kHz mono PCM-16.
 *
 * Keeps a short pre-roll so the first consonant is not clipped while the
 * energy gate opens. Hysteresis separates speech from room noise; a minimum
 * voiced duration rejects clicks and handling noise. Silence and maximum
 * duration are configurable, and callers own AudioCapture.stop().
 */
@Singleton
class VoiceActivityDetector @Inject constructor(
    private val audio: AudioCapture,
    private val config: ConfigRepository
) {
    private val openThreshold = 500.0
    private val closeThreshold = 250.0

    suspend fun captureUtterance(capture: AudioCapture): ByteArray = withContext(Dispatchers.IO) {
        capture.start()
        val out = ByteArrayOutputStream()
        val preRoll = ArrayDeque<ShortArray>()
        val preRollFrames = 3 // 240 ms at 80 ms per frame
        val silenceFramesRequired = (config.current.value.vadSilenceMs / 80).coerceAtLeast(5)
        val maxFrames = (config.current.value.maxCommandSeconds * 1000 / 80).coerceAtLeast(1)
        var silenceFrames = 0
        var voicedFrames = 0
        var voiced = false
        var frames = 0

        while (frames < maxFrames) {
            val frame = capture.readFrame() ?: continue
            frames++
            val level = rms(frame)

            if (!voiced) {
                preRoll.addLast(frame.copyOf())
                while (preRoll.size > preRollFrames) preRoll.removeFirst()
                if (level > openThreshold) {
                    voiced = true
                    while (preRoll.isNotEmpty()) appendPcm(out, preRoll.removeFirst())
                    voicedFrames++
                }
                continue
            }

            appendPcm(out, frame)
            if (level >= closeThreshold) {
                voicedFrames++
                silenceFrames = 0
            } else {
                silenceFrames++
                if (silenceFrames >= silenceFramesRequired) break
            }
        }

        if (voicedFrames < 2) ByteArray(0) else out.toByteArray()
    }

    private fun appendPcm(out: ByteArrayOutputStream, frame: ShortArray) {
        val bytes = ByteArray(frame.size * 2)
        for (i in frame.indices) {
            val value = frame[i].toInt()
            bytes[i * 2] = (value and 0xFF).toByte()
            bytes[i * 2 + 1] = ((value ushr 8) and 0xFF).toByte()
        }
        out.write(bytes)
    }

    private fun rms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var accumulated = 0.0
        for (sample in samples) accumulated += sample.toDouble() * sample
        return sqrt(accumulated / samples.size)
    }
}
