package com.example.money.ui.business.components.invoices

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.business.theme.BusinessColors

@Composable
fun InvoiceStatusBadge(
    status: InvoiceStatus,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) {
    val (label, color) = when (status) {
        InvoiceStatus.DRAFT -> stringResource(R.string.business_invoice_status_draft) to BusinessColors.Gray500
        InvoiceStatus.SENT -> stringResource(R.string.business_invoice_status_sent) to BusinessColors.PrimaryBlue
        InvoiceStatus.PAID -> stringResource(R.string.business_invoice_status_paid) to BusinessColors.ProfitGreen
        InvoiceStatus.OVERDUE -> stringResource(R.string.business_invoice_status_overdue) to BusinessColors.LossRed
    }
    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(labelColor = color),
    )
}
