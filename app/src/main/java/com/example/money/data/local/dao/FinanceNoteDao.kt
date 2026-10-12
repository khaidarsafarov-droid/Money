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
    @Query("SELECT * FROM finance_notes ORDER BY closed ASC, updated_at DESC")
    fun observeNotes(): Flow<List<FinanceNoteEntity>>

    @Query("SELECT * FROM finance_note_lines ORDER BY sort_order ASC, id ASC")
    fun observeLines(): Flow<List<FinanceNoteLineEntity>>

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
