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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import com.example.money.ui.motion.MotionGlyph
import com.example.money.ui.motion.MotionGlyphIcon
import com.example.money.ui.motion.moneySheetMotion
import com.example.money.ui.util.MoneyHaptic
import com.example.money.ui.util.moneyHaptic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.money.ui.components.AmountKeypad
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.applyAmountKey
import com.example.money.ui.components.hideAmountKeypad
import com.example.money.ui.components.imeTextField
import com.example.money.ui.components.rememberTextFocus
import com.example.money.ui.theme.MoneyTypeScale
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators

private fun formatAmountForInput(amount: Double): String {
    if (amount <= 0) return ""
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtorFormDialog(
    debtType: DebtType,
    existingDebtor: Debtor? = null,
    @Suppress("UNUSED_PARAMETER") availableBalance: Long,
    allDebtors: List<Debtor> = emptyList(),
    onDismiss: () -> Unit,
    onViewContactHistory: ((String, String) -> Unit)? = null,
    onDebtTypeChange: (DebtType) -> Unit = {},
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
    var selectedType by remember(existingDebtor?.id, debtType) {
        mutableStateOf(existingDebtor?.debtType ?: debtType)
    }
    val resolvedType = selectedType
    var showDetails by remember(existingDebtor?.id) { mutableStateOf(isEdit) }
    var showDue by remember(existingDebtor?.id) { mutableStateOf(true) }
    var showNote by remember(existingDebtor?.id) {
        mutableStateOf(isEdit && !existingDebtor?.note.isNullOrBlank())
    }

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val textFocus = rememberTextFocus()
    val keypadHidden = hideAmountKeypad(textFocus.focused)
    val editingDebtor = existingDebtor
    val title = if (editingDebtor != null) {
        stringResource(R.string.debtor_edit_title, editingDebtor.name)
    } else {
        stringResource(titleRes)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { BottomSheetDefaults.windowInsets },
    ) {
        DialogImeEffect()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .moneySheetMotion(onDismiss)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (!isEdit) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(DebtType.CREDITOR, DebtType.DEBTOR).forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = resolvedType == type,
                            onClick = {
                                selectedType = type
                                onDebtTypeChange(type)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            icon = {},
                            label = {
                                Text(
                                    stringResource(
                                        if (type == DebtType.CREDITOR) {
                                            R.string.debt_dashboard_owes_me
                                        } else {
                                            R.string.debt_dashboard_i_owe
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
            Text(
                text = if (amountText.isBlank()) "0" else amountText,
                style = MoneyTypeScale.amountInput,
                color = if (amountError != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            amountError?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
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
                    modifier = Modifier
                        .weight(1f)
                        .imeTextField(textFocus),
                )
                IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Outlined.Contacts,
                        contentDescription = stringResource(R.string.contact_pick_action),
                    )
                }
            }
            pickedContact?.let { contact ->
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (onViewContactHistory != null && existingContactDebtCount > 0) {
                val lookupKey = pickedContact?.lookupKey
                TextButton(
                    onClick = {
                        if (lookupKey != null) onViewContactHistory(lookupKey, name.trim())
                    },
                ) {
                    Text(stringResource(R.string.contact_history_short, existingContactDebtCount))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = false,
                    onClick = { dateGiven = System.currentTimeMillis() },
                    label = { Text(stringResource(R.string.debt_chip_today)) },
                )
                FilterChip(
                    selected = showDue,
                    onClick = { showDue = !showDue },
                    label = { Text(stringResource(R.string.debt_chip_due)) },
                )
                FilterChip(
                    selected = showNote,
                    onClick = { showNote = !showNote },
                    label = { Text(stringResource(R.string.debt_note_label)) },
                )
                FilterChip(
                    selected = showDetails,
                    onClick = { showDetails = !showDetails },
                    label = { Text(stringResource(R.string.action_more)) },
                )
            }
            if (showDue) {
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
            }
            if (showNote) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.debt_note_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .imeTextField(textFocus),
                )
            }
            if (showDetails) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.debtor_phone_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imeTextField(textFocus),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.debtor_email_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imeTextField(textFocus),
                )
                if (isEdit && existingDebtor.currentAmountCents > 0L) {
                    Text(
                        text = stringResource(
                            R.string.debtor_edit_paid_hint,
                            formatAmountForInput(MoneyMath.fromCents(existingDebtor.currentAmountCents)),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (resolvedType == DebtType.DEBTOR) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .imeTextField(textFocus),
                    )
                }
            }
            if (!keypadHidden) {
                AmountKeypad(
                    onKey = { key ->
                        amountText = applyAmountKey(amountText, key)
                        amountError = null
                    },
                )
            }
            val saveView = androidx.compose.ui.platform.LocalView.current
            var saved by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            Button(
                enabled = canSubmit && !saved,
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
                        if (amount != null && amount < existingDebtor.currentAmountCents) {
                            amountError = amountBelowPaidError
                        }
                    }
                    if (nameError != null || amountError != null || dateError != null || interestError != null) {
                        if (dateError != null) showDue = true
                        if (interestError != null) showDetails = true
                        saveView.moneyHaptic(MoneyHaptic.Reject)
                        return@Button
                    }
                    val amount = InputValidators.parseAmount(amountText) ?: return@Button
                    val interestRateBps = InputValidators.parsePercent(interestRateText) ?: return@Button
                    onDebtTypeChange(resolvedType)
                    saved = true
                    saveView.moneyHaptic(MoneyHaptic.Confirm)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                if (saved) {
                    MotionGlyphIcon(MotionGlyph.Check, tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
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
