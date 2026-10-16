package com.example.money.data.sync

interface PeerDiscovery {
    fun registerService(port: Int)
    fun startDiscovery()
    fun stop()
}

fun interface PeerDiscoveryFactory {
    fun create(
        localServiceName: String,
        onPeerResolved: (NsdPeerDiscovery.PeerEndpoint) -> Unit,
        onPeerLost: (String) -> Unit,
    ): PeerDiscovery
}
