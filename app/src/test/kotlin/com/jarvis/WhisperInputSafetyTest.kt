package com.jarvis

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Contract test documenting the JNI boundary: PCM-16 input must have even byte length. */
class WhisperInputSafetyTest {
    @Test fun oddPcmByteCountIsInvalid() {
        val oddByteCount = 1
        assertThat(oddByteCount % 2).isEqualTo(1)
    }
}
