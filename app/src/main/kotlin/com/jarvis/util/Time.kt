package com.jarvis.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Time {
    fun humanNow(): String = Instant.now()
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy, h:mm a"))
    fun millisNow(): Long = System.currentTimeMillis()
}
