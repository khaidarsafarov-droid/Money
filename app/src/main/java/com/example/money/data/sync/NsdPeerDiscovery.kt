package com.example.money.data.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class NsdPeerDiscovery(
    context: Context,
    private val localServiceName: String,
    private val onPeerResolved: (PeerEndpoint) -> Unit,
    private val onPeerLost: (String) -> Unit,
) {
    data class PeerEndpoint(
        val serviceName: String,
        val host: String,
        val port: Int,
    )

    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private val registered = AtomicBoolean(false)
    private val discovering = AtomicBoolean(false)

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(info: NsdServiceInfo) = Unit

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "NSD registration failed: $errorCode")
        }

        override fun onServiceUnregistered(info: NsdServiceInfo) = Unit

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "NSD unregistration failed: $errorCode")
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(serviceType: String) = Unit

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            if (serviceInfo.serviceName == localServiceName) return
            if (!serviceInfo.serviceName.startsWith(SERVICE_PREFIX)) return
            nsdManager.resolveService(
                serviceInfo,
                object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "NSD resolve failed for ${serviceInfo.serviceName}: $errorCode")
                    }

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        if (resolved.serviceName == localServiceName) return
                        val host = resolved.host?.hostAddress ?: return
                        onPeerResolved(
                            PeerEndpoint(
                                serviceName = resolved.serviceName,
                                host = host,
                                port = resolved.port,
                            ),
                        )
                    }
                },
            )
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            onPeerLost(serviceInfo.serviceName)
        }

        override fun onDiscoveryStopped(serviceType: String) = Unit

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "NSD discovery start failed: $errorCode")
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "NSD discovery stop failed: $errorCode")
        }
    }

    fun registerService(port: Int) {
        if (!registered.compareAndSet(false, true)) return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = localServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun startDiscovery() {
        if (!discovering.compareAndSet(false, true)) return
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stop() {
        if (discovering.compareAndSet(true, false)) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to stop NSD discovery", exception)
            }
        }
        if (registered.compareAndSet(true, false)) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to unregister NSD service", exception)
            }
        }
    }

    companion object {
        private const val TAG = "NsdPeerDiscovery"
        private const val SERVICE_TYPE = "_money._tcp."
        private const val SERVICE_PREFIX = "Money-"

        fun buildServiceName(deviceId: String): String = "$SERVICE_PREFIX$deviceId"

        fun peerDeviceId(serviceName: String): String? {
            if (!serviceName.startsWith(SERVICE_PREFIX)) return null
            return serviceName.removePrefix(SERVICE_PREFIX).takeIf { it.isNotEmpty() }
        }
    }
}
