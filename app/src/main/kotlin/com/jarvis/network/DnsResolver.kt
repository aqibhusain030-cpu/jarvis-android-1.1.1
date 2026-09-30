package com.jarvis.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DnsResolver @Inject constructor() {
    suspend fun resolve(host: String): List<String> = withContext(Dispatchers.IO) {
        require(host.length in 1..253)
        InetAddress.getAllByName(host).map { it.hostAddress }.distinct().take(32)
    }
}
