package com.jarvis.network

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NetworkBoundsTest {
    @Test
    fun serviceFingerprinterIsDeterministic() {
        val f = ServiceFingerprinter()
        assertThat(f.identify(443)).isEqualTo("https")
        assertThat(f.identify(22)).isEqualTo("ssh")
        assertThat(f.identify(31337)).isEqualTo("unknown")
    }
}
