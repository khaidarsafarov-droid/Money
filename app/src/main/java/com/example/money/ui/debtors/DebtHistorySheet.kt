package com.example.money.ui.debtors

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.example.money.ui.theme.finoraBalanceBrush
import com.example.money.ui.theme.finoraInitials
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.ui.motion.MoneyInWindowSheet
import com.example.money.ui.motion.MotionGlyph
import com.example.money.ui.motion.MotionGlyphIcon
import com.example.money.ui.motion.moneySheetMotion
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.util.MoneyHaptic
import com.example.money.ui.util.moneyHaptic
import com.example.money.ui.util.systemMotionEnabled
import com.example.money.ui.util.vibrateLight
import com.example.money.R
import com.example.money.data.sync.DebtAdvanceIds
import com.example.money.domain.model.DebtAdvance
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.MoneyMath
import com.example.money.ui.components.AmountKeypad
import com.example.money.ui.components.DatePickerField
import com.example.money.ui.components.applyAmountKey
import com.example.money.ui.theme.LocalMoneyExtraColors
import com.example.money.ui.theme.MoneyTypeScale
import com.example.money.ui.util.Formatters
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.hideAmountKeypad
import com.example.money.ui.components.imeTextField
import com.example.money.ui.components.rememberTextFocus
import com.example.money.ui.util.InputValidators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtHistorySheet(
    debtor: Debtor,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
    onAddAdvance: (amountCents: Long, givenAt: Long, note: String) -> Unit,
    onAddPayment: (amountCents: Long, paidAt: Long) -> Unit,
    inWindow: Boolean = false,
) {
    var form by remember(debtor.id) { mutableStateOf(HistoryForm.None) }
    val entries = remember(debtor.id, debtor.advances, debtor.paymentHistory, debtor.targetAmountCents, debtor.dateGiven) {
        historyEntries(debtor)
    }

    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val motionEnabled = systemMotionEnabled(context)
    val returned = (debtor.targetAmountCents - debtor.remainingAmountCents).coerceAtLeast(0L)
    val progress = if (debtor.targetAmountCents <= 0L) {
        0f
    } else {
        returned.toFloat() / debtor.targetAmountCents.toFloat()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val body: @Composable () -> Unit = {
        if (!inWindow) DialogImeEffect()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .fillMaxHeight(if (inWindow) 1f else 0.96f)
                .then(if (inWindow) Modifier else Modifier.moneySheetMotion(onDismiss)),
        ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = debtor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (debtor.phone.isNotBlank()) {
                    IconButton(onClick = { DebtContactActions.dialPhone(context, debtor.phone) }) {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = stringResource(R.string.debt_action_call),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(finoraBalanceBrush())
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = finoraInitials(debtor.name), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = stringResource(if (debtor.debtType == com.example.money.domain.model.DebtType.CREDITOR) R.string.debt_dashboard_owes_me else R.string.debt_dashboard_i_owe),
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency, hide = hideAmounts),
                    color = Color.White,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                if (debtor.dateDue > 0L) {
                    Text(
                        text = stringResource(R.string.debt_until, Formatters.dueDateCompact(debtor.dateDue)),
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.debt_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val ordered = entries.sortedByDescending { it.at }
                var previousMonth = ""
                var previousDay = ""
                ordered.forEachIndexed { index, entry ->
                    val month = Formatters.monthYear(entry.at)
                    val day = Formatters.dueDateCompact(entry.at)
                    if (month != previousMonth) {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        previousMonth = month
                        previousDay = ""
                    }
                    if (day != previousDay) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        previousDay = day
                    }
                    HistoryRow(entry = entry, currency = debtor.currency, hideAmounts = hideAmounts)
                    val nextDay = ordered.getOrNull(index + 1)?.let { Formatters.dueDateCompact(it.at) }
                    if (nextDay == day) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
            AnimatedContent(
                targetState = form,
                transitionSpec = {
                    if (!motionEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(
                            tween(MoneyMotion.fadeIn, easing = MoneyMotion.emphasizedDecelerate),
                        ) togetherWith fadeOut(
                            tween(MoneyMotion.fadeOut, easing = MoneyMotion.emphasizedAccelerate),
                        )
                    }
                },
                label = "debtHistoryForm",
            ) { current ->
                when (current) {
                    HistoryForm.Advance -> AdvanceForm(
                        onCancel = { form = HistoryForm.None },
                        onSave = { amountCents, givenAt, note ->
                            view.moneyHaptic(MoneyHaptic.Confirm)
                            onAddAdvance(amountCents, givenAt, note)
                            form = HistoryForm.None
                        },
                    )
                    HistoryForm.Payment -> PaymentForm(
                        remainingCents = debtor.remainingAmountCents,
                        onCancel = { form = HistoryForm.None },
                        onSave = { amountCents, paidAt ->
                            view.moneyHaptic(MoneyHaptic.Confirm)
                            onAddPayment(amountCents, paidAt)
                            form = HistoryForm.None
                        },
                    )
                    HistoryForm.None -> HistoryActions(
                        onTook = { form = HistoryForm.Advance },
                        onReturned = { form = HistoryForm.Payment },
                    )
                }
            }
            }
        }
    }
    if (inWindow) {
        MoneyInWindowSheet(onDismiss = onDismiss) { body() }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            contentWindowInsets = { BottomSheetDefaults.windowInsets },
        ) { body() }
    }
}

@Composable
private fun HistoryRow(
    entry: DebtHistoryEntry,
    currency: String,
    hideAmounts: Boolean,
) {
    val took = entry.kind == DebtHistoryKind.Advance
    val scheme = MaterialTheme.colorScheme
    val extra = LocalMoneyExtraColors.current
    val formatted = Formatters.currencyFromCents(entry.amountCents, currency, hide = hideAmounts)
    val signed = signedHistoryAmount(formatted, took = took, hidden = hideAmounts)
    val title = stringResource(if (took) R.string.debt_history_took else R.string.debt_history_returned)
    val spoken = stringResource(
        if (took) R.string.debt_history_advance else R.string.debt_history_payment,
        signed,
    )
    val accent = if (took) extra.negative else extra.positive
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (took) Color(0xFFE2725B) else Color(0xFF2FA884)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (took) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = Formatters.date(entry.at),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Text(
                text = signed,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }
        if (entry.note.isNotBlank()) {
            Text(
                text = entry.note,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 52.dp),
            )
        }
    }
}

