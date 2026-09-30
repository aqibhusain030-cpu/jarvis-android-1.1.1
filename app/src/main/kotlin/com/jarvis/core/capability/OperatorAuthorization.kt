package com.jarvis.core.capability

interface OperatorAuthorization {
    /**
     * Atomically consumes one authenticated operator authorization.
     * Returns true only when a fresh authorization is available.
     */
    fun consume(): Boolean
}
