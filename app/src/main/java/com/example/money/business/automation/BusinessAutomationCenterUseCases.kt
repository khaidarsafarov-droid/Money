package com.example.money.business.automation

import android.content.Context
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.business.BusinessCommunicationSendResult
import com.example.money.domain.repository.BusinessRepository
import com.example.money.domain.usecase.BusinessCommunicationUseCases
import com.example.money.domain.usecase.BusinessSmsUseCases
import com.example.money.domain.util.ai.BusinessAiAssistantService
import com.example.money.notification.BusinessNotificationHelper
import com.example.money.settings.BusinessBranchSettings
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Central automation orchestrator — debts, invoices, large transactions, AI insights.
 */
class BusinessAutomationCenterUseCases(
    private val businessRepository: BusinessRepository,
    private val communicationUseCases: BusinessCommunicationUseCases,
    private val smsUseCases: BusinessSmsUseCases,
    private val preferencesStore: MoneyPreferencesStore,
) {

    suspend fun runAll(
        context: Context,
        settings: BusinessBranchSettings,
    ): BusinessAutomationRunResult {
        val workflowResult = if (settings.hasWorkflowDebtAutomation()) {
            communicationUseCases.runWorkflowAutomation(context, settings)
        } else {
            BusinessCommunicationSendResult(0, 0, 0)
        }

        val invoiceCount = if (settings.hasOverdueInvoiceAutomation()) {
            val result = smsUseCases.runOverdueInvoiceAutomation(settings)
            result.sentCount + result.openedAppCount
        } else {
            0
        }

        val largeAlerts = if (settings.largeTransactionAlertsEnabled) {
            checkLargeTransactions(context, settings)
        } else {
            0
        }

        val insightShown = if (settings.weeklyAiInsightsEnabled) {
            showWeeklyInsight(context, settings)
        } else {
            false
        }

        return BusinessAutomationRunResult(
            workflowDebts = workflowResult,
            overdueInvoices = invoiceCount,
            largeTransactionAlerts = largeAlerts,
            weeklyInsightShown = insightShown,
        )
    }

    private suspend fun checkLargeTransactions(
        context: Context,
        settings: BusinessBranchSettings,
    ): Int {
        val threshold = settings.largeTransactionThresholdCents
        val since = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        val alreadyAlerted = preferencesStore.alertedLargeTransactionIds.first()
        val recent = businessRepository.observeTransactions().first()
            .filter { it.date >= since && it.amountCents >= threshold && it.id !in alreadyAlerted }
        var shown = 0
        val newlyAlerted = alreadyAlerted.toMutableSet()
        recent.forEach { tx ->
            if (BusinessNotificationHelper.showLargeTransactionAlert(context, tx, threshold)) {
                shown++
                newlyAlerted += tx.id
            }
        }
        if (newlyAlerted.size > alreadyAlerted.size) {
            preferencesStore.setAlertedLargeTransactionIds(newlyAlerted)
        }
        return shown
    }

    private suspend fun showWeeklyInsight(
        context: Context,
        settings: BusinessBranchSettings,
    ): Boolean {
        val now = System.currentTimeMillis()
        val lastShown = preferencesStore.lastWeeklyAiInsightMillis.first()
        if (now - lastShown < TimeUnit.DAYS.toMillis(7)) {
            return false
        }

        val (monthStart, monthEnd) = currentMonthRange()
        val dashboard = businessRepository.observeDashboard(monthStart, monthEnd).first()
        val invoices = businessRepository.observeInvoices().first()
        val clients = businessRepository.observeClients().first()
        val transactions = businessRepository.observeTransactions().first()
        val insight = BusinessAiAssistantService.respond(
            promptId = "insights",
            dashboard = dashboard,
            invoices = invoices,
            clients = clients,
            transactions = transactions,
            forecast = emptyList(),
            invoiceRisks = emptyMap(),
        )
        val shown = BusinessNotificationHelper.showWeeklyAiInsight(
            context = context,
            businessName = settings.businessName,
            body = insight,
        )
        if (shown) {
            preferencesStore.setLastWeeklyAiInsightMillis(now)
        }
        return shown
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        return monthStart to calendar.timeInMillis
    }
}
