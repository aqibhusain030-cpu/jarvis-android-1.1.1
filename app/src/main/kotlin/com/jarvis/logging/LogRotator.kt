package com.jarvis.logging

import javax.inject.Inject
import javax.inject.Singleton

/** Explicit daily rotator for logs that must survive logger restarts. */
@Singleton
class LogRotator @Inject constructor(private val logger: JarvisLogger) {
    fun rotateAll() { listOf("logs", "audit").forEach { logger.dir(it) } }
}
