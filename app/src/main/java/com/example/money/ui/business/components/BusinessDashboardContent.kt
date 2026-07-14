package com.example.money.ui.business.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
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
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.ui.business.components.cards.BusinessKpiCard
import com.example.money.ui.business.components.charts.CashflowLineChart
import com.example.money.ui.business.components.charts.ProfitBarChart
import com.example.money.ui.business.components.widgets.AiInsightsWidget
import com.example.money.ui.business.components.widgets.ClientProfitabilityWidget
import com.example.money.ui.business.components.widgets.OverdueInvoicesWidget
import com.example.money.ui.business.components.widgets.ProjectPerformanceWidget
import com.example.money.ui.business.components.widgets.RecentTransactionsWidget
import com.example.money.ui.business.components.widgets.UpcomingPaymentsWidget
import com.example.money.ui.business.theme.BusinessKpiType
import com.example.money.ui.business.theme.BusinessSpacing

private val WideLayoutBreakpoint = 720.dp

/**
 * Compose equivalent of BusinessDashboardPage.tsx — two-column on wide screens.
 */
@Composable
fun BusinessDashboardContent(
    dashboard: BusinessDashboardSummary,
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    transactions: List<BusinessTransaction>,
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
        item { DashboardHeader() }
        item {
            DashboardFilterRow(
                clients = clients,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onClearFilters = onClearFilters,
            )
        }
        item { KpiGrid(dashboard) }
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= WideLayoutBreakpoint) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
                    ) {
                        DashboardMainColumn(
                            dashboard = dashboard,
                            modifier = Modifier.weight(2f),
                        )
                        DashboardSideColumn(
                            dashboard = dashboard,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md)) {
                        DashboardMainColumn(dashboard = dashboard)
                        DashboardSideColumn(dashboard = dashboard)
                    }
                }
            }
        }
        item {
            RecentTransactionsWidget(transactions = transactions)
        }
    }
}

@Composable
private fun DashboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.business_dashboard_title),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.business_dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.business_mode_chip)) },
        )
    }
}

@Composable
private fun KpiGrid(dashboard: BusinessDashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            BusinessKpiCard(
                title = stringResource(R.string.business_metric_revenue),
                amountCents = dashboard.monthRevenueCents,
                type = BusinessKpiType.Revenue,
                changePercent = dashboard.revenueChangePercent,
                sparklineValues = dashboard.revenueSparkline,
                icon = Icons.Outlined.TrendingUp,
                modifier = Modifier.weight(1f),
            )
            BusinessKpiCard(
                title = stringResource(R.string.business_metric_expenses),
                amountCents = dashboard.monthExpenseCents,
                type = BusinessKpiType.Expense,
                changePercent = dashboard.expenseChangePercent,
                sparklineValues = dashboard.expenseSparkline,
                icon = Icons.Outlined.TrendingDown,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            BusinessKpiCard(
                title = stringResource(R.string.business_metric_profit),
                amountCents = dashboard.monthNetProfitCents,
                type = BusinessKpiType.Profit,
                changePercent = dashboard.profitChangePercent,
                sparklineValues = dashboard.profitSparkline,
                icon = Icons.Outlined.AccountBalance,
                modifier = Modifier.weight(1f),
            )
            BusinessKpiCard(
                title = stringResource(R.string.business_metric_cash_on_hand),
                amountCents = dashboard.cashOnHandCents,
                type = BusinessKpiType.Cash,
                changePercent = null,
                sparklineValues = dashboard.cashSparkline,
                icon = Icons.Outlined.Wallet,
                subtitle = stringResource(R.string.business_cash_subtitle),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DashboardMainColumn(
    dashboard: BusinessDashboardSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        CashflowLineChart(
            incomeValues = dashboard.cashflowWeekly.map { it.incomeCents },
            expenseValues = dashboard.cashflowWeekly.map { it.expenseCents },
            labels = dashboard.cashflowWeekly.map { it.label },
            title = stringResource(R.string.business_chart_cashflow),
        )
        ProfitBarChart(
            values = dashboard.monthlyProfit.map { it.profitCents },
            labels = dashboard.monthlyProfit.map { it.label },
            title = stringResource(R.string.business_chart_pnl),
        )
        AiInsightsWidget(insights = dashboard.insights)
    }
}

@Composable
private fun DashboardSideColumn(
    dashboard: BusinessDashboardSummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        ClientProfitabilityWidget(
            title = stringResource(R.string.business_top_clients_table),
            clients = dashboard.clientTable,
        )
        ProjectPerformanceWidget(projects = dashboard.projectCards)
        Row(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
            OverdueInvoicesWidget(
                items = dashboard.overdueInvoices,
                modifier = Modifier.weight(1f),
            )
            UpcomingPaymentsWidget(
                items = dashboard.upcomingPayments,
                modifier = Modifier.weight(1f),
            )
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
            if (filters.clientId != null || filters.projectId != null || filters.invoiceStatus != null) {
                item {
                    FilterChip(
                        selected = false,
                        onClick = onClearFilters,
                        label = { Text(stringResource(R.string.business_filter_clear)) },
                    )
                }
            }
        }
    }
}
