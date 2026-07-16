package com.example.money.ui.business.workflow

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.business.BusinessCommunicationSendResult
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.domain.usecase.BusinessCommunicationUseCases
import com.example.money.domain.usecase.BusinessWorkflowUseCases
import com.example.money.domain.util.MoneyMath
import com.example.money.domain.util.ai.BusinessSmsTone
import com.example.money.messaging.MessagingChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkflowRoute {
    data object Categories : WorkflowRoute
    data class Processes(val categoryId: Long, val categoryName: String) : WorkflowRoute
    data class Bills(val processId: Long, val processName: String) : WorkflowRoute
    data class BillDetail(val billId: Long) : WorkflowRoute
    data class CreateProcess(val categoryId: Long) : WorkflowRoute
    data class CreateBillInfo(val processId: Long) : WorkflowRoute
    data class CreateBillParticipants(val processId: Long) : WorkflowRoute
    data class CreateBillDistribution(val processId: Long) : WorkflowRoute
    data class MessageComposer(val billId: Long) : WorkflowRoute
    data object CommunicationCenter : WorkflowRoute
}

@OptIn(ExperimentalCoroutinesApi::class)
class BusinessWorkflowViewModel(
    private val workflowUseCases: BusinessWorkflowUseCases,
    private val communicationUseCases: BusinessCommunicationUseCases,
    private val preferencesStore: MoneyPreferencesStore,
) : ViewModel() {

    private val _route = MutableStateFlow<WorkflowRoute>(WorkflowRoute.Categories)
    val route: StateFlow<WorkflowRoute> = _route.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _selectedDebtIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedDebtIds: StateFlow<Set<Long>> = _selectedDebtIds.asStateFlow()

    private val _messageBody = MutableStateFlow("")
    val messageBody: StateFlow<String> = _messageBody.asStateFlow()

    private val _debtFilter = MutableStateFlow(WorkflowDebtFilter.ALL)
    val debtFilter: StateFlow<WorkflowDebtFilter> = _debtFilter.asStateFlow()

    private val _billDraft = MutableStateFlow<CreateBillDraft?>(null)
    val billDraft: StateFlow<CreateBillDraft?> = _billDraft.asStateFlow()

    private val _scopedCategoryId = MutableStateFlow<Long?>(null)
    private val _scopedProcessId = MutableStateFlow<Long?>(null)
    private val _scopedBillId = MutableStateFlow<Long?>(null)

    val categories: StateFlow<List<WorkflowCategory>> = workflowUseCases.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val processes: StateFlow<List<WorkflowProcess>> = _scopedCategoryId
        .flatMapLatest { categoryId ->
            if (categoryId != null) workflowUseCases.observeProcesses(categoryId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bills: StateFlow<List<WorkflowBill>> = _scopedProcessId
        .flatMapLatest { processId ->
            if (processId != null) workflowUseCases.observeBills(processId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val debts: StateFlow<List<WorkflowDebtWithParticipant>> = _scopedBillId
        .flatMapLatest { billId ->
            if (billId != null) workflowUseCases.observeDebtsWithParticipants(billId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredDebts: StateFlow<List<WorkflowDebtWithParticipant>> = combine(debts, _debtFilter) { list, filter ->
        applyDebtFilter(list, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentBill: StateFlow<WorkflowBill?> = _scopedBillId
        .flatMapLatest { billId ->
            if (billId != null) {
                flow { emit(workflowUseCases.getBill(billId)) }
            } else {
                flowOf<WorkflowBill?>(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val breadcrumb: StateFlow<WorkflowBreadcrumb> = _route
        .flatMapLatest { route -> flow { emit(buildBreadcrumb(route)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkflowBreadcrumb(emptyList()))

    fun navigateToCategories() {
        _route.value = WorkflowRoute.Categories
        _scopedCategoryId.value = null
        _scopedProcessId.value = null
        _scopedBillId.value = null
        _selectedCategoryId.value = null
        _selectedDebtIds.value = emptySet()
        _messageBody.value = ""
        _debtFilter.value = WorkflowDebtFilter.ALL
        _billDraft.value = null
    }

    fun bindCategoriesScope() {
        navigateToCategories()
    }

    fun bindCategoryScope(categoryId: Long) {
        _scopedCategoryId.value = categoryId
        _scopedProcessId.value = null
        _scopedBillId.value = null
        _selectedCategoryId.value = categoryId
        val category = categories.value.find { it.id == categoryId }
        if (category != null) {
            _route.value = WorkflowRoute.Processes(category.id, category.name)
        }
    }

    fun bindProcessScope(processId: Long) {
        viewModelScope.launch {
            val process = workflowUseCases.getProcess(processId) ?: return@launch
            _scopedCategoryId.value = process.categoryId
            _scopedProcessId.value = processId
            _scopedBillId.value = null
            _route.value = WorkflowRoute.Bills(process.id, process.name)
        }
    }

    fun bindBillScope(billId: Long) {
        _scopedBillId.value = billId
        _route.value = WorkflowRoute.BillDetail(billId)
        _selectedDebtIds.value = emptySet()
        _messageBody.value = ""
        _debtFilter.value = WorkflowDebtFilter.ALL
    }

    fun bindBillWizardScope(processId: Long) {
        _scopedProcessId.value = processId
        _scopedBillId.value = null
    }

    fun bindCommunicationCenterScope() {
        _route.value = WorkflowRoute.CommunicationCenter
    }

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun continueFromCategorySelection() {
        val categoryId = _selectedCategoryId.value ?: return
        val category = categories.value.find { it.id == categoryId } ?: return
        _route.value = WorkflowRoute.Processes(category.id, category.name)
    }

    fun openCategory(category: WorkflowCategory) {
        bindCategoryScope(category.id)
    }

    fun openCreateProcess(categoryId: Long) {
        _scopedCategoryId.value = categoryId
        _route.value = WorkflowRoute.CreateProcess(categoryId)
    }

    fun openProcess(process: WorkflowProcess) {
        _scopedCategoryId.value = process.categoryId
        _scopedProcessId.value = process.id
        _scopedBillId.value = null
        _route.value = WorkflowRoute.Bills(process.id, process.name)
    }

    fun startCreateBill(processId: Long) {
        _billDraft.value = CreateBillDraft(processId = processId)
        bindBillWizardScope(processId)
        _route.value = WorkflowRoute.CreateBillInfo(processId)
    }

    fun openBill(billId: Long) {
        bindBillScope(billId)
    }

    fun bindMessageComposerScope(billId: Long) {
        _scopedBillId.value = billId
        val current = _route.value
        if (current !is WorkflowRoute.MessageComposer || current.billId != billId) {
            _route.value = WorkflowRoute.MessageComposer(billId)
        }
        if (_selectedDebtIds.value.isEmpty() && debts.value.isNotEmpty()) {
            _selectedDebtIds.value = BusinessWorkflowDebtSelection.resolveSelectedDebtIds(
                debts = debts.value,
                currentSelection = _selectedDebtIds.value,
            )
        }
    }

    fun openMessageComposer(billId: Long) {
        val current = _route.value
        if (current is WorkflowRoute.MessageComposer &&
            current.billId == billId &&
            _scopedBillId.value == billId
        ) {
            return
        }
        _scopedBillId.value = billId
        _route.value = WorkflowRoute.MessageComposer(billId)
        _selectedDebtIds.value = BusinessWorkflowDebtSelection.resolveSelectedDebtIds(
            debts = debts.value,
            currentSelection = _selectedDebtIds.value,
        )
        if (_messageBody.value.isBlank()) {
            generateMessageForFirstSelected(billId)
        }
    }

    fun openCommunicationCenter() {
        bindCommunicationCenterScope()
    }

    fun updateBillDraft(transform: (CreateBillDraft) -> CreateBillDraft) {
        _billDraft.value = _billDraft.value?.let(transform)
    }

    fun continueToParticipants() {
        val draft = _billDraft.value ?: return
        _route.value = WorkflowRoute.CreateBillParticipants(draft.processId)
    }

    fun continueToDistribution() {
        val draft = _billDraft.value ?: return
        _route.value = WorkflowRoute.CreateBillDistribution(draft.processId)
    }

    fun splitDraftEqually() {
        val draft = _billDraft.value ?: return
        val totalCents = MoneyMath.toCents(draft.totalInput.replace(',', '.').toDoubleOrNull() ?: 0.0)
        val selected = draft.participants.filter { it.selected }
        if (totalCents <= 0 || selected.isEmpty()) return
        val shares = workflowUseCases.splitEqually(totalCents, selected.size)
        var shareIndex = 0
        updateBillDraft { current ->
            current.copy(
                participants = current.participants.map { row ->
                    if (!row.selected) row else {
                        val cents = shares.getOrElse(shareIndex) { 0L }
                        shareIndex++
                        row.copy(amountInput = formatAmountInput(cents))
                    }
                },
            )
        }
    }

    fun resetDraftAmounts() {
        updateBillDraft { draft ->
            draft.copy(participants = draft.participants.map { it.copy(amountInput = "") })
        }
    }

    fun aiDistributeDraft() {
        splitDraftEqually()
    }

    fun createBillFromDraft(onCreated: (Long) -> Unit) {
        val draft = _billDraft.value ?: return
        val totalCents = MoneyMath.toCents(draft.totalInput.replace(',', '.').toDoubleOrNull() ?: 0.0)
        val participants = draft.toParticipantDrafts()
        if (draft.title.isBlank() || totalCents <= 0 || participants.isEmpty()) return
        viewModelScope.launch {
            val id = workflowUseCases.createBillWithDebts(
                processId = draft.processId,
                title = draft.title,
                totalAmountCents = totalCents,
                description = draft.description,
                periodLabel = draft.periodLabel,
                participants = participants,
            )
            _billDraft.value = null
            onCreated(id)
        }
    }

    fun setDebtFilter(filter: WorkflowDebtFilter) {
        _debtFilter.value = filter
    }

    private fun applyDebtFilter(
        list: List<WorkflowDebtWithParticipant>,
        filter: WorkflowDebtFilter,
    ): List<WorkflowDebtWithParticipant> = when (filter) {
        WorkflowDebtFilter.ALL -> list
        WorkflowDebtFilter.UNPAID -> list.filter { it.debt.status != WorkflowDebtStatus.PAID }
        WorkflowDebtFilter.PAID -> list.filter { it.debt.status == WorkflowDebtStatus.PAID }
    }

    fun navigateBack() {
        when (val current = _route.value) {
            WorkflowRoute.Categories -> Unit
            WorkflowRoute.CommunicationCenter -> navigateToCategories()
            is WorkflowRoute.Processes -> navigateToCategories()
            is WorkflowRoute.CreateProcess -> {
                val category = categories.value.find { it.id == current.categoryId }
                if (category != null) openCategory(category) else navigateToCategories()
            }
            is WorkflowRoute.Bills -> viewModelScope.launch {
                val process = workflowUseCases.getProcess(current.processId)
                if (process != null) {
                    workflowUseCases.getCategory(process.categoryId)?.let { openCategory(it) }
                        ?: navigateToCategories()
                } else navigateToCategories()
            }
            is WorkflowRoute.CreateBillInfo -> {
                val process = processes.value.find { it.id == current.processId }
                if (process != null) openProcess(process) else navigateToCategories()
            }
            is WorkflowRoute.CreateBillParticipants -> {
                _route.value = WorkflowRoute.CreateBillInfo(current.processId)
            }
            is WorkflowRoute.CreateBillDistribution -> {
                _route.value = WorkflowRoute.CreateBillParticipants(current.processId)
            }
            is WorkflowRoute.BillDetail -> viewModelScope.launch {
                val bill = workflowUseCases.getBill(current.billId)
                if (bill != null) {
                    workflowUseCases.getProcess(bill.processId)?.let { openProcess(it) }
                        ?: navigateToCategories()
                } else navigateToCategories()
            }
            is WorkflowRoute.MessageComposer -> openBill(current.billId)
        }
    }

    fun createProcess(categoryId: Long, name: String, description: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = workflowUseCases.createProcess(categoryId, name, description)
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
            val first = debts.value.firstOrNull { it.debt.id in _selectedDebtIds.value }
                ?: debts.value.firstOrNull { it.debt.status != WorkflowDebtStatus.PAID }
                ?: return@launch
            val settings = preferencesStore.businessBranchSettings.first()
            _messageBody.value = communicationUseCases.aiGenerateWorkflowMessage(
                billId = billId,
                debt = first,
                businessName = settings.businessName,
            )
        }
    }

    fun aiImproveMessage(billId: Long) = generateMessageForFirstSelected(billId)

    fun aiShortenMessage() {
        val lines = _messageBody.value.lines().filter { it.isNotBlank() }
        _messageBody.value = lines.take(3).joinToString("\n")
    }

    fun aiFormalMessage(billId: Long) {
        viewModelScope.launch {
            val first = debts.value.firstOrNull { it.debt.id in _selectedDebtIds.value } ?: return@launch
            val settings = preferencesStore.businessBranchSettings.first()
            _messageBody.value = communicationUseCases.aiGenerateWorkflowMessage(
                billId = billId,
                debt = first,
                businessName = settings.businessName,
                tone = BusinessSmsTone.FORMAL,
            )
        }
    }

    private fun selectedDebts(): List<WorkflowDebtWithParticipant> =
        BusinessWorkflowDebtSelection.debtsForSend(debts.value, _selectedDebtIds.value)

    suspend fun sendReminders(
        context: Context,
        billId: Long,
        channel: MessagingChannel,
        useDirectSms: Boolean = false,
    ): BusinessCommunicationSendResult {
        val settings = preferencesStore.businessBranchSettings.first()
        return communicationUseCases.sendWorkflowReminders(
            context = context,
            billId = billId,
            channel = channel,
            selectedDebts = selectedDebts(),
            messageBody = _messageBody.value,
            useDirectSms = useDirectSms,
            businessName = settings.businessName,
        )
    }

    suspend fun sendPushReminders(context: Context, billId: Long): BusinessCommunicationSendResult {
        val settings = preferencesStore.businessBranchSettings.first()
        return communicationUseCases.sendWorkflowPushReminders(
            context = context,
            billId = billId,
            selectedDebts = selectedDebts(),
            messageBody = _messageBody.value,
            businessName = settings.businessName,
        )
    }

    suspend fun sendAllChannels(
        context: Context,
        billId: Long,
        useDirectSms: Boolean,
        includePush: Boolean,
    ): BusinessCommunicationSendResult {
        val settings = preferencesStore.businessBranchSettings.first()
        return communicationUseCases.sendWorkflowBulk(
            context = context,
            billId = billId,
            channels = listOf(
                MessagingChannel.WHATSAPP,
                MessagingChannel.SMS,
                MessagingChannel.TELEGRAM,
                MessagingChannel.EMAIL,
            ),
            selectedDebts = selectedDebts(),
            messageBody = _messageBody.value,
            useDirectSms = useDirectSms,
            includePush = includePush,
            businessName = settings.businessName,
        )
    }

    fun markDebtPaid(debtId: Long) {
        viewModelScope.launch { workflowUseCases.markDebtPaid(debtId) }
    }

    private suspend fun buildBreadcrumb(route: WorkflowRoute): WorkflowBreadcrumb {
        val steps = mutableListOf<String>()
        when (route) {
            WorkflowRoute.Categories, WorkflowRoute.CommunicationCenter -> Unit
            is WorkflowRoute.Processes -> steps += route.categoryName
            is WorkflowRoute.CreateProcess -> categories.value.find { it.id == route.categoryId }?.name?.let { steps += it }
            is WorkflowRoute.Bills -> appendProcessBreadcrumb(steps, route.processId, route.processName)
            is WorkflowRoute.CreateBillInfo -> appendProcessBreadcrumb(steps, route.processId, null)
            is WorkflowRoute.CreateBillParticipants -> appendProcessBreadcrumb(steps, route.processId, null)
            is WorkflowRoute.CreateBillDistribution -> appendProcessBreadcrumb(steps, route.processId, null)
            is WorkflowRoute.BillDetail -> appendBillBreadcrumb(steps, route.billId)
            is WorkflowRoute.MessageComposer -> appendBillBreadcrumb(steps, route.billId)
        }
        return WorkflowBreadcrumb(steps)
    }

    private suspend fun appendProcessBreadcrumb(steps: MutableList<String>, processId: Long, processName: String?) {
        val process = workflowUseCases.getProcess(processId) ?: return
        workflowUseCases.getCategory(process.categoryId)?.name?.let { steps += it }
        steps += processName ?: process.name
    }

    private suspend fun appendBillBreadcrumb(steps: MutableList<String>, billId: Long) {
        val bill = workflowUseCases.getBill(billId) ?: return
        appendProcessBreadcrumb(steps, bill.processId, null)
        steps += bill.title
    }

    private fun formatAmountInput(cents: Long): String {
        val value = MoneyMath.fromCents(cents)
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    class Factory(
        private val workflowUseCases: BusinessWorkflowUseCases,
        private val communicationUseCases: BusinessCommunicationUseCases,
        private val preferencesStore: MoneyPreferencesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BusinessWorkflowViewModel::class.java)) {
                return BusinessWorkflowViewModel(workflowUseCases, communicationUseCases, preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
