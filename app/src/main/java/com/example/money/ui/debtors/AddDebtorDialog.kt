package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.MoneyMath
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators

private fun formatAmountForInput(amount: Double): String {
    if (amount <= 0) return ""
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}

@Composable
fun DebtorFormDialog(
    debtType: DebtType,
    existingDebtor: Debtor? = null,
    availableBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        amount: Double,
        dateGiven: Long,
        dateDue: Long,
        affectsBalance: Boolean,
        note: String,
    ) -> Unit,
) {
    val isEdit = existingDebtor != null
    val resolvedType = existingDebtor?.debtType ?: debtType

    var name by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.name.orEmpty())
    }
    var amountText by remember(existingDebtor?.id, debtType) {
        mutableStateOf(
            existingDebtor?.let { formatAmountForInput(MoneyMath.fromCents(it.targetAmountCents)) }
                .orEmpty(),
        )
    }
    var note by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.note.orEmpty())
    }
    var affectsBalance by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.affectsBalance ?: true)
    }
    var dateGiven by remember(existingDebtor?.id, debtType) {
        mutableLongStateOf(existingDebtor?.dateGiven ?: System.currentTimeMillis())
    }
    var dateDue by remember(existingDebtor?.id, debtType) {
        mutableLongStateOf(existingDebtor?.dateDue ?: defaultDueDateMillis())
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }

    val titleRes = when {
        isEdit -> R.string.debtor_edit_title
        resolvedType == DebtType.CREDITOR -> R.string.debt_add_title_owes_me
        else -> R.string.debt_add_title_i_owe
    }
    val nameLabelRes = when (resolvedType) {
        DebtType.CREDITOR -> R.string.debtor_name_label
        DebtType.DEBTOR -> R.string.debt_creditor_name_label
    }
    val dateGivenLabelRes = when (resolvedType) {
        DebtType.CREDITOR -> R.string.debtor_date_given_label
        DebtType.DEBTOR -> R.string.debt_date_received_label
    }
    val balanceCheckboxRes = when (resolvedType) {
        DebtType.CREDITOR -> R.string.debt_deduct_from_budget
        DebtType.DEBTOR -> R.string.debt_credit_to_budget
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) {
                    stringResource(R.string.debtor_edit_title, existingDebtor!!.name)
                } else {
                    stringResource(titleRes)
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(nameLabelRes)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = stringResource(R.string.debtor_amount_label),
                    isError = amountError != null,
                    supportingText = amountError,
                )
                if (isEdit && existingDebtor != null && existingDebtor.currentAmountCents > 0L) {
                    Text(
                        text = stringResource(
                            R.string.debtor_edit_paid_hint,
                            formatAmountForInput(MoneyMath.fromCents(existingDebtor.currentAmountCents)),
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    )
                }
                DatePickerField(
                    label = stringResource(dateGivenLabelRes),
                    selectedDateMillis = dateGiven,
                    onDateSelected = {
                        dateGiven = it
                        dateError = null
                    },
                )
                DatePickerField(
                    label = stringResource(R.string.debtor_date_due_label),
                    selectedDateMillis = dateDue,
                    onDateSelected = {
                        dateDue = it
                        dateError = null
                    },
                    isError = dateError != null,
                    supportingText = dateError,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.debt_note_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = affectsBalance,
                        onCheckedChange = { affectsBalance = it },
                    )
                    Column {
                        Text(stringResource(balanceCheckboxRes))
                        Text(
                            text = stringResource(
                                R.string.debt_balance_available,
                                Formatters.currencyCompact(availableBalance),
                            ),
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    nameError = InputValidators.validateName(name)
                    amountError = InputValidators.validateAmount(amountText)
                    if (amountError == null) {
                        val parsedAmount = InputValidators.parseAmount(amountText)
                        if (parsedAmount != null && MoneyMath.toCents(parsedAmount) <= 0L) {
                            amountError = context.getString(R.string.error_amount_positive)
                        }
                    }
                    dateError = InputValidators.validateDueDate(dateGiven, dateDue)

                    if (isEdit && amountError == null) {
                        val amount = InputValidators.parseAmount(amountText)
                        if (amount != null) {
                            val targetCents = MoneyMath.toCents(amount)
                            if (targetCents < existingDebtor!!.currentAmountCents) {
                                amountError = context.getString(R.string.error_amount_below_paid)
                            }
                        }
                    }

                    if (nameError != null || amountError != null || dateError != null) return@TextButton

                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    onConfirm(name.trim(), amount, dateGiven, dateDue, affectsBalance, note.trim())
                },
            ) {
                Text(
                    if (isEdit) stringResource(R.string.action_save)
                    else stringResource(R.string.debtor_add_confirm),
                )
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
fun AddDebtorDialog(
    debtType: DebtType,
    availableBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        amount: Double,
        dateGiven: Long,
        dateDue: Long,
        affectsBalance: Boolean,
        note: String,
    ) -> Unit,
) = DebtorFormDialog(
    debtType = debtType,
    existingDebtor = null,
    availableBalance = availableBalance,
    onDismiss = onDismiss,
    onConfirm = onConfirm,
)

private fun defaultDueDateMillis(): Long {
    val calendar = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, 30)
        set(java.util.Calendar.HOUR_OF_DAY, 12)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}
