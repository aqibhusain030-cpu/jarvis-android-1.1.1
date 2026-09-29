package com.jarvis.models

import java.io.File
import java.security.MessageDigest

/**
 * SHA-256 verification. Blank expected digests are a hard failure
 * (fail-closed), never treated as valid.
 */
object ModelChecksum {
    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { ins ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = ins.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * @return true only when [expected] is a non-blank hex digest that matches
     * the file. Blank or malformed expected values always return false.
     */
    fun verify(file: File, expected: String): Boolean {
        if (expected.isBlank()) return false
        val exp = expected.trim().lowercase()
        if (!exp.matches(Regex("[0-9a-f]{64}"))) return false
        if (!file.exists() || !file.isFile) return false
        return sha256(file).equals(exp, ignoreCase = true)
    }
}
