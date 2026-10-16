package com.example.money.ui.notes

import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import com.example.money.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val saved = mutableListOf<FinanceNote>()
    private val deletedNotes = mutableListOf<Long>()

    private val repository = object : NoteRepository {
        override fun observeNotes(): Flow<List<FinanceNote>> = MutableStateFlow(emptyList())
        override fun observeLines(): Flow<List<FinanceNoteLine>> = MutableStateFlow(emptyList())
        override suspend fun addNote(title: String, kind: NoteKind): Long = 1L
        override suspend fun updateNote(note: FinanceNote) {
            saved += note
        }
        override suspend fun deleteNote(id: Long) {
            deletedNotes += id
        }
        override suspend fun addLine(noteId: Long, label: String, amountCents: Long) = Unit
        override suspend fun updateLine(line: FinanceNoteLine) = Unit
        override suspend fun deleteLine(noteId: Long, lineId: Long) = Unit
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun textDraft_savesAfterQuietPeriod() = runTest(dispatcher) {
        val viewModel = NoteViewModel(repository)
        val note = FinanceNote(id = 7L, title = "List", kind = NoteKind.TEXT, body = "")

        viewModel.onBodyChange(note, "hello")
        advanceTimeBy(499)
        assertTrue(saved.isEmpty())

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf("hello"), saved.map { it.body })
        assertEquals("List", saved.single().title)
    }

    @Test
    fun scheduledDelete_canBeUndoneBeforeTheTimeout() = runTest(dispatcher) {
        val viewModel = NoteViewModel(repository)
        viewModel.scheduleDeleteNote(4L)
        advanceTimeBy(4_999)
        assertTrue(deletedNotes.isEmpty())

        viewModel.undoDeleteNote(4L)
        advanceTimeBy(5_000)
        advanceUntilIdle()

        assertTrue(deletedNotes.isEmpty())
    }
}
