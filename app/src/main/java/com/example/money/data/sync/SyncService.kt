package com.example.money.data.sync

import android.util.Log
import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SyncConnectionState
import com.example.money.domain.sync.SyncStatus
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
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SyncService(
    private val readDeviceId: suspend () -> String,
    private val financeSnapshotRepository: FinanceSnapshotRepository,
    private val applySnapshot: suspend (FinanceSnapshot) -> Unit,
    private val syncQueueDao: SyncQueueDao,
    private val scope: CoroutineScope,
    private val keyVault: SyncKeyVault,
    private val syncEnabled: suspend () -> Boolean = { true },
    private val httpClients: SyncHttpClients = SyncHttpClients(),
    private val discoveryFactory: PeerDiscoveryFactory,
) {
    private var server: EmbeddedServer<CIOApplicationEngine, *>? = null
    private var nsdDiscovery: PeerDiscovery? = null
    private var queueObserverJob: Job? = null
    private var syncedDecayJob: Job? = null

    private var localDeviceId: String = ""
    private var localServiceName: String = ""
    private val discoveredPeers = ConcurrentHashMap<String, NsdPeerDiscovery.PeerEndpoint>()
    private var hostOffer: PairingOffer? = null
    private var joinerAttempt: OutgoingPairing? = null

    private val _connectionState = MutableStateFlow(SyncConnectionState())
    val connectionState: StateFlow<SyncConnectionState> = _connectionState.asStateFlow()

    fun createPairingOffer(): PairingOffer {
        val offer = SyncPairing.createOffer()
        hostOffer = offer
        joinerAttempt = null
        return offer
    }

    fun pairWithQr(payload: String): Boolean {
        val pairing = SyncPairing.parseQr(payload) ?: return false
        joinerAttempt = pairing
        hostOffer = null
        return true
    }

    fun pairWithPin(pin: String): Boolean {
        val pairing = SyncPairing.fromPin(pin) ?: return false
        joinerAttempt = pairing
        hostOffer = null
        return true
    }

    suspend fun start() {
        if (!syncEnabled()) return
        if (server != null) return
        httpClients.open()
        localDeviceId = readDeviceId()
        localServiceName = NsdPeerDiscovery.buildServiceName(localDeviceId)
        startWebSocketServer()
        val port = server?.engine?.resolvedConnectors()?.firstOrNull()?.port
        if (port == null) {
            stop()
            return
        }
        val webSocketUrl = LocalNetworkAddress.buildWebSocketUrl(
            LocalNetworkAddress.resolveLanIpv4(),
            port,
        )
        _connectionState.update { it.copy(webSocketUrl = webSocketUrl) }
        nsdDiscovery = discoveryFactory.create(
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
        httpClients.close()
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
        val paired = keyVault.find(peerDeviceId)
        if (joinerAttempt == null && paired?.role != SyncPeerRole.JOINER) return
        runInitiatorSession(peer.host, peer.port, peerDeviceId)
    }

    internal suspend fun syncWith(host: String, port: Int, peerId: String) {
        runInitiatorSession(host, port, peerId)
    }

    private suspend fun runInitiatorSession(host: String, port: Int, peerId: String) {
        SyncWriteGuard.withWriteLock {
            updateStatus(SyncStatus.Syncing)
            try {
                httpClients.require().webSocket(host = host, port = port, path = "/sync") {
                    val local = financeSnapshotRepository.buildSnapshot()
                    val pairing = joinerAttempt
                    val merged = SyncSecureChannel.runInitiator(
                        localDeviceId = localDeviceId,
                        expectedPeerId = peerId,
                        vault = keyVault,
                        pairing = pairing,
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
                    if (pairing != null) joinerAttempt = null
                    SyncWriteGuard.runWithoutRecording {
                        applySnapshot(merged)
                    }
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
        SyncWriteGuard.withWriteLock {
            updateStatus(SyncStatus.Syncing)
            try {
                val local = financeSnapshotRepository.buildSnapshot()
                val offer = hostOffer
                val merged = SyncSecureChannel.runAcceptor(
                    localDeviceId = localDeviceId,
                    vault = keyVault,
                    hostOffer = offer,
                    local = local,
                    encode = financeSnapshotRepository::encodeSnapshot,
                    decode = financeSnapshotRepository::decodeSnapshot,
                    sendText = sendText,
                    receiveText = receiveText,
                )
                if (offer != null) hostOffer = null
                SyncWriteGuard.runWithoutRecording {
                    applySnapshot(merged)
                }
                syncQueueDao.clearAll()
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
