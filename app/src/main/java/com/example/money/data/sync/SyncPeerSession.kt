package com.example.money.data.sync

import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SyncEngine

object SyncPeerSession {
    const val ACK = "ACK"

    suspend fun runAsInitiator(
        local: FinanceSnapshot,
        encode: (FinanceSnapshot) -> String,
        decode: (String) -> FinanceSnapshot,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): FinanceSnapshot {
        sendText(encode(local))
        val remote = decode(receiveText())
        return SyncEngine.mergeSymmetric(local, remote)
    }

    suspend fun runAsAcceptor(
        local: FinanceSnapshot,
        encode: (FinanceSnapshot) -> String,
        decode: (String) -> FinanceSnapshot,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): FinanceSnapshot {
        val remote = decode(receiveText())
        sendText(encode(local))
        return SyncEngine.mergeSymmetric(local, remote)
    }
}
