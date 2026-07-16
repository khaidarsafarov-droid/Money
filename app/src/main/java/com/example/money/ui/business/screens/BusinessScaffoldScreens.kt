package com.example.money.ui.business.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.ui.business.workflow.CreateBillDraft
import com.example.money.ui.business.workflow.WorkflowBreadcrumb
import com.example.money.ui.business.workflow.WorkflowCategoriesScreen
import com.example.money.ui.business.workflow.WorkflowCommunicationCenterScreen
import com.example.money.ui.business.workflow.WorkflowCreateBillDistributionScreen
import com.example.money.ui.business.workflow.WorkflowCreateBillInfoScreen
import com.example.money.ui.business.workflow.WorkflowCreateBillParticipantsScreen
import com.example.money.ui.business.workflow.WorkflowCreateProcessScreen
import com.example.money.ui.business.workflow.WorkflowDebtFilter
import com.example.money.ui.business.workflow.WorkflowDebtListScreen
import com.example.money.ui.business.workflow.WorkflowDebtStats
import com.example.money.ui.business.workflow.WorkflowMessageComposerScreen
import com.example.money.ui.business.workflow.WorkflowProcessesScreen
import com.example.money.ui.business.workflow.WorkflowBillsScreen

/**
 * Scaffold screen names from the Business UI spec — thin wrappers over workflow composables.
 */

@Composable
fun CategoriesScreen(
    categories: List<WorkflowCategory>,
    selectedCategoryId: Long?,
    onCategoryClick: (Long) -> Unit,
    onContinue: () -> Unit,
    onOpenCommunicationCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowCategoriesScreen(
        categories = categories,
        selectedCategoryId = selectedCategoryId,
        onSelectCategory = onCategoryClick,
        onContinue = onContinue,
        onOpenCommunicationCenter = onOpenCommunicationCenter,
        modifier = modifier,
    )
}

@Composable
fun CategoryFlowScreen(
    categoryName: String,
    processes: List<WorkflowProcess>,
    onProcessClick: (WorkflowProcess) -> Unit,
    onNewProcessClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowProcessesScreen(
        categoryName = categoryName,
        processes = processes,
        onProcessClick = onProcessClick,
        onCreateProcess = onNewProcessClick,
        modifier = modifier,
    )
}

@Composable
fun ProcessBillsScreen(
    processName: String,
    bills: List<WorkflowBill>,
    onBillClick: (WorkflowBill) -> Unit,
    onCreateBillClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowBillsScreen(
        processName = processName,
        bills = bills,
        onBillClick = onBillClick,
        onCreateBill = onCreateBillClick,
        modifier = modifier,
    )
}

@Composable
fun ProcessCreateScreen(
    onProcessCreated: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowCreateProcessScreen(
        onCreate = onProcessCreated,
        modifier = modifier,
    )
}

@Composable
fun BillCreateScreen(
    draft: CreateBillDraft?,
    onDraftChange: (CreateBillDraft) -> Unit,
    onBillCreated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowCreateBillInfoScreen(
        draft = draft,
        onDraftChange = onDraftChange,
        onContinue = onBillCreated,
        modifier = modifier,
    )
}

@Composable
fun ParticipantsScreen(
    draft: CreateBillDraft?,
    onDraftChange: (CreateBillDraft) -> Unit,
    onParticipantsSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowCreateBillParticipantsScreen(
        draft = draft,
        onDraftChange = onDraftChange,
        onContinue = onParticipantsSelected,
        modifier = modifier,
    )
}

@Composable
fun DebtsScreen(
    draft: CreateBillDraft?,
    onDraftChange: (CreateBillDraft) -> Unit,
    onSplitEqually: () -> Unit,
    onAiDistribute: () -> Unit,
    onReset: () -> Unit,
    onDebtsConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowCreateBillDistributionScreen(
        draft = draft,
        onDraftChange = onDraftChange,
        onSplitEqually = onSplitEqually,
        onAiDistribute = onAiDistribute,
        onReset = onReset,
        onCreateDebts = onDebtsConfirmed,
        modifier = modifier,
    )
}

@Composable
fun BillDebtsScreen(
    bill: WorkflowBill?,
    debts: List<WorkflowDebtWithParticipant>,
    stats: WorkflowDebtStats,
    selectedDebtIds: Set<Long>,
    debtFilter: WorkflowDebtFilter,
    onFilterChange: (WorkflowDebtFilter) -> Unit,
    onToggleDebt: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onMarkPaid: (Long) -> Unit,
    onSendReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowDebtListScreen(
        bill = bill,
        debts = debts,
        stats = stats,
        selectedDebtIds = selectedDebtIds,
        debtFilter = debtFilter,
        onFilterChange = onFilterChange,
        onToggleDebt = onToggleDebt,
        onSelectAll = onSelectAll,
        onMarkPaid = onMarkPaid,
        onSendReminders = onSendReminders,
        modifier = modifier,
    )
}

@Composable
fun MessageComposerScreen(
    messageBody: String,
    selectedCount: Int,
    onMessageChange: (String) -> Unit,
    onAiImprove: () -> Unit,
    onAiShorten: () -> Unit,
    onAiFormal: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendSms: () -> Unit,
    onSendPush: () -> Unit,
    onSendAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowMessageComposerScreen(
        messageBody = messageBody,
        selectedCount = selectedCount,
        onMessageChange = onMessageChange,
        onAiImprove = onAiImprove,
        onAiShorten = onAiShorten,
        onAiFormal = onAiFormal,
        onSendWhatsApp = onSendWhatsApp,
        onSendSms = onSendSms,
        onSendPush = onSendPush,
        onSendAll = onSendAll,
        modifier = modifier,
    )
}

@Composable
fun CommunicationCenterScreen(
    onAutomationClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onOpenSmsCompose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkflowCommunicationCenterScreen(
        onOpenSmsCompose = onOpenSmsCompose,
        onOpenHistory = onHistoryClick,
        onOpenTemplates = onTemplatesClick,
        onOpenAutomation = onAutomationClick,
        modifier = modifier,
    )
}

@Composable
fun AutomationScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    content()
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    content()
}

@Composable
fun TemplatesScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    content()
}