internal fun signedHistoryAmount(formatted: String, took: Boolean, hidden: Boolean): String {
    if (hidden) return formatted
    return if (took) "− $formatted" else "+ $formatted"
}

private enum class HistoryForm { None, Advance, Payment }

@Composable
private fun HistoryActions(
    onTook: () -> Unit,
    onReturned: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tookFill = historyTookFill(isSystemInDarkTheme())
    val tookContainer = if (tookFill == HistoryTookFill.ErrorContainer) scheme.errorContainer else scheme.error
    val tookContent = if (tookFill == HistoryTookFill.ErrorContainer) scheme.onErrorContainer else scheme.onError
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onTook,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tookContainer,
                contentColor = tookContent,
            ),
        ) {
            MotionGlyphIcon(MotionGlyph.Took, tint = tookContent)
            Text(stringResource(R.string.debt_history_took))
        }
        Button(
            onClick = onReturned,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
            ),
        ) {
            MotionGlyphIcon(MotionGlyph.Returned, tint = scheme.onPrimary)
            Text(stringResource(R.string.debt_history_returned))
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
    val noteFocus = rememberTextFocus()
    val positiveError = stringResource(R.string.error_amount_positive)
    val keypadHidden = hideAmountKeypad(noteFocus.focused)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (amountText.isBlank()) "0" else amountText,
            style = MoneyTypeScale.amountInput,
            color = if (amountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
        amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!keypadHidden) {
            AmountKeypad(onKey = { amountText = applyAmountKey(amountText, it); amountError = null })
        }
        DatePickerField(
            label = stringResource(R.string.debt_advance_date_label),
            selectedDateMillis = givenAt,
            onDateSelected = { givenAt = it },
        )
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.debt_advance_note_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .imeTextField(noteFocus),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
            var saved by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    val cents = InputValidators.parseAmount(amountText)
                    if (cents == null || cents <= 0L) {
                        amountError = positiveError
                    } else {
                        saved = true
                        onSave(cents, givenAt, note.trim())
                    }
                },
            ) {
                if (saved) {
                    MotionGlyphIcon(MotionGlyph.Check, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@Composable
private fun PaymentForm(
    remainingCents: Long,
    onCancel: () -> Unit,
    onSave: (amountCents: Long, paidAt: Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var paidAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var amountError by remember { mutableStateOf<String?>(null) }
    val positiveError = stringResource(R.string.error_amount_positive)
    val exceedsError = stringResource(R.string.error_payment_exceeds_remaining)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (amountText.isBlank()) "0" else amountText,
            style = MoneyTypeScale.amountInput,
            color = if (amountError != null) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        amountError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FilterChip(
            selected = false,
            onClick = {
                amountText = formatAmountInput(remainingCents)
                amountError = null
            },
            label = { Text(stringResource(R.string.debt_history_pay_all)) },
        )
        AmountKeypad(onKey = { key ->
            amountText = applyAmountKey(amountText, key)
            amountError = null
        })
        DatePickerField(
            label = stringResource(R.string.debt_advance_date_label),
            selectedDateMillis = paidAt,
            onDateSelected = { paidAt = it },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
            var saved by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    val cents = InputValidators.parseAmount(amountText)
                    when {
                        cents == null || cents <= 0L -> amountError = positiveError
                        cents > remainingCents -> amountError = exceedsError
                        else -> {
                            saved = true
                            onSave(cents, paidAt)
                        }
                    }
                },
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                if (saved) {
                    MotionGlyphIcon(MotionGlyph.Check, tint = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

private fun formatAmountInput(cents: Long): String {
    val amount = MoneyMath.fromCents(cents)
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
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
