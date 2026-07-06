package com.example.money.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.money.R

@Composable
fun QuickAddDialog(
    hasActiveDebts: Boolean,
    onDismiss: () -> Unit,
    onDeposit: () -> Unit,
    onExpense: () -> Unit,
    onDebtPayment: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_add_title)) },
        text = {
            Column {
                TextButton(onClick = {
                    onDismiss()
                    onDeposit()
                }) {
                    Text(stringResource(R.string.quick_add_deposit))
                }
                TextButton(onClick = {
                    onDismiss()
                    onExpense()
                }) {
                    Text(stringResource(R.string.quick_add_expense))
                }
                if (hasActiveDebts) {
                    TextButton(onClick = {
                        onDismiss()
                        onDebtPayment()
                    }) {
                        Text(stringResource(R.string.quick_add_debt_payment))
                    }
                }
            }
        },
        confirmButton = {},
    )
}
