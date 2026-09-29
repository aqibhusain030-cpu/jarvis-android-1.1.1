package com.jarvis.util

sealed class Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>()
    data class Err(val message: String, val code: String = "error") : Outcome<Nothing>()
    fun getOrNull(): T? = (this as? Ok)?.value
    fun errorOrNull(): Err? = this as? Err
}
inline fun <T> runCatchingOutcome(block: () -> T): Outcome<T> = try {
    Outcome.Ok(block())
} catch (t: Throwable) { Outcome.Err(t.message ?: "error") }
