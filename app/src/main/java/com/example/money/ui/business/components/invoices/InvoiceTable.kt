package com.example.money.ui.business.components.invoices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.business.components.ai.InvoiceRiskBadge
import com.example.money.ui.util.Formatters

@Composable
fun InvoiceTable(
    invoices: List<BusinessInvoice>,
    clients: List<BusinessClient>,
    invoiceRisks: Map<Long, InvoiceRiskAssessment> = emptyMap(),
    onMarkPaid: (BusinessInvoice) -> Unit,
    emptyText: String = stringResource(R.string.business_empty_invoices),
    modifier: Modifier = Modifier,
) {
    if (invoices.isEmpty()) {
        Text(emptyText, modifier = modifier.padding(24.dp))
        return
    }
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        invoices.forEach { invoice ->
            val clientName = clients.find { it.id == invoice.clientId }?.name ?: "—"
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(clientName, fontWeight = FontWeight.SemiBold)
                        InvoiceStatusBadge(status = invoice.status)
                    }
                    Text(Formatters.currencyFromCents(invoice.amountCents))
                    Text(
                        "${stringResource(R.string.business_invoice_due)}: ${Formatters.shortDate(invoice.dueDate)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    InvoiceRiskBadge(assessment = invoiceRisks[invoice.id])
                    if (invoice.status != InvoiceStatus.PAID) {
                        TextButton(onClick = { onMarkPaid(invoice) }) {
                            Text(stringResource(R.string.business_mark_paid))
                        }
                    }
                }
            }
        }
    }
}
