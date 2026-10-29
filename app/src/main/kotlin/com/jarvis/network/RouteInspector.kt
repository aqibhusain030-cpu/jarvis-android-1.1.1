package com.jarvis.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RouteSnapshot(
    val interfaceName: String?,
    val routes: List<String>,
    val dnsServers: List<String>
)

@Singleton
class RouteInspector @Inject constructor(@ApplicationContext private val context: Context) {
    fun inspect(): RouteSnapshot {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return RouteSnapshot(null, emptyList(), emptyList())
        val lp: LinkProperties = cm.getLinkProperties(network) ?: return RouteSnapshot(null, emptyList(), emptyList())
        return RouteSnapshot(
            lp.interfaceName,
            lp.routes.map { it.toString() },
            lp.dnsServers.mapNotNull { it.hostAddress }
        )
    }
}
