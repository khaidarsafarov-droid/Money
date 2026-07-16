package com.example.money.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.FinanceBranch
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardFilters
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessDashboardView
import com.example.money.domain.model.business.BusinessSmsRecipient
import com.example.money.domain.model.business.BusinessSmsSendRequest
import com.example.money.domain.model.business.BusinessSmsSendResult
import com.example.money.domain.model.business.BusinessSmsTemplateId
import com.example.money.domain.model.business.BusinessSmsLog
import com.example.money.domain.model.business.BusinessCategory
import com.example.money.domain.model.business.CategoryType
import com.example.money.domain.model.business.BusinessReportFilters
import com.example.money.domain.model.business.BusinessReportPeriod
import com.example.money.domain.model.business.BusinessReportType
import com.example.money.domain.model.business.BusinessReportsBundle
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.domain.model.business.BusinessFilterPreset
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.SavedFilterPreset
import com.example.money.domain.model.business.RecurringBusinessRule
import com.example.money.domain.model.business.RecurringBusinessRuleCodec
import com.example.money.domain.model.business.CashflowForecastPoint
import com.example.money.domain.repository.BusinessRepository
import com.example.money.business.api.BusinessApi
import com.example.money.domain.usecase.BusinessCommunicationUseCases
import com.example.money.domain.usecase.BusinessSmsUseCases
import com.example.money.domain.usecase.BusinessUseCases
import com.example.money.domain.util.BusinessAutomationService
import com.example.money.domain.util.BusinessCsvExporter
import com.example.money.domain.util.BusinessFinanceEngine
import com.example.money.domain.util.BusinessReportsEngine
import com.example.money.domain.util.ai.AiInvoiceRiskService
import com.example.money.domain.util.ai.BusinessSmsTone
import com.example.money.domain.util.ai.BusinessAiAssistantService
import com.example.money.domain.util.MoneyMath
import com.example.money.notification.BusinessAutomationCenterScheduler
import com.example.money.notification.BusinessInvoiceReminderScheduler
import com.example.money.notification.BusinessSmsScheduler
import com.example.money.notification.WorkflowDebtScheduler
import com.example.money.settings.BusinessBranchSettings
import com.example.money.ui.business.assistant.BusinessAssistantMessage
import com.example.money.ui.business.assistant.BusinessAssistantRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.money.ui.business.reports.BusinessReportTab
import com.example.money.ui.business.reports.BusinessReportShare
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BusinessTab {
    Dashboard,
    Workflows,
    Clients,
    Projects,
    Invoices,
    Cashflow,
    Reports,
    Assistant,
    Sms,
    Settings,
}

enum class BusinessQuickAction {
    CREATE_INVOICE,
    ADD_TRANSACTION,
    ADD_CLIENT,
    ADD_CATEGORY,
}

