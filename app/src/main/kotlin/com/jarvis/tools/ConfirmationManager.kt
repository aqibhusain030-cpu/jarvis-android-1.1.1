package com.jarvis.tools

import com.jarvis.voice.TextToSpeechBridge
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Confirmation gate. Each [ask] creates a fresh deferred bound to a unique
 * request id; completed or timed-out requests are never reused.
 */
@Singleton
class ConfirmationManager @Inject constructor(
    private val tts: TextToSpeechBridge
) {
    private data class Pending(
        val id: String,
        val deferred: CompletableDeferred<Boolean>,
        val createdAt: Long = System.currentTimeMillis()
    )

    private val mutex = Mutex()
    private val current = AtomicReference<Pending?>(null)
    private val acceptingInput = AtomicReference(false)

    private val yes = setOf(
        "yes", "yeah", "yep", "haan", "ha", "हाँ", "हां", "करो",
        "confirm", "ok", "okay", "sure", "बिलकुल"
    )
    private val no = setOf(
        "no", "nah", "nahi", "नहीं", "नही", "cancel", "रद्द", "stop", "रुको"
    )

    /**
     * Speak [question] and wait up to [timeoutMs] for a yes/no answer.
     * Always allocates a new deferred; previous pending requests are cancelled.
     */
    suspend fun ask(question: String, timeoutMs: Long = 20_000): Boolean {
        val req = Pending(
            id = UUID.randomUUID().toString(),
            deferred = CompletableDeferred()
        )
        mutex.withLock {
            current.getAndSet(req)?.deferred?.cancel()
            acceptingInput.set(false)
        }
        try {
            tts.speak(question)
            mutex.withLock {
                if (current.get() === req) acceptingInput.set(true)
            }
            val ans = withTimeoutOrNull(timeoutMs) { req.deferred.await() }
            return ans == true
        } finally {
            // Clear only if this request still owns the active slot.
            mutex.withLock {
                if (current.compareAndSet(req, null)) {
                    acceptingInput.set(false)
                }
            }
        }
    }

    /** Feed recognized speech into the active confirmation, if any. */
    fun submit(userText: String) {
        if (!acceptingInput.get()) return
        val pending = current.get() ?: return
        if (pending.deferred.isCompleted) return
        val first = userText.trim().lowercase(Locale.ROOT)
            .replace(Regex("[,;.!?\u0964\u0965]"), " ")
            .trim().split(Regex("\\s+"))
            .firstOrNull() ?: return
        when {
            first in yes -> pending.deferred.complete(true)
            first in no -> pending.deferred.complete(false)
        }
    }

    /** Cancel any outstanding confirmation without granting it. */
    fun hasPending(): Boolean = acceptingInput.get() && current.get() != null

    fun cancel() {
        acceptingInput.set(false)
        current.getAndSet(null)?.deferred?.complete(false)
    }
}
