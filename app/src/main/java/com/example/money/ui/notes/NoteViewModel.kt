package com.example.money.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import com.example.money.domain.model.NoteMark
import com.example.money.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    val notes: StateFlow<List<FinanceNote>> = noteRepository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lines: StateFlow<List<FinanceNoteLine>> = noteRepository.observeLines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addNote(title: String, kind: NoteKind, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = noteRepository.addNote(title, kind)
            onCreated(id)
        }
    }

    fun updateNote(note: FinanceNote) {
        viewModelScope.launch { noteRepository.updateNote(note) }
    }

    fun setMark(note: FinanceNote, mark: NoteMark) {
        val next = if (note.mark == mark) NoteMark.NONE else mark
        updateNote(note.copy(mark = next))
    }

    fun setClosed(note: FinanceNote, closed: Boolean) {
        updateNote(note.copy(closed = closed))
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { noteRepository.deleteNote(id) }
    }

    fun addLine(noteId: Long, label: String, amountCents: Long) {
        viewModelScope.launch { noteRepository.addLine(noteId, label, amountCents) }
    }

    fun updateLine(line: FinanceNoteLine) {
        viewModelScope.launch { noteRepository.updateLine(line) }
    }

    fun deleteLine(noteId: Long, lineId: Long) {
        viewModelScope.launch { noteRepository.deleteLine(noteId, lineId) }
    }

    class Factory(
        private val noteRepository: NoteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteViewModel(noteRepository) as T
        }
    }
}
