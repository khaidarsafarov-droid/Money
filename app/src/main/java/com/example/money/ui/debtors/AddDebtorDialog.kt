package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import com.example.money.contacts.ContactResolver
import com.example.money.contacts.PickedContact
import com.example.money.domain.model.LoanCategory
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.ContactDebtHistoryEngine
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
    allDebtors: List<Debtor> = emptyList(),
    onDismiss: () -> Unit,
    onViewContactHistory: ((String, String) -> Unit)? = null,
    onConfirm: (
        name: String,
        amount: Double,
        dateGiven: Long,
        dateDue: Long,
        affectsBalance: Boolean,
        note: String,
        loanCategory: LoanCategory,
        interestRateBps: Int,
        contactLookupKey: String?,
        contactUri: String?,
        contactPhotoUri: String?,
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
    var loanCategory by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.loanCategory ?: LoanCategory.OTHER)
    }
    var interestRateText by remember(existingDebtor?.id, debtType) {
        mutableStateOf(
            existingDebtor?.interestRateBps?.let { if (it > 0) (it / 100.0).toString() else "" }
                .orEmpty(),
        )
    }
    var pickedContact by remember(existingDebtor?.id, debtType) {
        mutableStateOf(
            existingDebtor?.contactLookupKey?.let { key ->
                PickedContact(
                    displayName = existingDebtor.name,
                    lookupKey = key,
                    contactUri = existingDebtor.contactUri.orEmpty(),
                    photoUri = existingDebtor.contactPhotoUri,
                )
            },
        )
    }

    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) {
            ContactResolver.resolve(context, uri)?.let { contact ->
                pickedContact = contact
                name = contact.displayName
                nameError = null
            }
        }
    }

    val existingContactDebtCount = remember(pickedContact?.lookupKey, allDebtors) {
        pickedContact?.lookupKey?.let { key ->
            ContactDebtHistoryEngine.countExistingDebts(allDebtors, key)
        } ?: 0
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
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
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Outlined.Contacts,
                            contentDescription = stringResource(R.string.contact_pick_action),
                        )
                    }
                }
                pickedContact?.let { contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.contact_linked_hint, contact.displayName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        if (existingContactDebtCount > 0 && onViewContactHistory != null) {
                            TextButton(
                                onClick = { onViewContactHistory(contact.lookupKey, contact.displayName) },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 4.dp),
                                )
                                Text(stringResource(R.string.contact_history_short, existingContactDebtCount))
                            }
                        }
                    }
                }
                if (existingContactDebtCount > 0 && pickedContact != null) {
                    Text(
                        text = stringResource(R.string.contact_existing_debts_hint, existingContactDebtCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                if (resolvedType == DebtType.DEBTOR) {
                    Text(
                        text = stringResource(R.string.loan_category_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoanCategory.entries.forEach { category ->
                            FilterChip(
                                selected = loanCategory == category,
                                onClick = { loanCategory = category },
                                label = { Text(loanCategoryLabel(category)) },
                            )
                        }
                    }
                    if (loanCategory.isHighPriority) {
                        Text(
                            text = stringResource(R.string.loan_category_priority_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it },
                        label = { Text(stringResource(R.string.loan_interest_rate_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                    val interestRateBps = interestRateText.toDoubleOrNull()
                        ?.let { (it * 100).toInt().coerceAtLeast(0) } ?: 0
                    onConfirm(
                        name.trim(),
                        amount,
                        dateGiven,
                        dateDue,
                        affectsBalance,
                        note.trim(),
                        loanCategory,
                        interestRateBps,
                        pickedContact?.lookupKey,
                        pickedContact?.contactUri,
                        pickedContact?.photoUri,
                    )
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
    allDebtors: List<Debtor> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        amount: Double,
        dateGiven: Long,
        dateDue: Long,
        affectsBalance: Boolean,
        note: String,
        loanCategory: LoanCategory,
        interestRateBps: Int,
        contactLookupKey: String?,
        contactUri: String?,
        contactPhotoUri: String?,
    ) -> Unit,
) = DebtorFormDialog(
    debtType = debtType,
    existingDebtor = null,
    availableBalance = availableBalance,
    allDebtors = allDebtors,
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