class BusinessViewModel(
    private val businessRepository: BusinessRepository,
    private val preferencesStore: MoneyPreferencesStore,
    private val businessUseCases: BusinessUseCases,
    private val businessSmsUseCases: BusinessSmsUseCases,
    private val invoiceReminderScheduler: BusinessInvoiceReminderScheduler? = null,
    private val businessSmsScheduler: BusinessSmsScheduler? = null,
    private val workflowDebtScheduler: WorkflowDebtScheduler? = null,
    private val businessAutomationCenterScheduler: BusinessAutomationCenterScheduler? = null,
    private val businessCommunicationUseCases: BusinessCommunicationUseCases? = null,
    private val businessApi: BusinessApi? = null,
) : ViewModel() {

    val branchSettings: StateFlow<BusinessBranchSettings> = preferencesStore.businessBranchSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BusinessBranchSettings(),
    )

    val clients: StateFlow<List<BusinessClient>> = businessRepository.observeClients().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val projects: StateFlow<List<BusinessProject>> = businessRepository.observeProjects().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val invoices: StateFlow<List<BusinessInvoice>> = businessRepository.observeInvoices().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val transactions: StateFlow<List<BusinessTransaction>> = businessRepository.observeTransactions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val categories: StateFlow<List<BusinessCategory>> = businessRepository.observeCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _filters = MutableStateFlow(BusinessDashboardFilters())
    val filters: StateFlow<BusinessDashboardFilters> = _filters.asStateFlow()

    val dashboard: StateFlow<BusinessDashboardSummary> = _filters
        .flatMapLatest { filters ->
            val range = BusinessFinanceEngine.periodRangeForPreset(filters.periodPreset)
            businessRepository.observeDashboard(range.first, range.second, filters)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BusinessDashboardSummary(
                monthRevenueCents = 0,
                monthExpenseCents = 0,
                monthNetProfitCents = 0,
                overdueReceivablesCents = 0,
                expectedReceivablesCents = 0,
                topClients = emptyList(),
                topExpenseCategories = emptyList(),
            ),
        )

    private val _selectedTab = MutableStateFlow(BusinessTab.Dashboard)
    val selectedTab: StateFlow<BusinessTab> = _selectedTab.asStateFlow()

    private val _pendingQuickAction = MutableStateFlow<BusinessQuickAction?>(null)
    val pendingQuickAction: StateFlow<BusinessQuickAction?> = _pendingQuickAction.asStateFlow()

    private val _reportFilters = MutableStateFlow(BusinessReportFilters())
    val reportFilters: StateFlow<BusinessReportFilters> = _reportFilters.asStateFlow()

    private val _selectedReportTab = MutableStateFlow(BusinessReportTab.PROFIT_LOSS)
    val selectedReportTab: StateFlow<BusinessReportTab> = _selectedReportTab.asStateFlow()

    val reports: StateFlow<BusinessReportsBundle> = combine(
        transactions,
        invoices,
        clients,
        projects,
        _reportFilters,
    ) { tx, inv, cl, pr, filters ->
        BusinessReportsEngine.generateAll(tx, inv, cl, pr, filters)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BusinessReportsEngine.generateAll(
            emptyList(), emptyList(), emptyList(), emptyList(), BusinessReportFilters(),
        ),
    )

    val invoiceRisks: StateFlow<Map<Long, InvoiceRiskAssessment>> = combine(invoices, clients) { inv, cl ->
        AiInvoiceRiskService.assessAll(inv, cl)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    val cashflowForecast: StateFlow<List<CashflowForecastPoint>> = businessUseCases.predictCashflow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _assistantMessages = MutableStateFlow<List<BusinessAssistantMessage>>(emptyList())
    val assistantMessages: StateFlow<List<BusinessAssistantMessage>> = _assistantMessages.asStateFlow()

    val smsHistory: StateFlow<List<BusinessSmsLog>> = businessSmsUseCases.observeSmsHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val smsTemplates = businessSmsUseCases.listTemplates()

    init {
        viewModelScope.launch {
            businessRepository.refreshInvoiceStatuses()
            val settings = preferencesStore.businessBranchSettings.first()
            if (settings.recurringTransactionsEnabled) {
                processRecurringRules(settings)
            }
        }
        viewModelScope.launch {
            combine(invoices, branchSettings) { inv, settings ->
                inv to settings
            }.collect { (inv, settings) ->
                invoiceReminderScheduler?.syncAll(inv, settings)
                businessSmsScheduler?.syncSchedule(settings)
                workflowDebtScheduler?.syncSchedule(settings)
            }
        }
    }

    private suspend fun processRecurringRules(settings: BusinessBranchSettings) {
        val updatedRules = BusinessAutomationService.processRecurringRules(
            businessRepository,
            settings.recurringRules(),
        )
        if (updatedRules != settings.recurringRules()) {
            preferencesStore.updateBusinessBranchSettings {
                it.copy(recurringRulesJson = RecurringBusinessRuleCodec.encode(updatedRules))
            }
        }
    }

    fun selectTab(tab: BusinessTab) {
        _selectedTab.value = tab
    }

    fun updateFilters(transform: (BusinessDashboardFilters) -> BusinessDashboardFilters) {
        _filters.value = transform(_filters.value)
    }

    fun applyFilterPreset(preset: BusinessFilterPreset) {
        _filters.value = when (preset) {
            BusinessFilterPreset.BY_CLIENT -> _filters.value.copy(periodPreset = preset)
            BusinessFilterPreset.BY_PROJECT -> _filters.value.copy(periodPreset = preset)
            else -> _filters.value.copy(
                periodPreset = preset,
                clientId = null,
                projectId = null,
            )
        }
    }

    fun applySavedFilterPreset(preset: SavedFilterPreset) {
        _filters.value = BusinessDashboardFilters(
            clientId = preset.clientId,
            projectId = preset.projectId,
            category = preset.category,
            periodPreset = preset.periodPreset,
            transactionType = preset.transactionType,
            invoiceStatus = preset.invoiceStatus,
        )
    }

    fun saveCurrentFilterPreset(name: String) {
        if (name.isBlank()) return
        val current = _filters.value
        val preset = SavedFilterPreset(
            name = name.trim(),
            clientId = current.clientId,
            projectId = current.projectId,
            category = current.category,
            transactionType = current.transactionType,
            invoiceStatus = current.invoiceStatus,
            periodPreset = current.periodPreset,
        )
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { settings ->
                val existing = settings.savedFilterPresets().filterNot { it.name == preset.name }
                settings.copy(
                    savedFilterPresetsJson = BusinessBranchSettings.encodeSavedFilterPresets(existing + preset),
                )
            }
        }
    }

    fun deleteSavedFilterPreset(name: String) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { settings ->
                val updated = settings.savedFilterPresets().filterNot { it.name == name }
                settings.copy(
                    savedFilterPresetsJson = BusinessBranchSettings.encodeSavedFilterPresets(updated),
                )
            }
        }
    }

    fun clearFilters() {
        _filters.value = BusinessDashboardFilters()
    }

    fun setDashboardView(view: BusinessDashboardView) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { it.copy(dashboardView = view) }
        }
    }

    fun requestQuickAction(action: BusinessQuickAction) {
        _pendingQuickAction.value = action
    }

    fun consumeQuickAction() {
        _pendingQuickAction.value = null
    }

    fun selectReportTab(tab: BusinessReportTab) {
        _selectedReportTab.value = tab
    }

    fun setReportPeriod(period: BusinessReportPeriod) {
        _reportFilters.value = _reportFilters.value.copy(period = period)
    }

    fun createReportExportIntent(context: Context, type: BusinessReportType): Intent {
        val csv = BusinessCsvExporter.export(type, reports.value)
        return BusinessReportShare.createShareIntent(context, type, csv)
    }

    fun switchToPersonal() {
        viewModelScope.launch {
            preferencesStore.setActiveFinanceBranch(FinanceBranch.PERSONAL)
        }
    }

    fun updateBusinessName(name: String) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { it.copy(businessName = name) }
        }
    }

    fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            businessRepository.addCategory(
                BusinessCategory(name = trimmed, type = CategoryType.EXPENSE),
            )
            preferencesStore.updateBusinessBranchSettings { settings ->
                val updated = (settings.customCategories() + trimmed).distinct()
                settings.copy(customCategoriesJson = BusinessBranchSettings.encodeStringList(updated))
            }
        }
    }

    fun addClient(name: String, email: String, phone: String, contactInfo: String) {
        viewModelScope.launch {
            businessRepository.addClient(
                BusinessClient(
                    name = name.trim(),
                    email = email.trim(),
                    phone = phone.trim(),
                    contactInfo = contactInfo.trim(),
                ),
            )
        }
    }

    fun deleteClient(client: BusinessClient) {
        viewModelScope.launch { businessRepository.deleteClient(client) }
    }

    fun addProject(clientId: Long, name: String, budgetAmount: Double) {
        viewModelScope.launch {
            businessRepository.addProject(
                BusinessProject(
                    clientId = clientId,
                    name = name.trim(),
                    budgetCents = MoneyMath.toCents(budgetAmount),
                ),
            )
        }
    }

    fun deleteProject(project: BusinessProject) {
        viewModelScope.launch { businessRepository.deleteProject(project) }
    }

    fun addInvoice(
        clientId: Long,
        projectId: Long?,
        amount: Double,
        issueDate: Long,
        dueDate: Long,
        status: InvoiceStatus,
    ) {
        viewModelScope.launch {
            businessRepository.addInvoice(
                BusinessInvoice(
                    clientId = clientId,
                    projectId = projectId,
                    issueDate = issueDate,
                    dueDate = dueDate,
                    amountCents = MoneyMath.toCents(amount),
                    status = status,
                ),
            )
            businessRepository.refreshInvoiceStatuses()
            rescheduleInvoiceReminders()
        }
    }

    fun markInvoicePaid(invoice: BusinessInvoice) {
        viewModelScope.launch {
            businessRepository.markInvoicePaid(invoice.id)
            businessRepository.refreshInvoiceStatuses()
            invoiceReminderScheduler?.cancel(invoice.id)
        }
    }

    fun deleteInvoice(invoice: BusinessInvoice) {
        viewModelScope.launch { businessRepository.deleteInvoice(invoice) }
    }

    fun addTransaction(
        type: BusinessTransactionType,
        amount: Double,
        category: String,
        ledgerType: BusinessLedgerType,
        clientId: Long?,
        projectId: Long?,
        date: Long,
        note: String,
    ) {
        viewModelScope.launch {
            businessRepository.addTransaction(
                BusinessTransaction(
                    type = type,
                    amountCents = MoneyMath.toCents(amount),
                    category = category,
                    ledgerType = ledgerType,
                    clientId = clientId,
                    projectId = projectId,
                    date = date,
                    note = note.trim(),
                ),
            )
        }
    }

    fun deleteTransaction(transaction: BusinessTransaction) {
        viewModelScope.launch { businessRepository.deleteTransaction(transaction) }
    }

    fun askAssistant(promptId: String, promptLabel: String) {
        val response = BusinessAiAssistantService.respond(
            promptId = promptId,
            dashboard = dashboard.value,
            invoices = invoices.value,
            clients = clients.value,
            transactions = transactions.value,
            forecast = cashflowForecast.value,
            invoiceRisks = invoiceRisks.value,
        )
        _assistantMessages.value = _assistantMessages.value + listOf(
            BusinessAssistantMessage(BusinessAssistantRole.USER, promptLabel),
            BusinessAssistantMessage(BusinessAssistantRole.ASSISTANT, response),
        )
    }

    fun clearAssistantHistory() {
        _assistantMessages.value = emptyList()
    }

    fun updateAutomationSettings(transform: (BusinessBranchSettings) -> BusinessBranchSettings) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings(transform)
            val settings = preferencesStore.businessBranchSettings.first()
            businessSmsScheduler?.syncSchedule(settings)
            workflowDebtScheduler?.syncSchedule(settings)
            businessAutomationCenterScheduler?.syncSchedule(settings)
        }
    }

    fun runWorkflowDebtAutomationNow(context: Context) {
        viewModelScope.launch {
            val settings = preferencesStore.businessBranchSettings.first()
            businessApi?.runAutomation(context, settings)
                ?: businessCommunicationUseCases?.runWorkflowAutomation(context, settings)
        }
    }

    fun addRecurringRule(rule: RecurringBusinessRule) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { settings ->
                val updated = settings.recurringRules() + rule
                settings.copy(recurringRulesJson = RecurringBusinessRuleCodec.encode(updated))
            }
        }
    }

    fun deleteRecurringRule(ruleId: String) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { settings ->
                val updated = settings.recurringRules().filterNot { it.id == ruleId }
                settings.copy(recurringRulesJson = RecurringBusinessRuleCodec.encode(updated))
            }
        }
    }

    fun toggleRecurringRule(ruleId: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.updateBusinessBranchSettings { settings ->
                val updated = settings.recurringRules().map { rule ->
                    if (rule.id == ruleId) rule.copy(enabled = enabled) else rule
                }
                settings.copy(recurringRulesJson = RecurringBusinessRuleCodec.encode(updated))
            }
        }
    }

    fun runRecurringRulesNow() {
        viewModelScope.launch {
            processRecurringRules(branchSettings.value)
        }
    }

    fun runAutomationNow(context: Context) {
        runRecurringRulesNow()
        viewModelScope.launch {
            val settings = preferencesStore.businessBranchSettings.first()
            businessApi?.runAutomation(context, settings)
                ?: businessCommunicationUseCases?.runWorkflowAutomation(context, settings)
        }
    }

    fun rescheduleInvoiceReminders() {
        viewModelScope.launch {
            val settings = branchSettings.value
            invoiceReminderScheduler?.syncAll(invoices.value, settings)
            businessSmsScheduler?.syncSchedule(settings)
            workflowDebtScheduler?.syncSchedule(settings)
            businessAutomationCenterScheduler?.syncSchedule(settings)
        }
    }

    suspend fun sendSms(
        recipients: List<BusinessSmsRecipient>,
        body: String,
        templateId: BusinessSmsTemplateId?,
        useDirectSend: Boolean,
    ): BusinessSmsSendResult {
        return businessSmsUseCases.sendSms(
            BusinessSmsSendRequest(
                recipients = recipients,
                body = body,
                templateId = templateId,
                useDirectSend = useDirectSend,
            ),
        )
    }

    suspend fun sendBulkSms(
        clientIds: List<Long>,
        body: String,
        templateId: BusinessSmsTemplateId?,
        useDirectSend: Boolean,
    ): BusinessSmsSendResult {
        return businessSmsUseCases.sendBulkToClients(clientIds, body, templateId, useDirectSend)
    }

    suspend fun renderSmsTemplate(templateId: BusinessSmsTemplateId, clientId: Long?): String {
        val businessName = branchSettings.value.businessName.ifBlank { "Business" }
        return businessSmsUseCases.renderTemplate(templateId, clientId, businessName)
    }

    suspend fun aiGenerateSms(
        templateId: BusinessSmsTemplateId,
        clientId: Long?,
        tone: BusinessSmsTone,
    ): String {
        val businessName = branchSettings.value.businessName.ifBlank { "Business" }
        return businessSmsUseCases.aiGenerateSms(templateId, clientId, businessName, tone)
    }

    class Factory(
        private val businessRepository: BusinessRepository,
        private val preferencesStore: MoneyPreferencesStore,
        private val businessUseCases: BusinessUseCases,
        private val businessSmsUseCases: BusinessSmsUseCases,
        private val invoiceReminderScheduler: BusinessInvoiceReminderScheduler? = null,
        private val businessSmsScheduler: BusinessSmsScheduler? = null,
        private val workflowDebtScheduler: WorkflowDebtScheduler? = null,
        private val businessAutomationCenterScheduler: BusinessAutomationCenterScheduler? = null,
        private val businessCommunicationUseCases: BusinessCommunicationUseCases? = null,
        private val businessApi: BusinessApi? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusinessViewModel::class.java)) {
                return BusinessViewModel(
                    businessRepository,
                    preferencesStore,
                    businessUseCases,
                    businessSmsUseCases,
                    invoiceReminderScheduler,
                    businessSmsScheduler,
                    workflowDebtScheduler,
                    businessAutomationCenterScheduler,
                    businessCommunicationUseCases,
                    businessApi,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
