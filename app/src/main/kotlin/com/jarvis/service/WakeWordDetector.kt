package com.jarvis.service

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.models.WakeWordAssetProvisioner
import java.nio.FloatBuffer
import java.util.ArrayDeque
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * openWakeWord streaming pipeline:
 * PCM-16 -> melspectrogram.onnx -> embedding_model.onnx -> hey_jarvis_v0.1.onnx.
 *
 * AudioCapture supplies 80 ms / 1280-sample frames at 16 kHz, matching the
 * upstream recommendation for efficient, low-latency streaming inference.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    private val audio: AudioCapture,
    private val config: ConfigRepository,
    private val logger: JarvisLogger,
    private val provisioner: WakeWordAssetProvisioner
) {
    private var env: OrtEnvironment? = null
    private var melSession: OrtSession? = null
    private var embeddingSession: OrtSession? = null
    private var classifierSession: OrtSession? = null

    private var melInputName = "input"
    private var embeddingInputName = "input_1"
    private var classifierInputName = "input_1"

    private val rawSamples = ArrayDeque<Short>()
    private val melRows = ArrayDeque<FloatArray>()
    private val embeddings = ArrayDeque<FloatArray>()

    @Volatile private var armed = true

    @Synchronized
    fun load() {
        if (classifierSession != null) return

        val assets = provisioner.ensureInstalled()
        val e = OrtEnvironment.getEnvironment()
        val opts = OrtSession.SessionOptions().apply { setIntraOpNumThreads(1) }

        val mel = e.createSession(assets.mel.absolutePath, opts)
        val embedding = e.createSession(assets.embedding.absolutePath, opts)
        val classifier = e.createSession(assets.classifier.absolutePath, opts)

        melInputName = mel.inputNames.firstOrNull() ?: "input"
        embeddingInputName = embedding.inputNames.firstOrNull() ?: "input_1"
        classifierInputName = classifier.inputNames.firstOrNull() ?: "input_1"

        melSession = mel
        embeddingSession = embedding
        classifierSession = classifier
        env = e

        resetBuffers()
        logger.i(TAG, "Wake-word pipeline loaded: ${assets.classifier.name}")
    }

    fun reset() {
        armed = true
        resetBuffers()
    }

    suspend fun awaitWake() {
        load()
        audio.start()
        while (currentCoroutineContext().isActive) {
            val frame = audio.readFrame()
            if (frame == null) {
                delay(if (audio.isRunning()) 1L else 5L)
                continue
            }
            if (!armed) continue
            if (score(frame) >= config.current.value.wakeThreshold) {
                armed = false
                logger.i(TAG, "Wake word detected")
                return
            }
        }
    }

    /**
     * Process one or more 1280-sample chunks and return the newest classifier score.
     * The classifier is evaluated only after the shared embedding history is populated.
     */
    private fun score(frame: ShortArray): Float {
        val e = env ?: return 0f
        val mel = melSession ?: return 0f
        val embedding = embeddingSession ?: return 0f
        val classifier = classifierSession ?: return 0f

        for (sample in frame) {
            rawSamples.addLast(sample)
        }

        while (rawSamples.size > RAW_CONTEXT_SAMPLES) {
            rawSamples.removeFirst()
        }
        if (rawSamples.size < MIN_RAW_SAMPLES) return 0f

        val melInput = FloatArray(rawSamples.size)
        var index = 0
        for (sample in rawSamples) {
            melInput[index++] = sample.toFloat()
        }

        val melData = runFloatModel(
            session = mel,
            inputName = melInputName,
            input = melInput,
            shape = longArrayOf(1, melInput.size.toLong()),
            env = e
        ) ?: return 0f

        // openWakeWord's exported mel model emits 32 mel bins. Appending rows
        // preserves the streaming feature history expected by the embedding model.
        require(melData.size >= MEL_BINS && melData.size % MEL_BINS == 0) {
            "Unexpected melspectrogram output size: ${melData.size}"
        }

        val melFrames = melData.size / MEL_BINS
        for (rowIndex in 0 until melFrames) {
            val row = FloatArray(MEL_BINS)
            for (bin in 0 until MEL_BINS) {
                row[bin] = melData[rowIndex * MEL_BINS + bin] / 10f + 2f
            }
            melRows.addLast(row)
        }
        while (melRows.size > MEL_CONTEXT_FRAMES) melRows.removeFirst()

        if (melRows.size < MEL_CONTEXT_FRAMES) return 0f

        val melWindow = FloatArray(MEL_CONTEXT_FRAMES * MEL_BINS)
        var offset = 0
        for (row in melRows) {
            row.copyInto(melWindow, offset)
            offset += row.size
        }

        val embData = runFloatModel(
            session = embedding,
            inputName = embeddingInputName,
            input = melWindow,
            shape = longArrayOf(1, MEL_CONTEXT_FRAMES.toLong(), MEL_BINS.toLong(), 1),
            env = e
        ) ?: return 0f

        if (embData.isEmpty()) return 0f
        val embeddingVector = FloatArray(EMBEDDING_SIZE)
        for (i in embeddingVector.indices) {
            embeddingVector[i] = embData.getOrElse(i) { 0f }
        }
        embeddings.addLast(embeddingVector)

        while (embeddings.size > CLASSIFIER_FRAMES + 4) embeddings.removeFirst()
        if (embeddings.size < CLASSIFIER_FRAMES) return 0f

        val classifierInput = FloatArray(CLASSIFIER_FRAMES * EMBEDDING_SIZE)
        val recent = embeddings.toList().takeLast(CLASSIFIER_FRAMES)
        offset = 0
        for (vector in recent) {
            vector.copyInto(classifierInput, offset)
            offset += vector.size
        }

        val result = runFloatModel(
            session = classifier,
            inputName = classifierInputName,
            input = classifierInput,
            shape = longArrayOf(1, CLASSIFIER_FRAMES.toLong(), EMBEDDING_SIZE.toLong()),
            env = e
        ) ?: return 0f

        return result.firstOrNull()?.coerceIn(0f, 1f) ?: 0f
    }

    private fun runFloatModel(
        session: OrtSession,
        inputName: String,
        input: FloatArray,
        shape: LongArray,
        env: OrtEnvironment
    ): FloatArray? {
        return runCatching {
            OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
                session.run(mapOf(inputName to tensor)).use { output ->
                    flattenFloats(output[0].value)
                }
            }
        }.onFailure { logger.w(TAG, "Wake-word inference failed: ${it.message}") }.getOrNull()
    }

    private fun flattenFloats(value: Any?): FloatArray {
        val out = ArrayList<Float>()
        fun visit(v: Any?) {
            when (v) {
                is FloatArray -> v.forEach(out::add)
                is DoubleArray -> v.forEach { out.add(it.toFloat()) }
                is IntArray -> v.forEach { out.add(it.toFloat()) }
                is LongArray -> v.forEach { out.add(it.toFloat()) }
                is Number -> out.add(v.toFloat())
                is Array<*> -> v.forEach(::visit)
                is List<*> -> v.forEach(::visit)
            }
        }
        visit(value)
        return out.toFloatArray()
    }

    private fun resetBuffers() {
        rawSamples.clear()
        melRows.clear()
        embeddings.clear()
        // Upstream keeps a short warm-up period before trusting classifier output.
        repeat(MEL_CONTEXT_FRAMES) { melRows.addLast(FloatArray(MEL_BINS) { 1f }) }
    }

    @Synchronized
    fun close() {
        runCatching { classifierSession?.close() }
        runCatching { embeddingSession?.close() }
        runCatching { melSession?.close() }
        classifierSession = null
        embeddingSession = null
        melSession = null
        env = null
        resetBuffers()
    }

    companion object {
        private const val TAG = "WakeWord"
        private const val RAW_CONTEXT_SAMPLES = 1760
        private const val MIN_RAW_SAMPLES = 1280
        private const val MEL_BINS = 32
        private const val MEL_CONTEXT_FRAMES = 76
        private const val EMBEDDING_SIZE = 96
        private const val CLASSIFIER_FRAMES = 16
    }
}
