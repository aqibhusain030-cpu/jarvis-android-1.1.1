package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.models.ModelChecksum
import org.junit.Test
import java.io.File

class ModelChecksumTest {
    @Test fun malformedDigestFailsClosed() {
        val f = File.createTempFile("jarvis", ".bin")
        f.writeText("test")
        try { assertThat(ModelChecksum.verify(f, "PLACEHOLDER")).isFalse() }
        finally { f.delete() }
    }

    @Test fun correctDigestPasses() {
        val f = File.createTempFile("jarvis", ".bin")
        f.writeText("test")
        try {
            assertThat(ModelChecksum.verify(f, "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")).isTrue()
        } finally { f.delete() }
    }
}
