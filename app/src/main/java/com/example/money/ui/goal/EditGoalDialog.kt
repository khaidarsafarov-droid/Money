package com.example.money.ui.goal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.ui.components.MoneyTextField
import com.example.money.ui.util.InputValidators

private fun formatAmountForInput(amount: Double): String {
    if (amount <= 0) return ""
    return if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()
}

@Composable
fun EditGoalDialog(
    currentTarget: Double,
    onDismiss: () -> Unit,
    onConfirm: (targetAmount: Double) -> Unit,
) {
    var amountText by remember {
        mutableStateOf(formatAmountForInput(currentTarget))
    }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_set_goal)) },
        text = {
            MoneyTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = stringResource(R.string.action_set_goal),
                isError = amountError != null,
                supportingText = amountError,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountError = InputValidators.validateTargetAmount(amountText)
                    if (amountError != null) return@TextButton
                    val amount = InputValidators.parseAmount(amountText) ?: return@TextButton
                    onConfirm(amount)
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
