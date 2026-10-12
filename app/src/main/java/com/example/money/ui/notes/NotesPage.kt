package com.example.money.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteKind
import com.example.money.domain.model.NoteMark
import com.example.money.domain.util.MoneyMath
import com.example.money.domain.util.NoteCalculator
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyDialog
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.OverdueRed
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators

@Composable
fun NoteFormDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, kind: NoteKind) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(NoteKind.TEXT) }
    var titleError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    MoneyDialog(
        title = stringResource(R.string.note_add_title),
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.goal_add_confirm),
        onConfirm = {
            titleError = InputValidators.validateName(title)?.let(context::getString)
            if (titleError != null) return@MoneyDialog
            onConfirm(title.trim(), kind)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text(stringResource(R.string.note_title_label)) },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = kind == NoteKind.TEXT,
                    onClick = { kind = NoteKind.TEXT },
                    label = { Text(stringResource(R.string.note_kind_text)) },
                )
                FilterChip(
                    selected = kind == NoteKind.FINANCIAL,
                    onClick = { kind = NoteKind.FINANCIAL },
                    label = { Text(stringResource(R.string.note_kind_financial)) },
                )
            }
        }
    }
}

@Composable
fun NotesPage(
    notes: List<FinanceNote>,
    lines: List<FinanceNoteLine>,
    openNoteId: Long?,
    onOpenNote: (Long) -> Unit,
    onCloseNote: () -> Unit,
    onUpdateNote: (FinanceNote) -> Unit,
    onSetMark: (FinanceNote, NoteMark) -> Unit,
    onSetClosed: (FinanceNote, Boolean) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onAddLine: (Long, String, Long) -> Unit,
    onUpdateLine: (FinanceNoteLine) -> Unit,
    onDeleteLine: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val openNote = notes.firstOrNull { it.id == openNoteId }
    if (openNote == null) {
        NotesList(
            notes = notes,
            lines = lines,
            onOpenNote = onOpenNote,
            modifier = modifier,
        )
    } else {
        NoteEditor(
            note = openNote,
            lines = lines.filter { it.noteId == openNote.id },
            onBack = onCloseNote,
            onUpdateNote = onUpdateNote,
            onSetMark = { onSetMark(openNote, it) },
            onSetClosed = { onSetClosed(openNote, it) },
            onDeleteNote = {
                onDeleteNote(openNote.id)
                onCloseNote()
            },
            onAddLine = { label, cents -> onAddLine(openNote.id, label, cents) },
            onUpdateLine = onUpdateLine,
            onDeleteLine = { lineId -> onDeleteLine(openNote.id, lineId) },
            modifier = modifier,
        )
    }
}

@Composable
private fun NotesList(
    notes: List<FinanceNote>,
    lines: List<FinanceNoteLine>,
    onOpenNote: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MoneySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.tab_notes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (notes.isEmpty()) {
            EmptyStateMessage(
                title = stringResource(R.string.notes_empty_title),
                hint = stringResource(R.string.notes_empty_hint),
            )
        } else {
            notes.forEach { note ->
                val total = NoteCalculator.totalCents(lines.filter { it.noteId == note.id })
                NoteListRow(note = note, totalCents = total, onClick = { onOpenNote(note.id) })
            }
        }
    }
}

@Composable
private fun NoteListRow(
    note: FinanceNote,
    totalCents: Long,
    onClick: () -> Unit,
) {
    val markColor = markColor(note.mark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(markColor.copy(alpha = if (note.mark == NoteMark.NONE) 0.08f else 0.18f))
            .clickable(onClick = onClick)
            .padding(MoneySpacing.md),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = note.title, fontWeight = FontWeight.SemiBold)
        val subtitle = when (note.kind) {
            NoteKind.TEXT -> stringResource(R.string.note_kind_text)
            NoteKind.FINANCIAL -> Formatters.currencyFromCents(totalCents)
        }
        Text(
            text = if (note.closed) {
                stringResource(R.string.note_closed_badge) + " · " + subtitle
            } else {
                subtitle
            },
            color = if (note.mark == NoteMark.NONE) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                markColor
            },
        )
    }
}

