package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.CategoryAmountRow
import com.example.money.domain.model.business.ClientProfitRow
import com.example.money.domain.model.business.OverdueInvoiceRow

object BusinessInsightEngine {

    fun generate(
        summary: BusinessDashboardSummary,
        clientTable: List<ClientProfitRow>,
        overdueInvoices: List<OverdueInvoiceRow>,
        expenseCategories: List<CategoryAmountRow>,
    ): List<String> = buildList {
        val topClient = clientTable.firstOrNull()
        if (topClient != null && summary.monthNetProfitCents > 0) {
            val share = (topClient.profitCents.toDouble() / summary.monthNetProfitCents * 100).toInt()
            if (share >= 30) {
                add("Клиент «${topClient.clientName}» даёт $share% прибыли — рассмотрите расширение проекта.")
            }
        }

        val marketing = expenseCategories.find { it.category.equals("Marketing", ignoreCase = true) }
        if (marketing != null && summary.monthExpenseCents > 0) {
            val share = (marketing.amountCents.toDouble() / summary.monthExpenseCents * 100).toInt()
            if (share >= 15) {
                add("Расходы на маркетинг — $share% от общих расходов. Проверьте эффективность каналов.")
            }
        }

        overdueInvoices.firstOrNull()?.let { overdue ->
            add("Счёт «${overdue.clientName}» просрочен на ${overdue.daysOverdue} дн. — отправьте напоминание.")
        }

        if (summary.monthNetProfitCents < 0) {
            add("Чистая прибыль отрицательная — пересмотрите крупные статьи расходов в этом месяце.")
        }

        if (summary.expectedReceivablesCents > summary.monthRevenueCents && summary.expectedReceivablesCents > 0) {
            add("Дебиторка выше месячного дохода — ускорьте сбор оплат по открытым счетам.")
        }

        if (isEmpty()) {
            add("Данных пока мало. Добавьте клиентов, счета и операции для персональных инсайтов.")
        }
    }.take(3)
}
