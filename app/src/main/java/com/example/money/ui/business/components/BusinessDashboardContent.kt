package com.example.money.ui.business.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardFilters
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.business.components.cards.BusinessKpiCard
import com.example.money.ui.business.components.charts.CashflowLineChart
import com.example.money.ui.business.components.charts.ProfitBarChart
import com.example.money.ui.business.components.widgets.AiInsightsWidget
import com.example.money.ui.business.components.widgets.ClientProfitabilityWidget
import com.example.money.ui.business.components.widgets.OverdueInvoicesWidget
import com.example.money.ui.business.components.widgets.ProjectPerformanceWidget
import com.example.money.ui.business.components.widgets.UpcomingPaymentsWidget
import com.example.money.ui.business.theme.BusinessKpiType
import com.example.money.ui.business.theme.BusinessSpacing

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
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        item {
            Text(
                stringResource(R.string.business_tab_dashboard),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        item {
            DashboardFilterRow(
                clients = clients,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onClearFilters = onClearFilters,
            )
        }

        item {
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                Row(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
                    BusinessKpiCard(
                        title = stringResource(R.string.business_metric_revenue),
                        amountCents = dashboard.monthRevenueCents,
                        type = BusinessKpiType.Revenue,
                        changePercent = dashboard.revenueChangePercent,
                        sparklineValues = dashboard.revenueSparkline,
                        modifier = Modifier.weight(1f),
                    )
                    BusinessKpiCard(
                        title = stringResource(R.string.business_metric_expenses),
                        amountCents = dashboard.monthExpenseCents,
                        type = BusinessKpiType.Expense,
                        changePercent = dashboard.expenseChangePercent,
                        sparklineValues = dashboard.expenseSparkline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
                BusinessKpiCard(
                    title = stringResource(R.string.business_metric_profit),
                    amountCents = dashboard.monthNetProfitCents,
                    type = BusinessKpiType.Profit,
                    changePercent = dashboard.profitChangePercent,
                    sparklineValues = dashboard.profitSparkline,
                    modifier = Modifier.weight(1f),
                )
                BusinessKpiCard(
                    title = stringResource(R.string.business_metric_cash_on_hand),
                    amountCents = dashboard.cashOnHandCents,
                    type = BusinessKpiType.Cash,
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
                title = stringResource(R.string.business_chart_cashflow),
            )
        }

        item {
            ProfitBarChart(
                values = dashboard.monthlyProfit.map { it.profitCents },
                labels = dashboard.monthlyProfit.map { it.label },
                title = stringResource(R.string.business_chart_pnl),
            )
        }

        item {
            ClientProfitabilityWidget(
                title = stringResource(R.string.business_top_clients_table),
                clients = dashboard.clientTable,
            )
        }

        item {
            ProjectPerformanceWidget(projects = dashboard.projectCards)
        }

        item {
            OverdueInvoicesWidget(items = dashboard.overdueInvoices)
        }

        item {
            UpcomingPaymentsWidget(items = dashboard.upcomingPayments)
        }

        item {
            AiInsightsWidget(insights = dashboard.insights)
        }
    }
}

@Composable
private fun DashboardFilterRow(
    clients: List<BusinessClient>,
    filters: BusinessDashboardFilters,
    onFiltersChange: (BusinessDashboardFilters) -> Unit,
    onClearFilters: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        Text(stringResource(R.string.business_filters), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
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
        LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
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
