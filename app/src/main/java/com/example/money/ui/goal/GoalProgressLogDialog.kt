package com.example.money.ui.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.components.bringIntoViewOnFocus
import com.example.money.ui.components.scrollAboveIme
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators

/**
 * Money goals record an amount on that goal and an optional completed task.
 * Any other goal records only the task.
 */
@Composable
fun GoalProgressLogDialog(
    goal: SavingsGoal,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
    onSave: (amountCents: Long?, note: String, dateMillis: Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }
    val needInput = stringResource(R.string.goal_log_need_input)
    val badAmount = stringResource(R.string.error_amount_positive)
    val tracksMoney = goal.kind == GoalKind.MONEY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_log_title, goal.name)) },
        text = {
            DialogImeEffect()
            Column(
                modifier = Modifier.scrollAboveIme(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (tracksMoney) {
                    Text(
                        text = stringResource(
                            R.string.goal_saved_progress,
                            Formatters.currencyFromCents(goal.currentAmountCents, hide = hideAmounts),
                            Formatters.currencyFromCents(goal.targetAmountCents, hide = hideAmounts),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MoneyTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            error = null
                        },
                        label = stringResource(R.string.goal_log_amount),
                        isError = error != null,
                        supportingText = error,
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                        error = null
                    },
                    label = { Text(stringResource(R.string.goal_log_note)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
                if (tracksMoney) {
                    DatePickerField(
                        label = stringResource(R.string.field_date),
                        selectedDateMillis = date,
                        onDateSelected = { date = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedNote = note.trim()
                    val amount = if (tracksMoney) {
                        amountText.trim().takeIf { it.isNotEmpty() }?.let { raw ->
                            InputValidators.parseAmount(raw)
                        }
                    } else {
                        null
                    }
                    if (tracksMoney && amountText.isNotBlank() && amount == null) {
                        error = badAmount
                        return@TextButton
                    }
                    if (amount == null && trimmedNote.isEmpty()) {
                        error = needInput
                        return@TextButton
                    }
                    onSave(amount, trimmedNote, date)
                },
            ) {
                Text(stringResource(R.string.goal_log_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
