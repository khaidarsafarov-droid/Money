package com.example.money.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import com.example.money.domain.model.NoteMark
import com.example.money.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val TEXT_SAVE_DELAY_MS = 500L

private data class NoteTextDraft(
    val title: String,
    val body: String,
    val base: FinanceNote,
)

@OptIn(FlowPreview::class)
class NoteViewModel(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    val notes: StateFlow<List<FinanceNote>> = noteRepository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lines: StateFlow<List<FinanceNoteLine>> = noteRepository.observeLines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val textDrafts = MutableStateFlow<Map<Long, NoteTextDraft>>(emptyMap())

    init {
        viewModelScope.launch {
            textDrafts.debounce(TEXT_SAVE_DELAY_MS).collect { drafts ->
                flushDrafts(drafts)
            }
        }
    }

    fun onTitleChange(note: FinanceNote, title: String) {
        stageDraft(note, title = title, body = textDrafts.value[note.id]?.body ?: note.body)
    }

    fun onBodyChange(note: FinanceNote, body: String) {
        stageDraft(note, title = textDrafts.value[note.id]?.title ?: note.title, body = body)
    }

    fun hasUnsavedDraft(noteId: Long): Boolean = textDrafts.value.containsKey(noteId)

    fun flushDraft(note: FinanceNote, title: String, body: String) {
        stageDraft(note, title, body)
        val draft = textDrafts.value[note.id] ?: return
        viewModelScope.launch { flushDrafts(mapOf(note.id to draft)) }
    }

    override fun onCleared() {
        val pending = textDrafts.value
        if (pending.isEmpty()) return
        runBlocking { flushDrafts(pending) }
    }

    private fun stageDraft(note: FinanceNote, title: String, body: String) {
        if (title == note.title && body == note.body) {
            textDrafts.update { current -> current - note.id }
            return
        }
        textDrafts.update { current ->
            current + (note.id to NoteTextDraft(title = title, body = body, base = current[note.id]?.base ?: note))
        }
    }

    private suspend fun flushDrafts(drafts: Map<Long, NoteTextDraft>) {
        drafts.forEach { (noteId, draft) ->
            val latest = textDrafts.value[noteId] ?: return@forEach
            if (latest != draft) return@forEach
            val nextTitle = latest.title.trim().ifBlank { latest.base.title }
            if (nextTitle != latest.base.title || latest.body != latest.base.body) {
                noteRepository.updateNote(latest.base.copy(title = nextTitle, body = latest.body))
            }
            textDrafts.update { current ->
                if (current[noteId] == latest) current - noteId else current
            }
        }
    }

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
