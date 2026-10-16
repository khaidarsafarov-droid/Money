package com.example.money.data.sync

import java.util.UUID

object SyncIds {
    private val uuid = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    )

    fun newId(): String = UUID.randomUUID().toString()

    fun isUuid(value: String): Boolean = uuid.matches(value)
}
