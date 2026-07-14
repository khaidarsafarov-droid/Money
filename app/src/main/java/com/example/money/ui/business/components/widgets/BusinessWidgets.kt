package com.example.money.ui.business.components.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import com.example.money.ui.business.components.tables.BusinessTable
import com.example.money.ui.business.components.tables.BusinessTableColumn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.ClientProfitRow
import com.example.money.domain.model.business.OverdueInvoiceRow
import com.example.money.domain.model.business.ProjectPerformanceRow
import com.example.money.domain.model.business.UpcomingPaymentRow
import com.example.money.ui.business.components.cards.BusinessCard
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing
import com.example.money.ui.util.Formatters

@Composable
fun AiInsightsWidget(
    insights: List<String>,
    modifier: Modifier = Modifier,
) {
    if (insights.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        RowWithIcon(
            icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = BusinessColors.PrimaryBlue) },
            title = stringResource(R.string.business_insights_title),
        )
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm), modifier = Modifier.padding(top = BusinessSpacing.sm)) {
            insights.forEach { insight ->
                Text("• $insight", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun OverdueInvoicesWidget(
    items: List<OverdueInvoiceRow>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        RowWithIcon(
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null, tint = BusinessColors.WarningYellow) },
            title = stringResource(R.string.business_overdue_invoices),
        )
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm), modifier = Modifier.padding(top = BusinessSpacing.sm)) {
            items.forEach { row ->
                Column {
                    Text(row.clientName, fontWeight = FontWeight.SemiBold)
                    Text(Formatters.currencyFromCents(row.amountCents), color = BusinessColors.LossRed)
                    Text(stringResource(R.string.business_days_overdue, row.daysOverdue), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun UpcomingPaymentsWidget(
    items: List<UpcomingPaymentRow>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        RowWithIcon(
            icon = { Icon(Icons.Outlined.Schedule, contentDescription = null, tint = BusinessColors.InvoiceBlue) },
            title = stringResource(R.string.business_upcoming_payments),
        )
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm), modifier = Modifier.padding(top = BusinessSpacing.sm)) {
            items.forEach { row ->
                Column {
                    Text(row.clientName, fontWeight = FontWeight.SemiBold)
                    Text(Formatters.currencyFromCents(row.amountCents))
                    Text(Formatters.shortDate(row.dueDate), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ProjectPerformanceWidget(
    projects: List<ProjectPerformanceRow>,
    modifier: Modifier = Modifier,
) {
    if (projects.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.business_project_performance), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md), modifier = Modifier.padding(top = BusinessSpacing.sm)) {
            projects.forEach { project ->
                Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.xs)) {
                    Text(project.projectName, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Formatters.currencyFromCents(project.actualCents)} / ${Formatters.currencyFromCents(project.budgetCents)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(project.status.name, style = MaterialTheme.typography.labelMedium, color = BusinessColors.Gray500)
                    LinearProgressIndicator(
                        progress = { project.progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = BusinessColors.PrimaryBlue,
                        trackColor = BusinessColors.Gray100,
                    )
                }
            }
        }
    }
}

@Composable
fun ClientProfitabilityWidget(
    title: String,
    clients: List<ClientProfitRow>,
    modifier: Modifier = Modifier,
) {
    if (clients.isEmpty()) return
    val columns = listOf(
        BusinessTableColumn(stringResource(R.string.business_field_name), 1.2f),
        BusinessTableColumn(stringResource(R.string.business_col_revenue), 1f),
        BusinessTableColumn(stringResource(R.string.business_col_profit), 1f),
        BusinessTableColumn(stringResource(R.string.business_col_invoice_status), 1f),
    )
    val rows = clients.map { row ->
        listOf(
            row.clientName,
            Formatters.currencyFromCents(row.revenueCents),
            Formatters.currencyFromCents(row.profitCents),
            row.lastInvoiceStatus?.name ?: "—",
        )
    }
    BusinessTable(
        title = title,
        columns = columns,
        rows = rows,
        modifier = modifier,
    )
}

@Composable
fun RecentTransactionsWidget(
    transactions: List<BusinessTransaction>,
    modifier: Modifier = Modifier,
) {
    if (transactions.isEmpty()) return
    BusinessCard(modifier = modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.business_recent_transactions),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
        )
        Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm), modifier = Modifier.padding(top = BusinessSpacing.sm)) {
            transactions.take(5).forEach { tx ->
                val typeLabel = if (tx.type == BusinessTransactionType.INCOME) {
                    stringResource(R.string.business_tx_income)
                } else {
                    stringResource(R.string.business_tx_expense)
                }
                val sign = if (tx.type == BusinessTransactionType.INCOME) "+" else "−"
                Column {
                    Text(
                        tx.note.ifBlank { "${tx.category}" },
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "$typeLabel • $sign${Formatters.currencyFromCents(tx.amountCents)} • ${Formatters.shortDate(tx.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BusinessColors.Gray500,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowWithIcon(
    icon: @Composable () -> Unit,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
    }
}
