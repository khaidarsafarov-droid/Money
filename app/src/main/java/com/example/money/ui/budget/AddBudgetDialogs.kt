package com.example.money.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.components.bringIntoViewOnFocus
import com.example.money.ui.components.scrollAboveIme
import com.example.money.ui.util.InputValidators

@Composable
fun AddTransferDialog(
    goalRemainingCents: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, date: Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showOverGoalConfirm by remember { mutableStateOf(false) }
    var pendingAmount by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    fun submitAmount(amountCents: Long) {
        if (goalRemainingCents != null && amountCents > goalRemainingCents) {
            pendingAmount = amountCents
            showOverGoalConfirm = true
        } else {
            onConfirm(amountCents, date)
        }
    }

    if (showOverGoalConfirm) {
        AlertDialog(
            onDismissRequest = { showOverGoalConfirm = false },
            title = { Text(stringResource(R.string.transfer_over_goal_title)) },
            text = { Text(stringResource(R.string.transfer_over_goal_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = pendingAmount ?: return@TextButton
                        showOverGoalConfirm = false
                        onConfirm(amount, date)
                    },
                ) {
                    Text(stringResource(R.string.action_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverGoalConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_new_transfer_title)) },
        text = {
            DialogImeEffect()
            Column(
                modifier = Modifier.scrollAboveIme(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = stringResource(R.string.field_amount),
                    isError = amountError != null,
                    supportingText = amountError,
                )
                DatePickerField(
                    label = stringResource(R.string.field_date),
                    selectedDateMillis = date,
                    onDateSelected = { date = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountError = InputValidators.validateAmount(amountText)?.let(context::getString)
                    if (amountError != null) return@TextButton
                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    submitAmount(amount)
                },
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun AddExpenseDialog(
    titleRes: Int = R.string.budget_new_expense_title,
    spendLimitCents: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, category: String, date: Long, note: String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val overLimit = stringResource(R.string.msg_goal_insufficient)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            DialogImeEffect()
            Column(
                modifier = Modifier.scrollAboveIme(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = stringResource(R.string.field_amount),
                    isError = amountError != null,
                    supportingText = amountError,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        categoryError = null
                    },
                    label = { Text(stringResource(R.string.field_category)) },
                    isError = categoryError != null,
                    supportingText = categoryError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_note_optional)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
                DatePickerField(
                    label = stringResource(R.string.field_date),
                    selectedDateMillis = date,
                    onDateSelected = { date = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountError = InputValidators.validateAmount(amountText)?.let(context::getString)
                    categoryError = InputValidators.validateCategory(category)?.let(context::getString)
                    if (amountError != null || categoryError != null) return@TextButton
                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    if (spendLimitCents != null && amount > spendLimitCents) {
                        amountError = overLimit
                        return@TextButton
                    }
                    onConfirm(amount, category.trim(), date, note.trim())
                },
            ) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
