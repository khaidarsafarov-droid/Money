package com.example.money.data.sync

import java.util.UUID

object SyncIds {
    fun newId(): String = UUID.randomUUID().toString()
}
