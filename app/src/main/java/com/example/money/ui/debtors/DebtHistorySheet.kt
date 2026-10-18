package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.data.sync.DebtAdvanceIds
import com.example.money.domain.model.DebtAdvance
import com.example.money.domain.model.Debtor
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtHistorySheet(
    debtor: Debtor,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
    onAddAdvance: (amountCents: Long, givenAt: Long, note: String) -> Unit,
) {
    var showForm by remember(debtor.id) { mutableStateOf(false) }
    val entries = remember(debtor.id, debtor.advances, debtor.paymentHistory, debtor.targetAmountCents, debtor.dateGiven) {
        historyEntries(debtor)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.debt_history_title, debtor.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.debt_history_remaining,
                    Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency, hide = hideAmounts),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.debt_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                entries.forEach { entry ->
                    HistoryRow(entry = entry, currency = debtor.currency, hideAmounts = hideAmounts)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
            if (showForm) {
                AdvanceForm(
                    onCancel = { showForm = false },
                    onSave = { amountCents, givenAt, note ->
                        onAddAdvance(amountCents, givenAt, note)
                        showForm = false
                    },
                )
            } else {
                Button(
                    onClick = { showForm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.debt_history_add))
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    entry: DebtHistoryEntry,
    currency: String,
    hideAmounts: Boolean,
) {
    val amount = Formatters.currencyFromCents(entry.amountCents, currency, hide = hideAmounts)
    val label = when (entry.kind) {
        DebtHistoryKind.Advance -> stringResource(R.string.debt_history_advance, amount)
        DebtHistoryKind.Payment -> stringResource(R.string.debt_history_payment, amount)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = Formatters.date(entry.at),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (entry.note.isNotBlank()) {
            Text(
                text = entry.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdvanceForm(
    onCancel: () -> Unit,
    onSave: (amountCents: Long, givenAt: Long, note: String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var givenAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    val positiveError = stringResource(R.string.error_amount_positive)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MoneyTextField(
            value = amountText,
            onValueChange = {
                amountText = it
                amountError = null
            },
            label = stringResource(R.string.debt_advance_amount_label),
            isError = amountError != null,
            supportingText = amountError,
        )
        DatePickerField(
            label = stringResource(R.string.debt_advance_date_label),
            selectedDateMillis = givenAt,
            onDateSelected = { givenAt = it },
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.debt_advance_note_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = {
                    val cents = InputValidators.parseAmount(amountText)
                    if (cents == null || cents <= 0L) {
                        amountError = positiveError
                    } else {
                        onSave(cents, givenAt, note.trim())
                    }
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

private enum class DebtHistoryKind { Advance, Payment }

private data class DebtHistoryEntry(
    val at: Long,
    val amountCents: Long,
    val note: String,
    val kind: DebtHistoryKind,
)

private fun historyEntries(debtor: Debtor): List<DebtHistoryEntry> {
    val advances = debtor.advances.ifEmpty {
        listOf(
            DebtAdvance(
                syncId = DebtAdvanceIds.opening(debtor.syncId),
                debtorId = debtor.id,
                amountCents = debtor.targetAmountCents,
                currency = debtor.currency,
                givenAt = debtor.dateGiven,
                mode = debtor.mode,
            ),
        ).filter { it.amountCents > 0L }
    }
    val advanceRows = advances.map { advance ->
        DebtHistoryEntry(
            at = advance.givenAt,
            amountCents = advance.amountCents,
            note = advance.note,
            kind = DebtHistoryKind.Advance,
        )
    }
    val paymentRows = debtor.paymentHistory.map { payment ->
        DebtHistoryEntry(
            at = payment.paidAt,
            amountCents = payment.amountCents,
            note = "",
            kind = DebtHistoryKind.Payment,
        )
    }
    return (advanceRows + paymentRows).sortedBy { it.at }
}
