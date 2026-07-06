package com.example.money.domain.sync

enum class SyncStatus {
    Offline,
    Discovering,
    PeerAvailable,
    Syncing,
    Synced,
}

data class SyncConnectionState(
    val status: SyncStatus = SyncStatus.Offline,
    val peerCount: Int = 0,
    val lastSyncedAt: Long? = null,
    val lastError: String? = null,
    /** ws://LAN-IP:port/sync — for pairing with the web client. */
    val webSocketUrl: String? = null,
) {
    val isIndicatorActive: Boolean
        get() = when (status) {
            SyncStatus.PeerAvailable,
            SyncStatus.Syncing,
            SyncStatus.Synced,
            -> true
            SyncStatus.Offline,
            SyncStatus.Discovering,
            -> false
        }
}
