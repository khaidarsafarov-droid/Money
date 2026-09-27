package com.example.money.data.repository

import com.example.money.data.local.dao.LuckDiaryDao
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.data.sync.SyncIds
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.model.LuckDiaryEntry
import com.example.money.domain.repository.LuckDiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LuckDiaryRepositoryImpl(
    private val luckDiaryDao: LuckDiaryDao,
) : LuckDiaryRepository {

    override fun observeEntries(): Flow<List<LuckDiaryEntry>> =
        luckDiaryDao.observeAll().map { entries -> entries.map { it.toDomain() } }

    override suspend fun addEntry(entry: LuckDiaryEntry): Long =
        SyncWriteGuard.withWriteLock {
            luckDiaryDao.insert(entry.toEntity())
        }

    override suspend fun updateEntry(entry: LuckDiaryEntry) {
        SyncWriteGuard.withWriteLock {
            luckDiaryDao.update(entry.toEntity())
        }
    }

    override suspend fun deleteEntry(entry: LuckDiaryEntry) {
        SyncWriteGuard.withWriteLock {
            luckDiaryDao.delete(entry.toEntity())
        }
    }

    private fun LuckDiaryEntryEntity.toDomain() = LuckDiaryEntry(
        id = id,
        syncId = syncId,
        entryDate = entryDate,
        note = note,
        amountCents = amountCents,
        category = category,
        createdAt = createdAt,
    )

    private fun LuckDiaryEntry.toEntity() = LuckDiaryEntryEntity(
        id = id,
        syncId = syncId.ifBlank { SyncIds.newId() },
        entryDate = entryDate,
        note = note,
        amountCents = amountCents,
        category = category,
        createdAt = createdAt,
    )
}