@Composable
private fun NoteEditor(
    note: FinanceNote,
    lines: List<FinanceNoteLine>,
    onBack: () -> Unit,
    onUpdateNote: (FinanceNote) -> Unit,
    onSetMark: (NoteMark) -> Unit,
    onSetClosed: (Boolean) -> Unit,
    onDeleteNote: () -> Unit,
    onAddLine: (String, Long) -> Unit,
    onUpdateLine: (FinanceNoteLine) -> Unit,
    onDeleteLine: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(note.id, note.title) { mutableStateOf(note.title) }
    var body by remember(note.id, note.body) { mutableStateOf(note.body) }
    val latestTitle = rememberUpdatedState(title)
    val latestBody = rememberUpdatedState(body)
    val latestNote = rememberUpdatedState(note)
    val latestUpdate = rememberUpdatedState(onUpdateNote)
    DisposableEffect(note.id) {
        onDispose {
            persistText(latestNote.value, latestTitle.value, latestBody.value, latestUpdate.value)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MoneySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                persistText(note, title, body, onUpdateNote)
                onBack()
            }) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.note_back))
            }
            Text(
                text = stringResource(R.string.tab_notes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.note_title_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        when (note.kind) {
            NoteKind.TEXT -> {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(stringResource(R.string.note_body_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                )
                MoneyButton(
                    text = stringResource(R.string.action_save),
                    onClick = { persistText(note, title, body, onUpdateNote) },
                )
            }
            NoteKind.FINANCIAL -> {
                FinancialNoteBody(
                    note = note,
                    lines = lines,
                    onSetMark = onSetMark,
                    onAddLine = onAddLine,
                    onUpdateLine = onUpdateLine,
                    onDeleteLine = onDeleteLine,
                    onTitleCommit = {
                        if (title.isNotBlank() && title.trim() != note.title) {
                            onUpdateNote(note.copy(title = title.trim()))
                        }
                    },
                )
            }
        }
        TextButton(onClick = { onSetClosed(!note.closed) }) {
            Text(
                stringResource(if (note.closed) R.string.note_reopen else R.string.note_close),
            )
        }
        TextButton(onClick = onDeleteNote) {
            Text(stringResource(R.string.note_delete), color = OverdueRed)
        }
    }
}

private fun persistText(
    note: FinanceNote,
    title: String,
    body: String,
    onUpdateNote: (FinanceNote) -> Unit,
) {
    val nextTitle = title.trim().ifBlank { note.title }
    if (nextTitle != note.title || body != note.body) {
        onUpdateNote(note.copy(title = nextTitle, body = body))
    }
}

@Composable
private fun FinancialNoteBody(
    note: FinanceNote,
    lines: List<FinanceNoteLine>,
    onSetMark: (NoteMark) -> Unit,
    onAddLine: (String, Long) -> Unit,
    onUpdateLine: (FinanceNoteLine) -> Unit,
    onDeleteLine: (Long) -> Unit,
    onTitleCommit: () -> Unit,
) {
    var label by remember(note.id) { mutableStateOf("") }
    var amountText by remember(note.id) { mutableStateOf("") }
    var editing by remember(note.id) { mutableStateOf<FinanceNoteLine?>(null) }
    val total = NoteCalculator.totalCents(lines)
    val totalColor = markColor(note.mark)

    lines.forEach { line ->
        if (editing?.id == line.id) {
            LineEditor(
                initialLabel = line.label,
                initialAmount = MoneyMath.fromCents(line.amountCents),
                onCancel = { editing = null },
                onSave = { nextLabel, cents ->
                    onUpdateLine(line.copy(label = nextLabel, amountCents = cents))
                    editing = null
                },
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = line.label, fontWeight = FontWeight.Medium)
                    Text(text = Formatters.currencyFromCents(line.amountCents))
                }
                IconButton(onClick = { editing = line }) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.note_edit_line))
                }
                IconButton(onClick = { onDeleteLine(line.id) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.note_delete))
                }
            }
        }
    }

    Text(
        text = stringResource(R.string.note_total) + ": " + Formatters.currencyFromCents(total),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = totalColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(totalColor.copy(alpha = 0.12f))
            .padding(MoneySpacing.md),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = note.mark == NoteMark.PAID,
            onClick = {
                onTitleCommit()
                onSetMark(NoteMark.PAID)
            },
            label = { Text(stringResource(R.string.note_mark_paid), color = FinancePositive) },
        )
        FilterChip(
            selected = note.mark == NoteMark.OWED,
            onClick = {
                onTitleCommit()
                onSetMark(NoteMark.OWED)
            },
            label = { Text(stringResource(R.string.note_mark_owed), color = OverdueRed) },
        )
    }
    Text(
        text = stringResource(R.string.note_add_line),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    OutlinedTextField(
        value = label,
        onValueChange = { label = it },
        label = { Text(stringResource(R.string.note_line_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    MoneyTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = stringResource(R.string.note_line_amount),
    )
    MoneyButton(
        text = stringResource(R.string.note_add_line),
        onClick = {
            val amount = InputValidators.parseAmount(amountText) ?: return@MoneyButton
            if (label.isBlank()) return@MoneyButton
            onTitleCommit()
            onAddLine(label.trim(), MoneyMath.toCents(amount))
            label = ""
            amountText = ""
        },
    )
}

@Composable
private fun LineEditor(
    initialLabel: String,
    initialAmount: Double,
    onCancel: () -> Unit,
    onSave: (String, Long) -> Unit,
) {
    var label by remember { mutableStateOf(initialLabel) }
    var amountText by remember {
        mutableStateOf(
            if (initialAmount % 1.0 == 0.0) initialAmount.toLong().toString() else initialAmount.toString(),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(R.string.note_line_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MoneyTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = stringResource(R.string.note_line_amount),
        )
        Row {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
            TextButton(onClick = {
                val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                if (label.isBlank()) return@TextButton
                onSave(label.trim(), MoneyMath.toCents(amount))
            }) { Text(stringResource(R.string.action_save)) }
        }
    }
}

private fun markColor(mark: NoteMark): Color = when (mark) {
    NoteMark.NONE -> Color(0xFF5C7D72)
    NoteMark.PAID -> FinancePositive
    NoteMark.OWED -> OverdueRed
}
