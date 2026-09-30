package com.jarvis.network

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkInventory @Inject constructor() {
    private val assets = ConcurrentHashMap<String, NetworkAsset>()
    fun upsert(asset: NetworkAsset) { assets[asset.assetId] = asset }
    fun get(assetId: String): NetworkAsset? = assets[assetId]
    fun all(scopeId: String): List<NetworkAsset> = assets.values.filter { it.scopeId == scopeId }.sortedBy { it.address ?: it.hostname ?: it.assetId }
    fun recordPort(scopeId: String, host: String, service: NetworkService) {
        val id = scopeId + ":" + host
        val existing = assets[id]
        val now = Instant.now()
        assets[id] = if (existing == null) {
            NetworkAsset(id, host, null, listOf(service), now, now, scopeId)
        } else {
            existing.copy(openServices = (existing.openServices + service).distinctBy { it.transport + ":" + it.port }, lastSeen = now)
        }
    }
}
