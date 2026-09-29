package com.jarvis.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.jarvis.logging.JarvisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Wraps AudioRecord for 16 kHz mono PCM-16 capture. A single instance is shared
 * by wake-word detection and VAD; callers must not capture concurrently.
 */
@Singleton
class AudioCapture @Inject constructor(private val logger: JarvisLogger) {
    @Volatile private var record: AudioRecord? = null
    @Volatile private var running = false

    val sampleRate = 16_000
    val frameSize = 1280

    @SuppressLint("MissingPermission")
    @Synchronized
    fun start() {
        if (running) return
        val min = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        require(min > 0) { "AudioRecord buffer size unavailable: $min" }
        val bufferSize = min.coerceAtLeast(frameSize * 4)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, bufferSize
        )
        check(rec.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord init failed" }
        runCatching { rec.startRecording() }.onFailure {
            rec.release()
            throw IllegalStateException("AudioRecord start failed", it)
        }
        record = rec
        running = true
        logger.i(TAG, "AudioCapture started (buf=$bufferSize)")
    }

    @Synchronized
    fun readFrame(): ShortArray? {
        val rec = record ?: return null
        val buf = ShortArray(frameSize)
        val n = rec.read(buf, 0, frameSize)
        return when {
            n == frameSize -> buf
            n > 0 -> buf.copyOf(n)
            else -> null
        }
    }

    suspend fun stream(onFrame: suspend (ShortArray) -> Unit) = withContext(Dispatchers.IO) {
        while (coroutineContext.isActive && running) {
            val frame = readFrame()
            if (frame == null) {
                if (!running) break
                kotlinx.coroutines.delay(5)
                continue
            }
            onFrame(frame)
            coroutineContext.ensureActive()
        }
    }

    @Synchronized
    fun stop() {
        running = false
        val current = record
        record = null
        current?.runCatching { stop() }
        current?.runCatching { release() }
    }

    @Synchronized
    fun reset() {
        stop()
        start()
    }

    companion object { private const val TAG = "AudioCapture" }
}
