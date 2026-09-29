package com.jarvis.service

import com.jarvis.config.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Energy-based VAD. Opens the mic, buffers frames until the configured silence
 * threshold is reached, and returns raw 16 kHz mono PCM-16 as a byte array.
 * RMS gate with hysteresis; Silero-ONNX-upgradeable without changing callers.
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
        val silenceFramesRequired = (config.current.value.vadSilenceMs / 80).coerceAtLeast(4)
        var silence = 0
        var voiced = false
        val maxFrames = (config.current.value.maxCommandSeconds * 1000 / 80)
        var frames = 0
        while (frames < maxFrames) {
            val frame = capture.readFrame() ?: continue
            frames++
            val rms = rms(frame)
            if (!voiced && rms > openThreshold) voiced = true
            if (voiced) appendPcm(out, frame)
            if (voiced && rms < closeThreshold) {
                silence++
                if (silence >= silenceFramesRequired) break
            } else silence = 0
        }
        out.toByteArray()
    }

    private fun appendPcm(out: ByteArrayOutputStream, frame: ShortArray) {
        val b = ByteArray(frame.size * 2)
        for (i in frame.indices) {
            val v = frame[i].toInt()
            b[i * 2] = (v and 0xFF).toByte()
            b[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        out.write(b)
    }

    private fun rms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var acc = 0.0
        for (s in samples) acc += s.toDouble() * s
        return sqrt(acc / samples.size)
    }
}
