package com.example.money.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SyncWriteGuard {
    private val mutex = Mutex()
    private var suppressRecordingDepth = 0

    val isRecordingSuppressed: Boolean
        get() = suppressRecordingDepth > 0

    suspend fun <T> runWithoutRecording(block: suspend () -> T): T {
        mutex.withLock { suppressRecordingDepth++ }
        return try {
            block()
        } finally {
            mutex.withLock {
                suppressRecordingDepth = (suppressRecordingDepth - 1).coerceAtLeast(0)
            }
        }
    }
}
