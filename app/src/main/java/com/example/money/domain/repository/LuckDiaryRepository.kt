package com.example.money.domain.repository

import com.example.money.domain.model.LuckDiaryEntry
import kotlinx.coroutines.flow.Flow

interface LuckDiaryRepository {
    fun observeEntries(): Flow<List<LuckDiaryEntry>>
    suspend fun addEntry(entry: LuckDiaryEntry): Long
    suspend fun deleteEntry(entry: LuckDiaryEntry)
}
