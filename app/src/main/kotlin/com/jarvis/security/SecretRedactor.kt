package com.jarvis.security

import javax.inject.Inject
import javax.inject.Singleton

/** Redacts obvious secrets from log lines. */
@Singleton
class SecretRedactor @Inject constructor() {
    private val keyVal = Regex("(?i)(api[_-]?key|token|password|passwd|secret)\\s*[=:]\\s*\\S+")
    private val phone = Regex("\\b\\+?\\d{10,15}\\b")
    private val bearer = Regex("(?i)bearer\\s+[A-Za-z0-9._-]+")

    fun redact(s: String): String {
        var out = keyVal.replace(s) { m -> "${m.groupValues[1]}=REDACTED" }
        out = phone.replace(out) { m ->
            m.value.replaceRange(2, (m.value.length - 2).coerceAtLeast(2), "***")
        }
        out = bearer.replace(out, "REDACTED")
        return out
    }
}
