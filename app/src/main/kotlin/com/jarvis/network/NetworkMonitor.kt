package com.jarvis.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class NetworkState(
    val available: Boolean,
    val transport: String?,
    val validated: Boolean,
    val metered: Boolean,
    val interfaceName: String?,
    val dnsServers: List<String>
)

@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile private var state = NetworkState(false, null, false, true, null, emptyList())
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun current(): NetworkState = state

    @Synchronized
    fun start() {
        if (callback != null) return
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                update(cm, network, null)
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                update(cm, network, capabilities)
            }
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                state = state.copy(
                    interfaceName = linkProperties.interfaceName,
                    dnsServers = linkProperties.dnsServers.map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
                )
            }
            override fun onLost(network: Network) {
                state = NetworkState(false, null, false, true, null, emptyList())
            }
        }
        cm.registerDefaultNetworkCallback(cb)
        callback = cb
    }

    @Synchronized
    fun stop() {
        val cb = callback ?: return
        context.getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(cb)
        callback = null
    }

    private fun update(cm: ConnectivityManager, network: Network, capabilities: NetworkCapabilities?) {
        val caps = capabilities ?: cm.getNetworkCapabilities(network) ?: return
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "other"
        }
        state = state.copy(
            available = true,
            transport = transport,
            validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        )
    }
}
