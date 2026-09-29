package com.jarvis.memory

import kotlin.math.sqrt

/**
 * Lightweight local semantic ranking primitive.
 *
 * When a verified embedding model is available, callers can supply vectors.
 * The fallback lexical score keeps memory functional on devices without an
 * embedding model and never sends memory off-device.
 */
object MemorySearch {
    data class Candidate(val key: String, val value: String, val score: Float)

    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size && a.isNotEmpty())
        var dot = 0.0
        var aa = 0.0
        var bb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            aa += a[i] * a[i]
            bb += b[i] * b[i]
        }
        val denom = sqrt(aa * bb)
        return if (denom == 0.0) 0f else (dot / denom).toFloat()
    }

    fun lexical(query: String, value: String): Float {
        val q = query.trim().lowercase().split(Regex("\\s+")).filter { it.length > 1 }.toSet()
        if (q.isEmpty()) return 0f
        val v = value.lowercase()
        return q.count { v.contains(it) }.toFloat() / q.size.toFloat()
    }
}
