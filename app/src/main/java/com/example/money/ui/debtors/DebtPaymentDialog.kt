package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
    availableBalance: Double,
    initialAmountCents: Long = 0L,
    hideAmounts: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, affectsBalance: Boolean) -> Unit,
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
    val defaultAffectsBalance = debtor.debtType == DebtType.CREDITOR
    var affectsBalance by remember(debtor.id) { mutableStateOf(defaultAffectsBalance) }

    val balanceCheckboxRes = when (debtor.debtType) {
        DebtType.CREDITOR -> R.string.debt_credit_to_budget
        DebtType.DEBTOR -> R.string.debt_deduct_from_budget
    }
    val paymentActionRes = when (debtor.debtType) {
        DebtType.CREDITOR -> R.string.action_add_payment
        DebtType.DEBTOR -> R.string.action_make_payment
    }

    val context = LocalContext.current
    val extraHintCents = 1_000L
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
                                Formatters.currencyCompact(availableBalance, hide = hideAmounts),
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
                    amountError = InputValidators.validateAmount(amountText)?.let(context::getString)
                    if (amountError != null) return@TextButton
                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    val paymentCents = MoneyMath.toCents(amount)
                    if (paymentCents > debtor.remainingAmountCents) {
                        amountError = context.getString(R.string.error_payment_exceeds_remaining)
                        return@TextButton
                    }
                    if (
                        affectsBalance &&
                        debtor.debtType == DebtType.DEBTOR &&
                        amount > availableBalance + 0.000_001
                    ) {
                        amountError = context.getString(R.string.msg_insufficient_funds)
                        return@TextButton
                    }
                    onConfirm(amount, affectsBalance)
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
