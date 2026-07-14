package com.example.money.ui.business

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.FinanceBranch
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardFilters
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.ProjectStatus
import com.example.money.domain.repository.BusinessRepository
import com.example.money.domain.util.BusinessFinanceEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.BusinessBranchSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BusinessTab {
    Dashboard,
    Clients,
    Projects,
    Invoices,
    Cashflow,
    Reports,
    Settings,
}

class BusinessViewModel(
    private val businessRepository: BusinessRepository,
    private val preferencesStore: MoneyPreferencesStore,
) : ViewModel() {

    private val monthRange = BusinessFinanceEngine.monthRange()

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

    private val _filters = MutableStateFlow(BusinessDashboardFilters())
    val filters: StateFlow<BusinessDashboardFilters> = _filters.asStateFlow()

    val dashboard: StateFlow<BusinessDashboardSummary> = _filters
        .flatMapLatest { filters ->
            businessRepository.observeDashboard(monthRange.first, monthRange.second, filters)
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

    init {
        viewModelScope.launch {
            businessRepository.refreshInvoiceStatuses()
        }
    }

    fun selectTab(tab: BusinessTab) {
        _selectedTab.value = tab
    }

    fun updateFilters(transform: (BusinessDashboardFilters) -> BusinessDashboardFilters) {
        _filters.value = transform(_filters.value)
    }

    fun clearFilters() {
        _filters.value = BusinessDashboardFilters()
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
        }
    }

    fun markInvoicePaid(invoice: BusinessInvoice) {
        viewModelScope.launch {
            businessRepository.markInvoicePaid(invoice.id)
            businessRepository.refreshInvoiceStatuses()
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

    class Factory(
        private val businessRepository: BusinessRepository,
        private val preferencesStore: MoneyPreferencesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusinessViewModel::class.java)) {
                return BusinessViewModel(businessRepository, preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
