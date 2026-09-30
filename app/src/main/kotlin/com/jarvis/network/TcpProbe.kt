package com.jarvis.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpProbe @Inject constructor() {
    suspend fun probe(host: String, port: Int, timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        require(port in 1..65535)
        require(timeoutMs in 100..5000)
        Socket().use { socket -> runCatching { socket.connect(InetSocketAddress(host, port), timeoutMs); true }.getOrDefault(false) }
    }
}
