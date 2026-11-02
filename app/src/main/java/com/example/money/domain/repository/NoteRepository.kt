package com.example.money.domain.repository

import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<FinanceNote>>
    fun observeLines(): Flow<List<FinanceNoteLine>>
    suspend fun addNote(title: String, kind: NoteKind): Long
    suspend fun updateNote(note: FinanceNote)
    suspend fun deleteNote(id: Long)
    suspend fun addLine(
        noteId: Long,
        label: String,
        amountCents: Long,
        occurredAt: Long = System.currentTimeMillis(),
    )
    suspend fun updateLine(line: FinanceNoteLine)
    suspend fun deleteLine(noteId: Long, lineId: Long)
}
