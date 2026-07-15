package com.example.money.ui.business.workflow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.domain.model.business.workflow.WorkflowWhatsAppSendResult
import com.example.money.domain.usecase.BusinessWorkflowUseCases
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkflowRoute {
    data object Categories : WorkflowRoute
    data class Processes(val categoryId: Long, val categoryName: String) : WorkflowRoute
    data class Bills(val processId: Long, val processName: String) : WorkflowRoute
    data class BillDetail(val billId: Long) : WorkflowRoute
    data class CreateProcess(val categoryId: Long) : WorkflowRoute
    data class CreateBill(val processId: Long) : WorkflowRoute
}

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessWorkflowViewModel(
    private val workflowUseCases: BusinessWorkflowUseCases,
    private val preferencesStore: MoneyPreferencesStore,
) : ViewModel() {

    private val _route = MutableStateFlow<WorkflowRoute>(WorkflowRoute.Categories)
    val route: StateFlow<WorkflowRoute> = _route.asStateFlow()

    private val _selectedDebtIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDebtIds: StateFlow<Set<Long>> = _selectedDebtIds.asStateFlow()

    private val _messageBody = MutableStateFlow("")
    val messageBody: StateFlow<String> = _messageBody.asStateFlow()

    val categories: StateFlow<List<WorkflowCategory>> = workflowUseCases.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val processes: StateFlow<List<WorkflowProcess>> = _route
        .flatMapLatest { route ->
            val categoryId = when (route) {
                is WorkflowRoute.Processes -> route.categoryId
                is WorkflowRoute.CreateProcess -> route.categoryId
                else -> null
            }
            if (categoryId != null) {
                workflowUseCases.observeProcesses(categoryId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bills: StateFlow<List<WorkflowBill>> = _route
        .flatMapLatest { route ->
            val processId = when (route) {
                is WorkflowRoute.Bills -> route.processId
                is WorkflowRoute.CreateBill -> route.processId
                else -> null
            }
            if (processId != null) {
                workflowUseCases.observeBills(processId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val debts: StateFlow<List<WorkflowDebtWithParticipant>> = _route
        .flatMapLatest { route ->
            when (route) {
                is WorkflowRoute.BillDetail -> workflowUseCases.observeDebtsWithParticipants(route.billId)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun navigateToCategories() {
        _route.value = WorkflowRoute.Categories
        _selectedDebtIds.value = emptySet()
        _messageBody.value = ""
    }

    fun openCategory(category: WorkflowCategory) {
        _route.value = WorkflowRoute.Processes(category.id, category.name)
    }

    fun openCreateProcess(categoryId: Long) {
        _route.value = WorkflowRoute.CreateProcess(categoryId)
    }

    fun openProcess(process: WorkflowProcess) {
        _route.value = WorkflowRoute.Bills(process.id, process.name)
    }

    fun openCreateBill(processId: Long) {
        _route.value = WorkflowRoute.CreateBill(processId)
    }

    fun openBill(billId: Long) {
        _route.value = WorkflowRoute.BillDetail(billId)
        _selectedDebtIds.value = emptySet()
        _messageBody.value = ""
    }

    fun navigateBack() {
        when (val current = _route.value) {
            is WorkflowRoute.Categories -> Unit
            is WorkflowRoute.Processes -> navigateToCategories()
            is WorkflowRoute.CreateProcess -> {
                val category = categories.value.find { it.id == current.categoryId }
                if (category != null) openCategory(category) else navigateToCategories()
            }
            is WorkflowRoute.Bills -> {
                viewModelScope.launch {
                    val process = workflowUseCases.getProcess(current.processId)
                    if (process != null) {
                        val category = workflowUseCases.getCategory(process.categoryId)
                        if (category != null) openCategory(category) else navigateToCategories()
                    } else {
                        navigateToCategories()
                    }
                }
            }
            is WorkflowRoute.CreateBill -> {
                val process = processes.value.find { it.id == current.processId }
                if (process != null) openProcess(process) else navigateToCategories()
            }
            is WorkflowRoute.BillDetail -> {
                viewModelScope.launch {
                    val bill = workflowUseCases.getBill(current.billId)
                    if (bill != null) {
                        val process = workflowUseCases.getProcess(bill.processId)
                        if (process != null) openProcess(process) else navigateToCategories()
                    } else {
                        navigateToCategories()
                    }
                }
            }
        }
    }

    fun createProcess(categoryId: Long, name: String, description: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = workflowUseCases.createProcess(categoryId, name, description)
            onCreated(id)
        }
    }

    fun createBill(
        processId: Long,
        title: String,
        totalAmountCents: Long,
        description: String,
        periodLabel: String,
        participants: List<WorkflowParticipantDraft>,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val id = workflowUseCases.createBillWithDebts(
                processId = processId,
                title = title,
                totalAmountCents = totalAmountCents,
                description = description,
                periodLabel = periodLabel,
                participants = participants,
            )
            onCreated(id)
        }
    }

    fun toggleDebtSelection(debtId: Long) {
        _selectedDebtIds.value = if (debtId in _selectedDebtIds.value) {
            _selectedDebtIds.value - debtId
        } else {
            _selectedDebtIds.value + debtId
        }
    }

    fun selectAllDebts(debtIds: List<Long>) {
        _selectedDebtIds.value = debtIds.toSet()
    }

    fun clearDebtSelection() {
        _selectedDebtIds.value = emptySet()
    }

    fun updateMessageBody(body: String) {
        _messageBody.value = body
    }

    fun generateMessageForFirstSelected(billId: Long) {
        viewModelScope.launch {
            val first = debts.value.firstOrNull { it.debt.id in _selectedDebtIds.value } ?: return@launch
            val settings = preferencesStore.businessBranchSettings.first()
            _messageBody.value = workflowUseCases.aiGenerateReminderMessage(
                billId = billId,
                debt = first,
                businessName = settings.businessName,
            )
        }
    }

    suspend fun sendWhatsAppReminders(
        context: Context,
        billId: Long,
    ): WorkflowWhatsAppSendResult {
        val selected = debts.value.filter { it.debt.id in _selectedDebtIds.value }
        return workflowUseCases.sendWhatsAppReminders(
            context = context,
            billId = billId,
            selectedDebts = selected,
            messageBody = _messageBody.value,
        )
    }

    fun markDebtPaid(debtId: Long) {
        viewModelScope.launch { workflowUseCases.markDebtPaid(debtId) }
    }

    class Factory(
        private val workflowUseCases: BusinessWorkflowUseCases,
        private val preferencesStore: MoneyPreferencesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusinessWorkflowViewModel::class.java)) {
                return BusinessWorkflowViewModel(workflowUseCases, preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
