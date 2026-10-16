package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceNoteDao {
    @Query("SELECT * FROM finance_notes WHERE mode = :mode ORDER BY closed ASC, updated_at DESC")
    fun observeNotes(mode: String): Flow<List<FinanceNoteEntity>>

    @Query(
        """
        SELECT finance_note_lines.* FROM finance_note_lines
        INNER JOIN finance_notes ON finance_notes.id = finance_note_lines.note_id
        WHERE finance_notes.mode = :mode
        ORDER BY finance_note_lines.sort_order ASC, finance_note_lines.id ASC
        """,
    )
    fun observeLines(mode: String): Flow<List<FinanceNoteLineEntity>>

    @Query("SELECT * FROM finance_notes")
    suspend fun getAllNotesOnce(): List<FinanceNoteEntity>

    @Query("SELECT * FROM finance_note_lines ORDER BY sort_order ASC, id ASC")
    suspend fun getAllLinesOnce(): List<FinanceNoteLineEntity>

    @Query("SELECT * FROM finance_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): FinanceNoteEntity?

    @Query("SELECT * FROM finance_note_lines WHERE id = :id LIMIT 1")
    suspend fun getLineById(id: Long): FinanceNoteLineEntity?

    @Query("SELECT * FROM finance_note_lines WHERE note_id = :noteId")
    suspend fun getLinesByNoteId(noteId: Long): List<FinanceNoteLineEntity>

    @Query("SELECT * FROM finance_notes WHERE sync_id = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): FinanceNoteEntity?

    @Query("SELECT * FROM finance_note_lines WHERE sync_id = :syncId LIMIT 1")
    suspend fun getLineBySyncId(syncId: String): FinanceNoteLineEntity?

    @Query("DELETE FROM finance_note_lines")
    suspend fun clearLines()

    @Query("DELETE FROM finance_notes")
    suspend fun clearNotes()

    @Insert
    suspend fun insertNote(note: FinanceNoteEntity): Long

    @Update
    suspend fun updateNote(note: FinanceNoteEntity)

    @Query("DELETE FROM finance_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Insert
    suspend fun insertLine(line: FinanceNoteLineEntity): Long

    @Update
    suspend fun updateLine(line: FinanceNoteLineEntity)

    @Query("DELETE FROM finance_note_lines WHERE id = :id")
    suspend fun deleteLine(id: Long)

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM finance_note_lines WHERE note_id = :noteId")
    suspend fun maxSortOrder(noteId: Long): Int

    @Query("UPDATE finance_notes SET updated_at = :updatedAt WHERE id = :noteId")
    suspend fun touchNote(noteId: Long, updatedAt: Long)
}
