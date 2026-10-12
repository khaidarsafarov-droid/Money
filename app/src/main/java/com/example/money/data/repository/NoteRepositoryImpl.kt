package com.example.money.data.repository

import com.example.money.data.local.dao.FinanceNoteDao
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import com.example.money.domain.model.NoteMark
import com.example.money.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: FinanceNoteDao,
) : NoteRepository {

    override fun observeNotes(): Flow<List<FinanceNote>> =
        noteDao.observeNotes().map { notes -> notes.map { it.toDomain() } }

    override fun observeLines(): Flow<List<FinanceNoteLine>> =
        noteDao.observeLines().map { lines -> lines.map { it.toDomain() } }

    override suspend fun addNote(title: String, kind: NoteKind): Long =
        SyncWriteGuard.withWriteLock {
            val now = System.currentTimeMillis()
            noteDao.insertNote(
                FinanceNoteEntity(
                    title = title.trim(),
                    kind = kind.name,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }

    override suspend fun updateNote(note: FinanceNote) {
        SyncWriteGuard.withWriteLock {
            val existing = noteDao.getNoteById(note.id) ?: return@withWriteLock
            noteDao.updateNote(
                existing.copy(
                    title = note.title.trim(),
                    kind = note.kind.name,
                    body = note.body,
                    mark = note.mark.name,
                    closed = note.closed,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun deleteNote(id: Long) {
        SyncWriteGuard.withWriteLock {
            noteDao.deleteNote(id)
        }
    }

    override suspend fun addLine(noteId: Long, label: String, amountCents: Long) {
        SyncWriteGuard.withWriteLock {
            val order = noteDao.maxSortOrder(noteId) + 1
            noteDao.insertLine(
                FinanceNoteLineEntity(
                    noteId = noteId,
                    label = label.trim(),
                    amountCents = amountCents,
                    sortOrder = order,
                ),
            )
            noteDao.touchNote(noteId, System.currentTimeMillis())
        }
    }

    override suspend fun updateLine(line: FinanceNoteLine) {
        SyncWriteGuard.withWriteLock {
            val existing = noteDao.getLineById(line.id) ?: return@withWriteLock
            noteDao.updateLine(
                existing.copy(
                    label = line.label.trim(),
                    amountCents = line.amountCents,
                    sortOrder = line.sortOrder,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            noteDao.touchNote(line.noteId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteLine(noteId: Long, lineId: Long) {
        SyncWriteGuard.withWriteLock {
            noteDao.deleteLine(lineId)
            noteDao.touchNote(noteId, System.currentTimeMillis())
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
)

private fun FinanceNoteLineEntity.toDomain() = FinanceNoteLine(
    id = id,
    noteId = noteId,
    label = label,
    amountCents = amountCents,
    sortOrder = sortOrder,
)
