package com.example.money.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.settings.CurrencyRegion
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.InputValidators
import com.example.money.ui.util.vibrateLight

private val QUICK_AMOUNT_CENTS = longArrayOf(5_000L, 10_000L, 50_000L)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickPaymentSheet(
    remainingCents: Long,
    currencyRegion: CurrencyRegion,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val amountRequired = stringResource(R.string.error_amount_required)
    val amountPositive = stringResource(R.string.error_amount_positive)
    val exceedsRemaining = stringResource(R.string.error_payment_exceeds_remaining)
    val context = LocalContext.current

    fun submit() {
        when (val validation = InputValidators.validateAmount(amountText)) {
            null -> Unit
            R.string.error_amount_required -> {
                errorText = amountRequired
                return
            }
            R.string.error_amount_positive -> {
                errorText = amountPositive
                return
            }
            else -> {
                errorText = amountPositive
                return
            }
        }
        val cents = InputValidators.parseAmount(amountText) ?: return
        if (cents > remainingCents) {
            errorText = exceedsRemaining
            return
        }
        onSave(cents)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_payment_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.debt_payment_remaining,
                    Formatters.currencyFromCents(remainingCents, currencyRegion),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            MoneyTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorText = null
                },
                label = stringResource(R.string.debt_payment_amount_label),
                isError = errorText != null,
                supportingText = errorText,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                focusRequester = focusRequester,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remainingCents > 0L) {
                    AssistChip(
                        onClick = {
                            vibrateLight(context)
                            amountText = centsToInput(remainingCents)
                            errorText = null
                        },
                        label = { Text(stringResource(R.string.quick_payment_remainder)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
                QUICK_AMOUNT_CENTS.forEach { cents ->
                    if (cents <= remainingCents) {
                        AssistChip(
                            onClick = {
                                vibrateLight(context)
                                amountText = centsToInput(cents)
                                errorText = null
                            },
                            label = { Text(Formatters.currencyFromCents(cents, currencyRegion)) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
            MoneyButton(
                text = stringResource(R.string.action_save),
                onClick = ::submit,
            )
        }
    }
}

internal fun centsToInput(cents: Long): String {
    val major = cents / 100
    val fraction = kotlin.math.abs(cents % 100)
    return if (fraction == 0L) major.toString() else "$major.${fraction.toString().padStart(2, '0')}"
}
