package com.example.money.ui.business.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.BusinessReportFilters
import com.example.money.domain.model.business.BusinessReportPeriod
import com.example.money.domain.model.business.BusinessReportType
import com.example.money.domain.model.business.BusinessReportsBundle
import com.example.money.ui.business.components.charts.CashflowLineChart
import com.example.money.ui.business.components.charts.CategoryDistributionChart
import com.example.money.ui.business.components.charts.ProfitBarChart
import com.example.money.ui.business.components.tables.BusinessTable
import com.example.money.ui.business.components.tables.BusinessTableColumn
import com.example.money.ui.business.theme.BusinessSpacing
import com.example.money.ui.util.Formatters

enum class BusinessReportTab {
    PROFIT_LOSS,
    CASHFLOW,
    CLIENT,
    CATEGORY,
    PROJECT,
    TAX,
}

@Composable
fun BusinessReportsContent(
    reports: BusinessReportsBundle,
    filters: BusinessReportFilters,
    selectedTab: BusinessReportTab,
    onTabSelected: (BusinessReportTab) -> Unit,
    onPeriodChange: (BusinessReportPeriod) -> Unit,
    onExport: (BusinessReportType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTab.ordinal, edgePadding = 8.dp) {
            BusinessReportTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab.ordinal == index,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            when (tab) {
                                BusinessReportTab.PROFIT_LOSS -> stringResource(R.string.business_report_pnl)
                                BusinessReportTab.CASHFLOW -> stringResource(R.string.business_report_cashflow)
                                BusinessReportTab.CLIENT -> stringResource(R.string.business_report_clients)
                                BusinessReportTab.CATEGORY -> stringResource(R.string.business_report_categories)
                                BusinessReportTab.PROJECT -> stringResource(R.string.business_report_projects)
                                BusinessReportTab.TAX -> stringResource(R.string.business_report_tax)
                            },
                        )
                    },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(BusinessSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
        ) {
            item {
                ReportPeriodFilters(period = filters.period, onPeriodChange = onPeriodChange)
            }
            item {
                ExportButton(
                    type = selectedTab.toReportType(),
                    onExport = onExport,
                )
            }
            item {
                when (selectedTab) {
                    BusinessReportTab.PROFIT_LOSS -> ProfitLossReportView(reports.profitLoss)
                    BusinessReportTab.CASHFLOW -> CashflowReportView(reports.cashflow)
                    BusinessReportTab.CLIENT -> ClientReportView(reports.client)
                    BusinessReportTab.CATEGORY -> CategoryReportView(reports.category)
                    BusinessReportTab.PROJECT -> ProjectReportView(reports.project)
                    BusinessReportTab.TAX -> TaxReportView(reports.tax)
                }
            }
        }
    }
}

@Composable
private fun ReportPeriodFilters(
    period: BusinessReportPeriod,
    onPeriodChange: (BusinessReportPeriod) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        item {
            PeriodChip(BusinessReportPeriod.DAY, period, onPeriodChange, R.string.business_filter_today)
        }
        item {
            PeriodChip(BusinessReportPeriod.WEEK, period, onPeriodChange, R.string.business_filter_this_week)
        }
        item {
            PeriodChip(BusinessReportPeriod.MONTH, period, onPeriodChange, R.string.business_filter_this_month)
        }
        item {
            PeriodChip(BusinessReportPeriod.YEAR, period, onPeriodChange, R.string.business_report_year)
        }
    }
}

@Composable
private fun PeriodChip(
    value: BusinessReportPeriod,
    selected: BusinessReportPeriod,
    onChange: (BusinessReportPeriod) -> Unit,
    labelRes: Int,
) {
    FilterChip(
        selected = selected == value,
        onClick = { onChange(value) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun ExportButton(
    type: BusinessReportType,
    onExport: (BusinessReportType) -> Unit,
) {
    Button(onClick = { onExport(type) }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.business_report_export_csv))
    }
}

@Composable
private fun ProfitLossReportView(report: com.example.money.domain.model.business.ProfitLossReport) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        ReportSummaryRow(stringResource(R.string.business_metric_revenue), report.revenueCents)
        ReportSummaryRow(stringResource(R.string.business_metric_expenses), report.expenseCents)
        ReportSummaryRow(stringResource(R.string.business_metric_profit), report.netProfitCents)
        ProfitBarChart(
            values = report.monthlyBreakdown.map { it.profitCents },
            labels = report.monthlyBreakdown.map { it.label },
            title = stringResource(R.string.business_chart_pnl),
        )
    }
}

