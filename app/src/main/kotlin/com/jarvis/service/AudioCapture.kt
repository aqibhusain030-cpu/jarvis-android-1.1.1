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
 * Wraps [AudioRecord] for 16 kHz mono PCM-16 capture. Single instance shared by
 * the wake-word detector and the VAD; access is serialized by the caller.
 */
@Singleton
class AudioCapture @Inject constructor(private val logger: JarvisLogger) {
    @Volatile private var record: AudioRecord? = null
    @Volatile private var running = false

    val sampleRate = 16_000
    val frameSize = 1280

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        val min = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(frameSize * 4)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, min
        )
        check(rec.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord init failed" }
        rec.startRecording()
        record = rec; running = true
        logger.i(TAG, "AudioCapture started (buf=$min)")
    }

    fun readFrame(): ShortArray? {
        val rec = record ?: return null
        val buf = ShortArray(frameSize)
        val n = rec.read(buf, 0, frameSize)
        return if (n > 0) buf else null
    }

    suspend fun stream(onFrame: suspend (ShortArray) -> Unit) = withContext(Dispatchers.IO) {
        while (coroutineContext.isActive && running) {
            val f = readFrame() ?: continue
            onFrame(f); coroutineContext.ensureActive()
        }
    }

    fun stop() {
        running = false
        record?.runCatching { stop() }
        record?.runCatching { release() }
        record = null
    }
    fun reset() { stop(); start() }
    companion object { private const val TAG = "AudioCapture" }
}
