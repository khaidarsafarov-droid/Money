package com.example.money.data.sync

import java.net.Inet4Address
import java.net.NetworkInterface

object LocalNetworkAddress {
    fun resolveLanIpv4(): String? {
        return NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { networkInterface -> networkInterface.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress }
            .firstOrNull { address ->
                address != null &&
                    !address.startsWith("127.") &&
                    !address.startsWith("169.254.")
            }
    }

    fun buildWebSocketUrl(host: String?, port: Int): String? {
        if (host.isNullOrBlank() || port <= 0) return null
        return "ws://$host:$port/sync"
    }
}