@Composable
private fun CashflowReportView(report: com.example.money.domain.model.business.CashflowReport) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        ReportSummaryRow(stringResource(R.string.business_tx_income), report.totalIncomeCents)
        ReportSummaryRow(stringResource(R.string.business_tx_expense), report.totalExpenseCents)
        ReportSummaryRow(stringResource(R.string.business_report_net_cashflow), report.netCashflowCents)
        CashflowLineChart(
            incomeValues = report.weeklyPoints.map { it.incomeCents },
            expenseValues = report.weeklyPoints.map { it.expenseCents },
            labels = report.weeklyPoints.map { it.label },
            title = stringResource(R.string.business_chart_cashflow),
        )
    }
}

@Composable
private fun ClientReportView(report: com.example.money.domain.model.business.ClientReport) {
    if (report.rows.isEmpty()) {
        Text(stringResource(R.string.business_report_empty))
        return
    }
    BusinessTable(
        title = stringResource(R.string.business_report_clients),
        columns = listOf(
            BusinessTableColumn(stringResource(R.string.business_field_name), 1.2f),
            BusinessTableColumn(stringResource(R.string.business_col_revenue), 1f),
            BusinessTableColumn(stringResource(R.string.business_col_expense), 1f),
            BusinessTableColumn(stringResource(R.string.business_col_profit), 1f),
        ),
        rows = report.rows.map {
            listOf(
                it.clientName,
                Formatters.currencyFromCents(it.revenueCents),
                Formatters.currencyFromCents(it.expenseCents),
                Formatters.currencyFromCents(it.profitCents),
            )
        },
    )
}

@Composable
private fun CategoryReportView(report: com.example.money.domain.model.business.CategoryReport) {
    if (report.rows.isEmpty()) {
        Text(stringResource(R.string.business_report_empty))
        return
    }
    CategoryDistributionChart(
        rows = report.rows,
        title = stringResource(R.string.business_chart_categories),
    )
    BusinessTable(
        title = stringResource(R.string.business_top_categories),
        columns = listOf(
            BusinessTableColumn(stringResource(R.string.business_field_category), 1.2f),
            BusinessTableColumn(stringResource(R.string.business_col_revenue), 1f),
            BusinessTableColumn(stringResource(R.string.business_col_expense), 1f),
        ),
        rows = report.rows.map {
            listOf(
                it.category,
                Formatters.currencyFromCents(it.incomeCents),
                Formatters.currencyFromCents(it.expenseCents),
            )
        },
    )
}

@Composable
private fun ProjectReportView(report: com.example.money.domain.model.business.ProjectReport) {
    if (report.rows.isEmpty()) {
        Text(stringResource(R.string.business_report_empty))
        return
    }
    BusinessTable(
        title = stringResource(R.string.business_report_projects),
        columns = listOf(
            BusinessTableColumn(stringResource(R.string.business_field_project), 1f),
            BusinessTableColumn(stringResource(R.string.business_field_name), 1f),
            BusinessTableColumn(stringResource(R.string.business_field_budget), 1f),
            BusinessTableColumn(stringResource(R.string.business_col_profit), 1f),
        ),
        rows = report.rows.map {
            listOf(
                it.projectName,
                it.clientName,
                Formatters.currencyFromCents(it.budgetCents),
                Formatters.currencyFromCents(it.profitCents),
            )
        },
    )
}

@Composable
private fun TaxReportView(report: com.example.money.domain.model.business.TaxReport) {
    Column(verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm)) {
        ReportSummaryRow(stringResource(R.string.business_report_taxable_income), report.taxableIncomeCents)
        ReportSummaryRow(stringResource(R.string.business_report_deductible), report.deductibleExpensesCents)
        ReportSummaryRow(stringResource(R.string.business_report_net_taxable), report.netTaxableCents)
        if (report.lines.isNotEmpty()) {
            BusinessTable(
                title = stringResource(R.string.business_report_tax_lines),
                columns = listOf(
                    BusinessTableColumn(stringResource(R.string.business_field_category), 1f),
                    BusinessTableColumn(stringResource(R.string.business_field_amount), 1f),
                    BusinessTableColumn("Type", 0.8f),
                ),
                rows = report.lines.take(20).map {
                    listOf(
                        it.category,
                        Formatters.currencyFromCents(it.amountCents),
                        it.type.name,
                    )
                },
            )
        }
    }
}

@Composable
private fun ReportSummaryRow(label: String, amountCents: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            Formatters.currencyFromCents(amountCents),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun BusinessReportTab.toReportType(): BusinessReportType = when (this) {
    BusinessReportTab.PROFIT_LOSS -> BusinessReportType.PROFIT_LOSS
    BusinessReportTab.CASHFLOW -> BusinessReportType.CASHFLOW
    BusinessReportTab.CLIENT -> BusinessReportType.CLIENT
    BusinessReportTab.CATEGORY -> BusinessReportType.CATEGORY
    BusinessReportTab.PROJECT -> BusinessReportType.PROJECT
    BusinessReportTab.TAX -> BusinessReportType.TAX
}
