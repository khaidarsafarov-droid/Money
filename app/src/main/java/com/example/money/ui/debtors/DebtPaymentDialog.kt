package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.money.domain.util.DebtExtraPaymentEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators

@Composable
fun DebtPaymentDialog(
    debtor: Debtor,
    @Suppress("UNUSED_PARAMETER") availableBalance: Long,
    initialAmountCents: Long = 0L,
    hideAmounts: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long, affectsBalance: Boolean) -> Unit,
) {
    var amountText by remember(debtor.id, initialAmountCents) {
        mutableStateOf(
            if (initialAmountCents > 0L) {
                MoneyMath.fromCents(initialAmountCents).let { amount ->
                    if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
                }
            } else {
                ""
            },
        )
    }
    var amountError by remember { mutableStateOf<String?>(null) }
    val paymentActionRes = when (debtor.debtType) {
        DebtType.CREDITOR -> R.string.action_add_payment
        DebtType.DEBTOR -> R.string.action_make_payment
    }

    val context = LocalContext.current
    val extraHintCents = DebtExtraPaymentEngine.SUGGESTED_EXTRA_CENTS
    val extraImpact = remember(debtor.id, debtor.remainingAmountCents) {
        DebtExtraPaymentEngine.calculateImpact(debtor, extraHintCents)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_payment_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(
                        R.string.debt_payment_remaining,
                        Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency, hide = hideAmounts),
                    ),
                )
                MoneyTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = stringResource(R.string.debt_payment_amount_label),
                    isError = amountError != null,
                    supportingText = amountError,
                )
                if (debtor.debtType == DebtType.DEBTOR && extraImpact.hint.isNotBlank()) {
                    Text(
                        text = extraImpact.hint,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(
                    onClick = {
                        amountText = MoneyMath.fromCents(debtor.remainingAmountCents).let { amount ->
                            if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
                        }
                        amountError = null
                    },
                ) {
                    Text(stringResource(R.string.debt_payment_max))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountError = InputValidators.validateAmount(amountText)?.let(context::getString)
                    if (amountError != null) return@TextButton
                    val amountCents = InputValidators.parseAmount(amountText) ?: return@TextButton
                    if (amountCents > debtor.remainingAmountCents) {
                        amountError = context.getString(R.string.error_payment_exceeds_remaining)
                        return@TextButton
                    }
                    onConfirm(amountCents, false)
                },
            ) {
                Text(stringResource(paymentActionRes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
