package com.example.money.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets

/**
 * Outgoing sync client. A closed Ktor client cannot be reused, so each
 * [open] after [close] builds a new one.
 */
class SyncHttpClients(
    private val factory: () -> HttpClient = { create() },
) {
    var client: HttpClient? = null
        private set

    fun open() {
        if (client == null) {
            client = factory()
        }
    }

    fun close() {
        client?.close()
        client = null
    }

    fun require(): HttpClient = client ?: error("Sync HTTP client is not started")

    companion object {
        fun create(): HttpClient = HttpClient(CIO) {
            install(WebSockets)
        }
    }
}
