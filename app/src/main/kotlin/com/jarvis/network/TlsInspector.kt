package com.jarvis.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.inject.Inject
import javax.inject.Singleton

data class TlsObservation(val host: String, val port: Int, val protocol: String, val cipherSuite: String, val subject: String?, val issuer: String?)

@Singleton
class TlsInspector @Inject constructor() {
    suspend fun inspect(host: String, port: Int, timeoutMs: Int = 3000): TlsObservation = withContext(Dispatchers.IO) {
        require(port in 1..65535)
        require(timeoutMs in 500..5000)
        val socket = (SSLSocketFactory.getDefault() as SSLSocketFactory).createSocket() as SSLSocket
        socket.use {
            it.soTimeout = timeoutMs
            it.connect(InetSocketAddress(host, port), timeoutMs)
            it.startHandshake()
            val cert = it.session.peerCertificates.firstOrNull() as? java.security.cert.X509Certificate
            TlsObservation(host, port, it.session.protocol, it.session.cipherSuite, cert?.subjectX500Principal?.name, cert?.issuerX500Principal?.name)
        }
    }
}
