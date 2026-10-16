package com.example.money.data.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class NsdPeerDiscovery(
    private val context: Context,
    private val localServiceName: String,
    private val onPeerResolved: (PeerEndpoint) -> Unit,
    private val onPeerLost: (String) -> Unit,
) : PeerDiscovery {
    data class PeerEndpoint(
        val serviceName: String,
        val host: String,
        val port: Int,
    )

    private val nsdManager = context.getSystemService(NsdManager::class.java)

    private fun peerHost(resolved: NsdServiceInfo): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return resolved.hostAddresses.firstOrNull()?.hostAddress
        }
        @Suppress("DEPRECATION")
        return resolved.host?.hostAddress
    }
    private val registered = AtomicBoolean(false)
    private val discovering = AtomicBoolean(false)
    private val serviceInfoCallbacks = ConcurrentHashMap<String, Any>()

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
            resolveFoundService(serviceInfo)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            unregisterServiceInfo(serviceInfo.serviceName)
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

    override fun registerService(port: Int) {
        if (!registered.compareAndSet(false, true)) return
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = localServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    override fun startDiscovery() {
        if (!discovering.compareAndSet(false, true)) return
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    override fun stop() {
        unregisterAllServiceInfo()
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

    private fun publishResolved(resolved: NsdServiceInfo) {
        if (resolved.serviceName == localServiceName) return
        val host = peerHost(resolved) ?: return
        onPeerResolved(
            PeerEndpoint(
                serviceName = resolved.serviceName,
                host = host,
                port = resolved.port,
            ),
        )
    }

    private fun resolveFoundService(serviceInfo: NsdServiceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerServiceInfo(serviceInfo)
            return
        }
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(failed: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD resolve failed for ${failed.serviceName}: $errorCode")
            }

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                publishResolved(resolved)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(serviceInfo, context.mainExecutor, listener)
        } else {
            @Suppress("DEPRECATION")
            nsdManager.resolveService(serviceInfo, listener)
        }
    }

    private fun registerServiceInfo(serviceInfo: NsdServiceInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        if (serviceInfoCallbacks.containsKey(serviceInfo.serviceName)) return
        val callback = object : NsdManager.ServiceInfoCallback {
            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                serviceInfoCallbacks.remove(serviceInfo.serviceName)
                Log.w(TAG, "NSD service info failed for ${serviceInfo.serviceName}: $errorCode")
            }

            override fun onServiceUpdated(updated: NsdServiceInfo) {
                if (peerHost(updated) == null) return
                publishResolved(updated)
                unregisterServiceInfo(updated.serviceName)
            }

            override fun onServiceLost() {
                val name = serviceInfo.serviceName
                serviceInfoCallbacks.remove(name)
                onPeerLost(name)
            }

            override fun onServiceInfoCallbackUnregistered() {
                serviceInfoCallbacks.remove(serviceInfo.serviceName)
            }
        }
        serviceInfoCallbacks[serviceInfo.serviceName] = callback
        nsdManager.registerServiceInfoCallback(serviceInfo, context.mainExecutor, callback)
    }

    private fun unregisterServiceInfo(serviceName: String) {
        unregisterResolvedServiceInfo(serviceName)
    }

    private fun unregisterResolvedServiceInfo(serviceName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val callback = serviceInfoCallbacks.remove(serviceName) as? NsdManager.ServiceInfoCallback ?: return
        try {
            nsdManager.unregisterServiceInfoCallback(callback)
        } catch (exception: Exception) {
            Log.w(TAG, "Failed to unregister NSD service info callback", exception)
        }
    }

    private fun unregisterAllServiceInfo() {
        serviceInfoCallbacks.keys.toList().forEach(::unregisterServiceInfo)
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
