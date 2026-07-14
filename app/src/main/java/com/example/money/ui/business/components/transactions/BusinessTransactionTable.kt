package com.example.money.ui.business.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.ui.util.Formatters

@Composable
fun BusinessTransactionTable(
    transactions: List<BusinessTransaction>,
    emptyText: String = stringResource(R.string.business_empty_cashflow),
    modifier: Modifier = Modifier,
) {
    if (transactions.isEmpty()) {
        Text(emptyText, modifier = modifier.padding(24.dp))
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(transactions, key = { it.id }) { tx ->
            BusinessTransactionRow(transaction = tx)
        }
    }
}

@Composable
private fun BusinessTransactionRow(transaction: BusinessTransaction) {
    val sign = if (transaction.type == BusinessTransactionType.INCOME) "+" else "−"
    val typeLabel = if (transaction.type == BusinessTransactionType.INCOME) {
        stringResource(R.string.business_tx_income)
    } else {
        stringResource(R.string.business_tx_expense)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                transaction.note.ifBlank { transaction.category },
                fontWeight = FontWeight.Medium,
            )
            Text(
                "$typeLabel • $sign${Formatters.currencyFromCents(transaction.amountCents)} • ${Formatters.shortDate(transaction.date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${transaction.category} • ${transaction.paymentMethod.name}",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
