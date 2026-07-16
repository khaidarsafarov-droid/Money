package com.example.money.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes local DB writes and P2P sync apply so mutations cannot interleave
 * with snapshot build/apply (prevents sync-time data loss).
 */
object SyncWriteGuard {
    private val writeMutex = Mutex()
    private var suppressRecordingDepth = 0

    val isRecordingSuppressed: Boolean
        get() = suppressRecordingDepth > 0

    suspend fun <T> withWriteLock(block: suspend () -> T): T =
        writeMutex.withLock { block() }

    suspend fun <T> runWithoutRecording(block: suspend () -> T): T {
        suppressRecordingDepth++
        return try {
            block()
        } finally {
            suppressRecordingDepth = (suppressRecordingDepth - 1).coerceAtLeast(0)
        }
    }
}
