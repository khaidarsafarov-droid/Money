package com.example.money.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRetryTest {

    @Test
    fun retriesUntilTheSyncSucceeds() = runBlocking {
        var calls = 0
        SyncRetry.run(attempts = 3, delayMillis = 1) {
            calls += 1
            if (calls < 3) error("not yet")
        }
        assertEquals(3, calls)
    }

    @Test
    fun cancellationIsNotRetried() = runBlocking {
        var calls = 0
        val result = runCatching {
            SyncRetry.run(attempts = 3, delayMillis = 1) {
                calls += 1
                throw CancellationException("stopped")
            }
        }
        assertEquals(1, calls)
        assertTrue(result.exceptionOrNull() is CancellationException)
    }
}
