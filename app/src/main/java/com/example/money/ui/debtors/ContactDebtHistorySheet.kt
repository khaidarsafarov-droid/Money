package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.ContactDebtHistoryEngine
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDebtHistorySheet(
    lookupKey: String,
    displayName: String,
    allDebtors: List<Debtor>,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
    onOpenDebt: (Debtor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val history = remember(lookupKey, allDebtors) {
        ContactDebtHistoryEngine.aggregate(allDebtors, lookupKey, displayName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = GoldPrimary)
                Text(
                    text = stringResource(R.string.contact_history_title, history.displayName),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CreamSurface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.contact_history_active_summary,
                            history.activeDebtCount,
                            Formatters.currencyFromCents(history.activeRemainingCents, hide = hideAmounts),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.contact_history_closed_summary, history.closedDebtCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = stringResource(R.string.contact_history_debts_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (history.debts.isEmpty()) {
                Text(
                    text = stringResource(R.string.contact_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                history.debts.forEach { debtor ->
                    Card(
                        onClick = { onOpenDebt(debtor) },
                        colors = CardDefaults.cardColors(containerColor = CreamSurface),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = when (debtor.debtType) {
                                        DebtType.CREDITOR -> stringResource(R.string.contact_history_owes_me)
                                        DebtType.DEBTOR -> stringResource(R.string.contact_history_i_owe)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldPrimary,
                                )
                                Text(
                                    text = if (debtor.isPaid) {
                                        stringResource(R.string.debtor_paid)
                                    } else {
                                        stringResource(R.string.debt_status_active)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (debtor.isPaid) FinancePositive else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = Formatters.currencyFromCents(debtor.targetAmountCents, hide = hideAmounts),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.contact_history_debt_dates,
                                    Formatters.shortDate(debtor.dateGiven),
                                    Formatters.shortDate(debtor.dateDue),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!debtor.isPaid) {
                                Text(
                                    text = stringResource(
                                        R.string.debtor_remaining,
                                        Formatters.currencyFromCents(debtor.remainingAmountCents, hide = hideAmounts),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            if (history.payments.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.contact_history_payments_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                history.payments.take(20).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Formatters.currencyFromCents(entry.payment.amountCents, hide = hideAmounts),
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = Formatters.shortDate(entry.payment.paidAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = when (entry.debtType) {
                                DebtType.CREDITOR -> stringResource(R.string.contact_history_owes_me)
                                DebtType.DEBTOR -> stringResource(R.string.contact_history_i_owe)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}
