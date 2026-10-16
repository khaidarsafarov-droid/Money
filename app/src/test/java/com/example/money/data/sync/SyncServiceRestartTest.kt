package com.example.money.data.sync

import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotGoal
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SyncServiceRestartTest {

    @Test
    fun stopThenStartThenSyncUsesAFreshClient() = runBlocking {
        val sharedKey = ByteArray(32) { index -> index.toByte() }
        val remote = snapshot(deviceId = "peer-device", goalName = "From peer")
        val hostVault = MemorySyncKeyVault().apply {
            save(PairedPeer("local-device", sharedKey, SyncPeerRole.HOST))
        }
        val peer = embeddedServer(CIO, host = "127.0.0.1", port = 0) {
            install(WebSockets)
            routing {
                webSocket("/sync") {
                    SyncSecureChannel.runAcceptor(
                        localDeviceId = "peer-device",
                        vault = hostVault,
                        hostOffer = null,
                        local = remote,
                        encode = FinanceSnapshotCodec::encode,
                        decode = FinanceSnapshotCodec::decode,
                        sendText = { text -> send(Frame.Text(text)) },
                        receiveText = {
                            when (val frame = incoming.receive()) {
                                is Frame.Text -> frame.readText()
                                else -> error("Unexpected frame")
                            }
                        },
                    )
                }
            }
        }.start(wait = false)
        val port = peer.engine.resolvedConnectors().first().port
        val applied = mutableListOf<FinanceSnapshot>()
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Default)
        var clientsCreated = 0
        val service = SyncService(
            readDeviceId = { "local-device" },
            financeSnapshotRepository = repository(snapshot(deviceId = "local-device", goalName = "Local")),
            applySnapshot = { applied += it },
            syncQueueDao = FakeSyncQueueDao(),
            scope = scope,
            keyVault = MemorySyncKeyVault().apply {
                save(PairedPeer("peer-device", sharedKey, SyncPeerRole.JOINER))
            },
            httpClients = SyncHttpClients {
                clientsCreated += 1
                SyncHttpClients.create()
            },
            discoveryFactory = PeerDiscoveryFactory { _, _, _ -> NoopPeerDiscovery },
        )

        try {
            service.start()
            service.stop()
            service.start()
            service.syncWith("127.0.0.1", port, "peer-device")
            assertEquals(2, clientsCreated)
            assertEquals("From peer", applied.single().goals.single { it.name == "From peer" }.name)
        } finally {
            service.stop()
            job.cancel()
            peer.stop(gracePeriodMillis = 100, timeoutMillis = 500)
        }
    }

    private fun repository(local: FinanceSnapshot) = object : FinanceSnapshotRepository {
        override suspend fun buildSnapshot(): FinanceSnapshot = local
        override fun encodeSnapshot(snapshot: FinanceSnapshot): String = FinanceSnapshotCodec.encode(snapshot)
        override fun decodeSnapshot(raw: String): FinanceSnapshot = FinanceSnapshotCodec.decode(raw)
    }

    private fun snapshot(deviceId: String, goalName: String) = FinanceSnapshot(
        schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
        deviceId = deviceId,
        updatedAt = 1L,
        goals = listOf(
            SnapshotGoal(
                id = "goal-$deviceId",
                name = goalName,
                targetAmountCents = 1_000L,
                currency = "USD",
                dateDeadline = null,
                syncUpdatedAt = 1L,
            ),
        ),
        debts = emptyList(),
        debtPayments = emptyList(),
        transactions = emptyList(),
        voiceSettings = null,
    )

    private object NoopPeerDiscovery : PeerDiscovery {
        override fun registerService(port: Int) = Unit
        override fun startDiscovery() = Unit
        override fun stop() = Unit
    }

    private class FakeSyncQueueDao : SyncQueueDao {
        override suspend fun getAllPending(): List<SyncQueueEntity> = emptyList()
        override fun observePendingCount(): Flow<Int> = flowOf(0)
        override suspend fun insert(item: SyncQueueEntity) = Unit
        override suspend fun deleteById(id: String) = Unit
        override suspend fun deleteByEntity(entityId: Long, entityType: String) = Unit
        override suspend fun clearAll() = Unit
    }
}
