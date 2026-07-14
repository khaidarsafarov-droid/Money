package com.example.money.ui.business.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.money.domain.model.business.BusinessDashboardView
import com.example.money.domain.model.business.BusinessFilterPreset
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.SavedFilterPreset
import com.example.money.ui.business.BusinessQuickAction
import com.example.money.ui.business.components.cards.BusinessKpiCard
import com.example.money.ui.business.components.charts.CashflowLineChart
import com.example.money.ui.business.components.charts.ProfitBarChart
import com.example.money.ui.business.components.filters.BusinessFiltersBar
import com.example.money.ui.business.components.widgets.AiInsightsWidget
import com.example.money.ui.business.components.widgets.AttentionCenterWidget
import com.example.money.ui.business.components.widgets.ClientProfitabilityWidget
import com.example.money.ui.business.components.widgets.OverdueInvoicesWidget
import com.example.money.ui.business.components.widgets.ProjectPerformanceWidget
import com.example.money.ui.business.components.widgets.RecentTransactionsWidget
import com.example.money.ui.business.components.widgets.UpcomingPaymentsWidget
import com.example.money.ui.business.theme.BusinessKpiType
import com.example.money.ui.business.theme.BusinessSpacing

private val WideLayoutBreakpoint = 720.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDashboardContent(
    dashboard: BusinessDashboardSummary,
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    transactions: List<BusinessTransaction>,
    filters: BusinessDashboardFilters,
    dashboardView: BusinessDashboardView,
    savedPresets: List<SavedFilterPreset>,
    onFiltersChange: (BusinessDashboardFilters) -> Unit,
    onApplyPreset: (BusinessFilterPreset) -> Unit,
    onApplySavedPreset: (SavedFilterPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeleteSavedPreset: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDashboardViewChange: (BusinessDashboardView) -> Unit,
    onQuickAction: (BusinessQuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        item { DashboardHeader() }
        item {
            DashboardViewSwitcher(
                selectedView = dashboardView,
                onViewChange = onDashboardViewChange,
            )
        }
        item {
            BusinessActionShortcuts(onAction = onQuickAction)
        }
        item {
            BusinessFiltersBar(
                clients = clients,
                projects = projects,
                filters = filters,
                savedPresets = savedPresets,
                onFiltersChange = onFiltersChange,
                onApplyPreset = onApplyPreset,
                onApplySavedPreset = onApplySavedPreset,
                onSavePreset = onSavePreset,
                onDeleteSavedPreset = onDeleteSavedPreset,
                onClearFilters = onClearFilters,
            )
        }
        item {
            AttentionCenterWidget(alerts = dashboard.attentionAlerts)
        }
        item { KpiGrid(dashboard) }
        item {
            when (dashboardView) {
                BusinessDashboardView.FINANCIAL -> FinancialDashboardBody(dashboard)
                BusinessDashboardView.CLIENT -> ClientDashboardBody(dashboard)
                BusinessDashboardView.PROJECT -> ProjectDashboardBody(dashboard)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardViewSwitcher(
    selectedView: BusinessDashboardView,
    onViewChange: (BusinessDashboardView) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        BusinessDashboardView.entries.forEachIndexed { index, view ->
            SegmentedButton(
                selected = selectedView == view,
                onClick = { onViewChange(view) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = BusinessDashboardView.entries.size),
                icon = {
                    val icon = when (view) {
                        BusinessDashboardView.FINANCIAL -> Icons.Outlined.AccountBalance
                        BusinessDashboardView.CLIENT -> Icons.Outlined.Groups
                        BusinessDashboardView.PROJECT -> Icons.Outlined.Work
                    }
                    androidx.compose.material3.Icon(icon, contentDescription = null)
                },
            ) {
                Text(
                    when (view) {
                        BusinessDashboardView.FINANCIAL -> stringResource(R.string.business_view_financial)
                        BusinessDashboardView.CLIENT -> stringResource(R.string.business_view_client)
                        BusinessDashboardView.PROJECT -> stringResource(R.string.business_view_project)
                    },
                )
            }
        }
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
private fun FinancialDashboardBody(dashboard: BusinessDashboardSummary) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= WideLayoutBreakpoint) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
            ) {
                FinancialMainColumn(dashboard = dashboard, modifier = Modifier.weight(2f))
                FinancialSideColumn(dashboard = dashboard, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md)) {
                FinancialMainColumn(dashboard = dashboard)
                FinancialSideColumn(dashboard = dashboard)
            }
        }
    }
}

@Composable
private fun ClientDashboardBody(dashboard: BusinessDashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md)) {
        ClientProfitabilityWidget(
            title = stringResource(R.string.business_top_clients_table),
            clients = dashboard.clientTable,
        )
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
        AiInsightsWidget(insights = dashboard.insights)
    }
}

@Composable
private fun ProjectDashboardBody(dashboard: BusinessDashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md)) {
        ProjectPerformanceWidget(projects = dashboard.projectCards)
        CashflowLineChart(
            incomeValues = dashboard.cashflowWeekly.map { it.incomeCents },
            expenseValues = dashboard.cashflowWeekly.map { it.expenseCents },
            labels = dashboard.cashflowWeekly.map { it.label },
            title = stringResource(R.string.business_chart_cashflow),
        )
    }
}

@Composable
private fun FinancialMainColumn(
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
private fun FinancialSideColumn(
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
