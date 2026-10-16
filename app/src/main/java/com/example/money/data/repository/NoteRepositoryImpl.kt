package com.example.money.data.repository

import com.example.money.data.local.dao.FinanceNoteDao
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import com.example.money.domain.model.NoteMark
import com.example.money.domain.repository.NoteRepository
import com.example.money.settings.AppMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: FinanceNoteDao,
    private val syncQueueRecorder: SyncQueueRecorder,
    private val appMode: Flow<AppMode>,
) : NoteRepository {

    override fun observeNotes(): Flow<List<FinanceNote>> =
        appMode.map { it.name }.distinctUntilChanged().flatMapLatest { mode ->
            noteDao.observeNotes(mode)
        }.map { notes -> notes.map { it.toDomain() } }

    override fun observeLines(): Flow<List<FinanceNoteLine>> =
        appMode.map { it.name }.distinctUntilChanged().flatMapLatest { mode ->
            noteDao.observeLines(mode)
        }.map { lines -> lines.map { it.toDomain() } }

    override suspend fun addNote(title: String, kind: NoteKind): Long =
        SyncWriteGuard.withWriteLock {
            val now = System.currentTimeMillis()
            val id = noteDao.insertNote(
                FinanceNoteEntity(
                    title = title.trim(),
                    kind = kind.name,
                    createdAt = now,
                    updatedAt = now,
                    mode = appMode.first().name,
                ),
            )
            noteDao.getNoteById(id)?.let { syncQueueRecorder.recordNote(it) }
            id
        }

    override suspend fun updateNote(note: FinanceNote) {
        SyncWriteGuard.withWriteLock {
            val existing = noteDao.getNoteById(note.id) ?: return@withWriteLock
            val updated = existing.copy(
                title = note.title.trim(),
                kind = note.kind.name,
                body = note.body,
                mark = note.mark.name,
                closed = note.closed,
                updatedAt = System.currentTimeMillis(),
                pinned = note.pinned,
                archived = note.archived,
                deletedAt = note.deletedAt,
                linkedType = note.linkedType,
                linkedSyncId = note.linkedSyncId,
            )
            noteDao.updateNote(updated)
            syncQueueRecorder.recordNote(updated)
        }
    }

    override suspend fun deleteNote(id: Long) {
        SyncWriteGuard.withWriteLock {
            val existing = noteDao.getNoteById(id) ?: return@withWriteLock
            val lines = noteDao.getLinesByNoteId(id)
            noteDao.deleteNote(id)
            lines.forEach { line ->
                syncQueueRecorder.recordNoteLine(line, existing.syncId, deleted = true)
            }
            syncQueueRecorder.recordNote(existing, deleted = true)
        }
    }

    override suspend fun addLine(noteId: Long, label: String, amountCents: Long) {
        SyncWriteGuard.withWriteLock {
            val note = noteDao.getNoteById(noteId) ?: return@withWriteLock
            val order = noteDao.maxSortOrder(noteId) + 1
            val id = noteDao.insertLine(
                FinanceNoteLineEntity(
                    noteId = noteId,
                    label = label.trim(),
                    amountCents = amountCents,
                    sortOrder = order,
                ),
            )
            val now = System.currentTimeMillis()
            noteDao.touchNote(noteId, now)
            noteDao.getLineById(id)?.let { syncQueueRecorder.recordNoteLine(it, note.syncId) }
            noteDao.getNoteById(noteId)?.let { syncQueueRecorder.recordNote(it) }
        }
    }

    override suspend fun updateLine(line: FinanceNoteLine) {
        SyncWriteGuard.withWriteLock {
            val existing = noteDao.getLineById(line.id) ?: return@withWriteLock
            val note = noteDao.getNoteById(line.noteId) ?: return@withWriteLock
            val updated = existing.copy(
                label = line.label.trim(),
                amountCents = line.amountCents,
                sortOrder = line.sortOrder,
                updatedAt = System.currentTimeMillis(),
            )
            noteDao.updateLine(updated)
            val now = System.currentTimeMillis()
            noteDao.touchNote(line.noteId, now)
            syncQueueRecorder.recordNoteLine(updated, note.syncId)
            noteDao.getNoteById(line.noteId)?.let { syncQueueRecorder.recordNote(it) }
        }
    }

    override suspend fun deleteLine(noteId: Long, lineId: Long) {
        SyncWriteGuard.withWriteLock {
            val existing = noteDao.getLineById(lineId) ?: return@withWriteLock
            val note = noteDao.getNoteById(noteId) ?: return@withWriteLock
            noteDao.deleteLine(lineId)
            val now = System.currentTimeMillis()
            noteDao.touchNote(noteId, now)
            syncQueueRecorder.recordNoteLine(existing, note.syncId, deleted = true)
            noteDao.getNoteById(noteId)?.let { syncQueueRecorder.recordNote(it) }
        }
    }
}

private fun FinanceNoteEntity.toDomain() = FinanceNote(
    id = id,
    title = title,
    kind = NoteKind.fromStored(kind),
    body = body,
    mark = NoteMark.fromStored(mark),
    closed = closed,
    createdAt = createdAt,
    updatedAt = updatedAt,
    mode = com.example.money.settings.AppMode.fromStored(mode),
    pinned = pinned,
    archived = archived,
    deletedAt = deletedAt,
    linkedType = linkedType,
    linkedSyncId = linkedSyncId,
)

private fun FinanceNoteLineEntity.toDomain() = FinanceNoteLine(
    id = id,
    noteId = noteId,
    label = label,
    amountCents = amountCents,
    sortOrder = sortOrder,
)
