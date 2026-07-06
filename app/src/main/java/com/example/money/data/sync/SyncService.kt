package com.example.money.data.sync

import android.content.Context
import android.util.Log
import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.sync.SyncConnectionState
import com.example.money.domain.sync.SyncStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncService(
    private val context: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val financeSnapshotRepository: FinanceSnapshotRepository,
    private val financeSnapshotApplier: FinanceSnapshotApplier,
    private val syncQueueDao: SyncQueueDao,
    private val scope: CoroutineScope,
) {
    private val syncMutex = Mutex()
    private val httpClient = HttpClient(ClientCIO) {
        install(ClientWebSockets)
    }
    private var server: EmbeddedServer<CIOApplicationEngine, *>? = null
    private var nsdDiscovery: NsdPeerDiscovery? = null
    private var queueObserverJob: Job? = null
    private var syncedDecayJob: Job? = null

    private var localDeviceId: String = ""
    private var localServiceName: String = ""
    private val discoveredPeers = linkedMapOf<String, NsdPeerDiscovery.PeerEndpoint>()

    private val _connectionState = MutableStateFlow(SyncConnectionState())
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    suspend fun start() {
        if (server != null) return
        localDeviceId = deviceIdProvider.getDeviceId()
        localServiceName = NsdPeerDiscovery.buildServiceName(localDeviceId)
        startWebSocketServer()
        val port = server?.engine?.resolvedConnectors()?.firstOrNull()?.port ?: return
        val webSocketUrl = LocalNetworkAddress.buildWebSocketUrl(
            LocalNetworkAddress.resolveLanIpv4(),
            port,
        )
        _connectionState.update { it.copy(webSocketUrl = webSocketUrl) }
        nsdDiscovery = NsdPeerDiscovery(
            context = context,
            localServiceName = localServiceName,
            onPeerResolved = { peer -> handlePeerDiscovered(peer) },
            onPeerLost = { serviceName -> handlePeerLost(serviceName) },
        ).also { discovery ->
            discovery.registerService(port)
            discovery.startDiscovery()
        }
        queueObserverJob = scope.launch {
            syncQueueDao.observePendingCount().collect { pendingCount ->
                if (pendingCount > 0) {
                    trySyncWithBestPeer()
                }
            }
        }
        updateStatus(SyncStatus.Discovering)
    }

    fun stop() {
        queueObserverJob?.cancel()
        syncedDecayJob?.cancel()
        nsdDiscovery?.stop()
        nsdDiscovery = null
        server?.stop(gracePeriodMillis = 250, timeoutMillis = 1_000)
        server = null
        httpClient.close()
        discoveredPeers.clear()
        updateStatus(SyncStatus.Offline)
    }

    private fun startWebSocketServer() {
        server = embeddedServer(ServerCIO, host = "0.0.0.0", port = 0) {
            install(ServerWebSockets)
            routing {
                webSocket("/sync") {
                    runAcceptorSession(
                        sendText = { text -> send(Frame.Text(text)) },
                        receiveText = {
                            when (val frame = incoming.receive()) {
                                is Frame.Text -> frame.readText()
                                else -> error("Unexpected frame during sync")
                            }
                        },
                    )
                }
            }
        }.start(wait = false)
    }

    private fun handlePeerDiscovered(peer: NsdPeerDiscovery.PeerEndpoint) {
        discoveredPeers[peer.serviceName] = peer
        _connectionState.update {
            it.copy(
                status = if (it.status == SyncStatus.Syncing) SyncStatus.Syncing else SyncStatus.PeerAvailable,
                peerCount = discoveredPeers.size,
            )
        }
        scope.launch {
            trySyncWithPeer(peer)
        }
    }

    private fun handlePeerLost(serviceName: String) {
        discoveredPeers.remove(serviceName)
        _connectionState.update {
            val nextStatus = when {
                it.status == SyncStatus.Syncing -> SyncStatus.Syncing
                discoveredPeers.isNotEmpty() -> SyncStatus.PeerAvailable
                else -> SyncStatus.Discovering
            }
            it.copy(status = nextStatus, peerCount = discoveredPeers.size)
        }
    }

    private suspend fun trySyncWithBestPeer() {
        val peer = discoveredPeers.values.firstOrNull() ?: return
        trySyncWithPeer(peer)
    }

    private suspend fun trySyncWithPeer(peer: NsdPeerDiscovery.PeerEndpoint) {
        val peerDeviceId = NsdPeerDiscovery.peerDeviceId(peer.serviceName) ?: return
        if (peerDeviceId <= localDeviceId) return
        runInitiatorSession(peer.host, peer.port)
    }

    private suspend fun runInitiatorSession(host: String, port: Int) {
        syncMutex.withLock {
            updateStatus(SyncStatus.Syncing)
            try {
                httpClient.webSocket(host = host, port = port, path = "/sync") {
                    val local = financeSnapshotRepository.buildSnapshot()
                    val merged = SyncPeerSession.runAsInitiator(
                        local = local,
                        encode = financeSnapshotRepository::encodeSnapshot,
                        decode = financeSnapshotRepository::decodeSnapshot,
                        sendText = { text -> send(Frame.Text(text)) },
                        receiveText = {
                            when (val frame = incoming.receive()) {
                                is Frame.Text -> frame.readText()
                                else -> error("Unexpected frame during sync")
                            }
                        },
                    )
                    financeSnapshotApplier.apply(merged)
                    syncQueueDao.clearAll()
                    markSynced()
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Initiator sync failed", exception)
                reportError(exception.message)
            }
        }
    }

    private suspend fun runAcceptorSession(
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ) {
        syncMutex.withLock {
            updateStatus(SyncStatus.Syncing)
            try {
                val local = financeSnapshotRepository.buildSnapshot()
                val merged = SyncPeerSession.runAsAcceptor(
                    local = local,
                    encode = financeSnapshotRepository::encodeSnapshot,
                    decode = financeSnapshotRepository::decodeSnapshot,
                    sendText = sendText,
                    receiveText = receiveText,
                )
                financeSnapshotApplier.apply(merged)
                syncQueueDao.clearAll()
                sendText(SyncPeerSession.ACK)
                markSynced()
            } catch (exception: Exception) {
                Log.w(TAG, "Acceptor sync failed", exception)
                reportError(exception.message)
            }
        }
    }

    private fun markSynced() {
        val now = System.currentTimeMillis()
        _connectionState.update {
            it.copy(
                status = SyncStatus.Synced,
                lastSyncedAt = now,
                lastError = null,
            )
        }
        syncedDecayJob?.cancel()
        syncedDecayJob = scope.launch {
            delay(SYNCED_INDICATOR_MS)
            if (!isActive) return@launch
            _connectionState.update { current ->
                if (current.status != SyncStatus.Synced) {
                    current
                } else {
                    current.copy(
                        status = if (discoveredPeers.isNotEmpty()) {
                            SyncStatus.PeerAvailable
                        } else {
                            SyncStatus.Discovering
                        },
                    )
                }
            }
        }
    }

    private fun reportError(message: String?) {
        _connectionState.update {
            it.copy(
                status = if (discoveredPeers.isNotEmpty()) SyncStatus.PeerAvailable else SyncStatus.Discovering,
                lastError = message,
            )
        }
    }

    private fun updateStatus(status: SyncStatus) {
        _connectionState.update {
            it.copy(status = status, peerCount = discoveredPeers.size)
        }
    }

    private companion object {
        const val TAG = "SyncService"
        const val SYNCED_INDICATOR_MS = 180_000L
    }
}
