package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessReportPeriod
import com.example.money.domain.model.business.BusinessReportType
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessReportsEngineTest {

    @Test
    fun generateProfitLossReport_sumsPeriodTotals() {
        val transactions = listOf(
            BusinessTransaction(
                id = 1,
                type = BusinessTransactionType.INCOME,
                amountCents = 100_00,
                category = "Revenue",
                date = 2_000,
            ),
            BusinessTransaction(
                id = 2,
                type = BusinessTransactionType.EXPENSE,
                amountCents = 40_00,
                category = "Software",
                date = 3_000,
            ),
        )
        val report = BusinessReportsEngine.generateProfitLossReport(
            transactions = transactions,
            filters = com.example.money.domain.model.business.BusinessReportFilters(
                period = BusinessReportPeriod.MONTH,
            ),
            nowMillis = 5_000,
        )
        assertEquals(100_00, report.revenueCents)
        assertEquals(40_00, report.expenseCents)
        assertEquals(60_00, report.netProfitCents)
    }

    @Test
    fun exportProfitLoss_producesCsvRows() {
        val bundle = BusinessReportsEngine.generateAll(
            transactions = listOf(
                BusinessTransaction(
                    id = 1,
                    type = BusinessTransactionType.INCOME,
                    amountCents = 50_00,
                    category = "Revenue",
                    date = 2_000,
                ),
            ),
            invoices = emptyList(),
            clients = emptyList(),
            projects = emptyList(),
            filters = com.example.money.domain.model.business.BusinessReportFilters(),
            nowMillis = 5_000,
        )
        val csv = BusinessCsvExporter.export(BusinessReportType.PROFIT_LOSS, bundle)
        assertTrue(csv.contains("Profit & Loss Report"))
        assertTrue(csv.contains("Revenue"))
    }
}
