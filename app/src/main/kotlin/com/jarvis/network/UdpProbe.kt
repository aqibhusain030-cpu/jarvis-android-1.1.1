package com.jarvis.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UdpProbe @Inject constructor() {
    suspend fun probe(host: String, port: Int, timeoutMs: Int = 1500): Boolean = withContext(Dispatchers.IO) {
        require(port in 1..65535)
        require(timeoutMs in 100..5000)
        DatagramSocket().use { socket ->
            socket.soTimeout = timeoutMs
            val address = InetAddress.getByName(host)
            val packet = DatagramPacket(ByteArray(0), 0, address, port)
            runCatching {
                socket.send(packet)
                true
            }.getOrDefault(false)
        }
    }
}
