package com.example.money.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

object SyncRetry {
    const val MAX_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 400L
    const val DEBOUNCE_MS = 500L

    suspend fun run(
        attempts: Int = MAX_ATTEMPTS,
        delayMillis: Long = RETRY_DELAY_MS,
        block: suspend () -> Unit,
    ) {
        var attempt = 0
        while (true) {
            try {
                block()
                return
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                attempt += 1
                if (attempt >= attempts) throw exception
                delay(delayMillis * attempt)
            }
        }
    }
}
