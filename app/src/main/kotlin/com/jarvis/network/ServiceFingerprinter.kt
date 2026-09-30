package com.jarvis.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceFingerprinter @Inject constructor() {
    fun identify(port: Int): String = when (port) {
        22 -> "ssh"; 25, 465, 587 -> "smtp"; 53 -> "dns"
        80, 8080, 8000, 8008 -> "http"; 110 -> "pop3"; 143 -> "imap"
        443, 8443 -> "https"; 554 -> "rtsp"; 3306 -> "mysql"
        5432 -> "postgresql"; 6379 -> "redis"; 8081 -> "http-alt"; else -> "unknown"
    }
}
