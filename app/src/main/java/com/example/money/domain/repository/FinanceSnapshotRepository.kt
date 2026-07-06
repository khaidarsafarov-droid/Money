package com.example.money.domain.repository

import com.example.money.domain.sync.FinanceSnapshot

interface FinanceSnapshotRepository {
    suspend fun buildSnapshot(): FinanceSnapshot
    fun encodeSnapshot(snapshot: FinanceSnapshot): String
    fun decodeSnapshot(raw: String): FinanceSnapshot
}
