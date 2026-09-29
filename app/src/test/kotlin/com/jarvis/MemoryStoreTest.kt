package com.jarvis

import com.google.common.truth.Truth.assertThat
import com.jarvis.memory.MemoryEntity
import org.junit.Test

class MemoryStoreTest {
    @Test fun memoryEntityEquality() {
        val a = MemoryEntity("k", "v", "fact")
        val b = MemoryEntity("k", "v", "fact")
        assertThat(a).isEqualTo(b)
    }
    @Test fun memoryEntityFieldsPersist() {
        val e = MemoryEntity("home", "lat,lng", "place", language = "hi")
        assertThat(e.key).isEqualTo("home")
        assertThat(e.category).isEqualTo("place")
        assertThat(e.language).isEqualTo("hi")
    }
}
