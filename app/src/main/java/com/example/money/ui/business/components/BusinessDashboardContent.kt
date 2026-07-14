package com.example.money.ui.business.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardFilters
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.theme.FinanceInfo
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters

@Composable
fun BusinessDashboardContent(
    dashboard: BusinessDashboardSummary,
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    filters: BusinessDashboardFilters,
    onFiltersChange: (BusinessDashboardFilters) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DashboardFilterRow(
                clients = clients,
                projects = projects,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onClearFilters = onClearFilters,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BusinessKpiCard(
                    title = stringResource(R.string.business_metric_revenue),
                    amountCents = dashboard.monthRevenueCents,
                    accentColor = FinancePositive,
                    changePercent = dashboard.revenueChangePercent,
                    sparklineValues = dashboard.revenueSparkline,
                    modifier = Modifier.weight(1f),
                )
                BusinessKpiCard(
                    title = stringResource(R.string.business_metric_expenses),
                    amountCents = dashboard.monthExpenseCents,
                    accentColor = FinanceNegative,
                    changePercent = dashboard.expenseChangePercent,
                    sparklineValues = dashboard.expenseSparkline,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BusinessKpiCard(
                    title = stringResource(R.string.business_metric_profit),
                    amountCents = dashboard.monthNetProfitCents,
                    accentColor = FinanceInfo,
                    changePercent = dashboard.profitChangePercent,
                    sparklineValues = dashboard.profitSparkline,
                    modifier = Modifier.weight(1f),
                )
                BusinessKpiCard(
                    title = stringResource(R.string.business_metric_cash_on_hand),
                    amountCents = dashboard.cashOnHandCents,
                    accentColor = GoldPrimary,
                    changePercent = dashboard.cashChangePercent,
                    sparklineValues = dashboard.cashSparkline,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            CashflowLineChart(
                incomeValues = dashboard.cashflowWeekly.map { it.incomeCents },
                expenseValues = dashboard.cashflowWeekly.map { it.expenseCents },
                labels = dashboard.cashflowWeekly.map { it.label },
            )
        }

        item {
            ProfitBarChart(
                values = dashboard.monthlyProfit.map { it.profitCents },
                labels = dashboard.monthlyProfit.map { it.label },
            )
        }

        if (dashboard.clientTable.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_top_clients_table)) }
            items(dashboard.clientTable) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(row.clientName, fontWeight = FontWeight.SemiBold)
                        Text("${stringResource(R.string.business_col_revenue)}: ${Formatters.currencyFromCents(row.revenueCents)}")
                        Text("${stringResource(R.string.business_col_profit)}: ${Formatters.currencyFromCents(row.profitCents)}")
                        row.lastInvoiceStatus?.let { status ->
                            Text("${stringResource(R.string.business_col_invoice_status)}: ${status.name}")
                        }
                    }
                }
            }
        }

        if (dashboard.projectCards.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_project_performance)) }
            items(dashboard.projectCards) { project ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(project.projectName, fontWeight = FontWeight.SemiBold)
                        Text("${Formatters.currencyFromCents(project.actualCents)} / ${Formatters.currencyFromCents(project.budgetCents)}")
                        Text(project.status.name, style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { project.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (dashboard.overdueInvoices.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_overdue_invoices)) }
            items(dashboard.overdueInvoices) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(row.clientName, fontWeight = FontWeight.SemiBold)
                        Text(Formatters.currencyFromCents(row.amountCents), color = FinanceNegative)
                        Text(stringResource(R.string.business_days_overdue, row.daysOverdue))
                    }
                }
            }
        }

        if (dashboard.upcomingPayments.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_upcoming_payments)) }
            items(dashboard.upcomingPayments) { row ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(row.clientName, fontWeight = FontWeight.SemiBold)
                        Text(Formatters.currencyFromCents(row.amountCents))
                        Text(Formatters.shortDate(row.dueDate))
                    }
                }
            }
        }

        if (dashboard.insights.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_insights_title)) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dashboard.insights.forEach { insight ->
                            Text("• $insight", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardFilterRow(
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    filters: BusinessDashboardFilters,
    onFiltersChange: (BusinessDashboardFilters) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.business_filters), fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filters.clientId == null,
                    onClick = { onFiltersChange(filters.copy(clientId = null)) },
                    label = { Text(stringResource(R.string.business_filter_all_clients)) },
                )
            }
            items(clients.take(6)) { client ->
                FilterChip(
                    selected = filters.clientId == client.id,
                    onClick = { onFiltersChange(filters.copy(clientId = client.id)) },
                    label = { Text(client.name) },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filters.invoiceStatus == null,
                    onClick = { onFiltersChange(filters.copy(invoiceStatus = null)) },
                    label = { Text(stringResource(R.string.business_filter_all_invoices)) },
                )
            }
            items(InvoiceStatus.entries) { status ->
                FilterChip(
                    selected = filters.invoiceStatus == status,
                    onClick = { onFiltersChange(filters.copy(invoiceStatus = status)) },
                    label = { Text(status.name) },
                )
            }
        }
        if (filters.clientId != null || filters.projectId != null || filters.invoiceStatus != null) {
            FilterChip(
                selected = false,
                onClick = onClearFilters,
                label = { Text(stringResource(R.string.business_filter_clear)) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
}
