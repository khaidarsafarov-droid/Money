package com.example.money.domain.model

enum class NoteKind {
    TEXT,
    FINANCIAL,
    ;

    companion object {
        fun fromStored(value: String?): NoteKind =
            entries.firstOrNull { it.name == value } ?: TEXT
    }
}

/** Green means the note total is paid. Red means it is still owed. */
enum class NoteMark {
    NONE,
    PAID,
    OWED,
    ;

    companion object {
        fun fromStored(value: String?): NoteMark =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

data class FinanceNote(
    val id: Long = 0,
    val title: String,
    val kind: NoteKind,
    val body: String = "",
    val mark: NoteMark = NoteMark.NONE,
    val closed: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val mode: com.example.money.settings.AppMode = com.example.money.settings.AppMode.PERSONAL,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val deletedAt: Long? = null,
    val linkedType: String? = null,
    val linkedSyncId: String? = null,
)

data class FinanceNoteLine(
    val id: Long = 0,
    val noteId: Long,
    val label: String,
    val amountCents: Long,
    val sortOrder: Int = 0,
)
