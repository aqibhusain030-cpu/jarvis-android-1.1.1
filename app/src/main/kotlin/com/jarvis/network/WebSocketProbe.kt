package com.jarvis.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class WebSocketProbeResult(val connected: Boolean, val code: Int?, val message: String?)

@Singleton
class WebSocketProbe @Inject constructor() {
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()

    suspend fun probe(url: String): WebSocketProbeResult = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(url).build()
        var socket: WebSocket? = null
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                if (continuation.isActive) continuation.resume(WebSocketProbeResult(true, response.code, response.message))
                webSocket.close(1000, "JARVIS probe complete")
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                if (continuation.isActive) continuation.resume(WebSocketProbeResult(false, response?.code, t.message))
            }
        })
        continuation.invokeOnCancellation { socket?.cancel() }
    }
}
