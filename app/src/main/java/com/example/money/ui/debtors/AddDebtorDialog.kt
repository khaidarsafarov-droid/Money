package com.example.money.ui.debtors

import android.Manifest
import android.net.Uri
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
import com.example.money.messaging.ContactInfoResolver
import com.example.money.messaging.SmsPermissions
import com.example.money.domain.model.DebtType
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
    @Suppress("UNUSED_PARAMETER") availableBalance: Long,
    allDebtors: List<Debtor> = emptyList(),
    onDismiss: () -> Unit,
    onViewContactHistory: ((String, String) -> Unit)? = null,
    onConfirm: (
        name: String,
        amountCents: Long,
        dateGiven: Long,
        dateDue: Long,
        affectsBalance: Boolean,
        note: String,
        loanCategory: LoanCategory,
        interestRateBps: Int,
        contactLookupKey: String?,
        contactUri: String?,
        contactPhotoUri: String?,
        phone: String,
        email: String,
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
        mutableStateOf(existingDebtor?.affectsBalance ?: false)
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
            existingDebtor?.interestRateBps?.let(InputValidators::formatBasisPoints).orEmpty(),
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
    var phone by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.phone.orEmpty())
    }
    var email by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.email.orEmpty())
    }

    val context = LocalContext.current

    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    val amountPositiveError = stringResource(R.string.error_amount_positive)
    val amountBelowPaidError = stringResource(R.string.error_amount_below_paid)
    var dateError by remember { mutableStateOf<String?>(null) }
    var interestError by remember { mutableStateOf<String?>(null) }

    val canSubmit = remember(name, amountText, dateGiven, dateDue, interestRateText, isEdit, existingDebtor) {
        InputValidators.validateName(name) == null &&
            InputValidators.validateAmount(amountText) == null &&
            InputValidators.validateDueDate(dateGiven, dateDue) == null &&
            InputValidators.validatePercent(interestRateText) == null &&
            if (isEdit) {
                val amount = InputValidators.parseAmount(amountText)
                amount == null || amount >= (existingDebtor?.currentAmountCents ?: 0L)
            } else {
                true
            }
    }

    var pendingContactUri by remember { mutableStateOf<Uri?>(null) }

    fun applyPickedContactDetails(uri: Uri): String {
        val info = ContactInfoResolver.resolve(context, uri)
        val resolvedPhone = info?.phone?.takeIf { it.isNotBlank() }
        val resolvedEmail = info?.email?.takeIf { it.isNotBlank() }
        if (resolvedPhone != null) phone = resolvedPhone
        if (resolvedEmail != null) email = resolvedEmail
        return resolvedPhone ?: phone
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val uri = pendingContactUri
        pendingContactUri = null
        if (granted && uri != null) {
            applyPickedContactDetails(uri)
        }
    }

    fun applyPickedContact(uri: Uri) {
        ContactResolver.resolve(context, uri)?.let { contact ->
            pickedContact = contact
            name = contact.displayName
            nameError = null
        }
        val filledPhone = applyPickedContactDetails(uri)
        if (filledPhone.isBlank() && !SmsPermissions.canReadContacts(context)) {
            pendingContactUri = uri
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) {
            applyPickedContact(uri)
        }
    }

    val existingContactDebtCount = remember(pickedContact?.lookupKey, allDebtors) {
        pickedContact?.lookupKey?.let { key ->
            ContactDebtHistoryEngine.countExistingDebts(allDebtors, key)
        } ?: 0
    }

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val editingDebtor = existingDebtor
            Text(
                if (editingDebtor != null) {
                    stringResource(R.string.debtor_edit_title, editingDebtor.name)
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
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.debtor_phone_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.debtor_email_label)) },
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
                if (isEdit && existingDebtor.currentAmountCents > 0L) {
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
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = {
                            interestRateText = it
                            interestError = null
                        },
                        label = { Text(stringResource(R.string.loan_interest_rate_label)) },
                        singleLine = true,
                        isError = interestError != null,
                        supportingText = interestError?.let { message -> { Text(message) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = {
                    nameError = InputValidators.validateName(name)?.let(context::getString)
                    amountError = InputValidators.validateAmount(amountText)?.let(context::getString)
                    if (amountError == null) {
                        val parsedAmount = InputValidators.parseAmount(amountText)
                        if (parsedAmount != null && parsedAmount <= 0L) {
                            amountError = amountPositiveError
                        }
                    }
                    dateError = InputValidators.validateDueDate(dateGiven, dateDue)?.let(context::getString)
                    interestError = InputValidators.validatePercent(interestRateText)?.let(context::getString)

                    if (isEdit && amountError == null) {
                        val amount = InputValidators.parseAmount(amountText)
                        if (amount != null) {
                            val targetCents = amount
                            if (targetCents < existingDebtor.currentAmountCents) {
                                amountError = amountBelowPaidError
                            }
                        }
                    }

                    if (nameError != null || amountError != null || dateError != null || interestError != null) {
                        return@TextButton
                    }

                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    val interestRateBps = InputValidators.parsePercent(interestRateText) ?: return@TextButton
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
                        phone.trim(),
                        email.trim(),
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
