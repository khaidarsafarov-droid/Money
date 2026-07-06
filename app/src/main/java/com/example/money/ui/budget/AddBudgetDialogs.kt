package com.example.money.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.util.InputValidators

@Composable
fun AddTransferDialog(
    availableBalance: Double? = null,
    goalRemainingAmount: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showOverGoalConfirm by remember { mutableStateOf(false) }
    var pendingAmount by remember { mutableStateOf<Double?>(null) }

    fun submitAmount(amount: Double) {
        if (availableBalance != null && amount > availableBalance) {
            amountError = "Недостаточно средств на общем остатке"
            return
        }
        if (goalRemainingAmount != null && amount > goalRemainingAmount) {
            pendingAmount = amount
            showOverGoalConfirm = true
        } else {
            onConfirm(amount, date)
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
        title = { Text("Новый перевод") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = "Сумма",
                    isError = amountError != null,
                    supportingText = amountError,
                )
                DatePickerField(
                    label = "Дата",
                    selectedDateMillis = date,
                    onDateSelected = { date = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountError = InputValidators.validateAmount(amountText)
                    if (amountError != null) return@TextButton
                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    submitAmount(amount)
                },
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, date: Long, note: String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый расход") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = "Сумма",
                    isError = amountError != null,
                    supportingText = amountError,
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        categoryError = null
                    },
                    label = { Text("Категория") },
                    isError = categoryError != null,
                    supportingText = categoryError?.let { { Text(it) } },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    singleLine = true,
                )
                DatePickerField(
                    label = "Дата",
                    selectedDateMillis = date,
                    onDateSelected = { date = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountError = InputValidators.validateAmount(amountText)
                    categoryError = InputValidators.validateCategory(category)
                    if (amountError != null || categoryError != null) return@TextButton
                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    onConfirm(amount, category.trim(), date, note.trim())
                },
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )
}
