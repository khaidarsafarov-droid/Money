package com.example.money.ui.business.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.money.R
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.ui.business.BusinessTab
import com.example.money.ui.business.screens.AutomationScreen
import com.example.money.ui.business.screens.BillCreateScreen
import com.example.money.ui.business.screens.BillDebtsScreen
import com.example.money.ui.business.screens.CategoriesScreen
import com.example.money.ui.business.screens.CategoryFlowScreen
import com.example.money.ui.business.screens.CommunicationCenterScreen
import com.example.money.ui.business.screens.DebtsScreen
import com.example.money.ui.business.screens.HistoryScreen
import com.example.money.ui.business.screens.MessageComposerScreen
import com.example.money.ui.business.screens.ParticipantsScreen
import com.example.money.ui.business.screens.ProcessBillsScreen
import com.example.money.ui.business.screens.ProcessCreateScreen
import com.example.money.ui.business.screens.TemplatesScreen
import com.example.money.ui.business.sms.BusinessSmsPanel
import com.example.money.ui.business.workflow.BusinessWorkflowDebtSelection
import com.example.money.ui.business.workflow.BusinessWorkflowViewModel
import com.example.money.ui.business.workflow.CreateBillDraft
import com.example.money.ui.business.workflow.WorkflowDebtFilter
import com.example.money.ui.business.workflow.workflowDebtStatsFrom

@Composable
fun BusinessNavHost(
    navController: NavHostController,
    viewModel: BusinessWorkflowViewModel,
    categories: List<WorkflowCategory>,
    selectedCategoryId: Long?,
    processes: List<WorkflowProcess>,
    bills: List<WorkflowBill>,
    debts: List<WorkflowDebtWithParticipant>,
    allDebts: List<WorkflowDebtWithParticipant>,
    selectedDebtIds: Set<Long>,
    messageBody: String,
    debtFilter: WorkflowDebtFilter,
    billDraft: CreateBillDraft?,
    currentBill: WorkflowBill?,
    onSelectBusinessTab: (BusinessTab, BusinessSmsPanel?) -> Unit,
    onSendWhatsApp: (Long) -> Unit,
    onSendSms: (Long) -> Unit,
    onSendPush: (Long) -> Unit,
    onSendAll: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BusinessRoute.Categories.route,
        modifier = modifier,
    ) {
        composable(BusinessRoute.Categories.route) {
            CategoriesScreen(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategoryClick = viewModel::selectCategory,
                onContinue = {
                    val categoryId = selectedCategoryId ?: return@CategoriesScreen
                    viewModel.bindCategoryScope(categoryId)
                    navController.navigate(BusinessRoute.CategoryFlow.create(categoryId))
                },
                onOpenCommunicationCenter = {
                    viewModel.bindCommunicationCenterScope()
                    navController.navigate(BusinessRoute.CommunicationCenter.route)
                },
            )
        }

        composable(BusinessRoute.CategoryFlow.route) { entry ->
            val categoryId = entry.arguments?.getString(BusinessRoute.CategoryFlow.ARG_CATEGORY_ID)?.toLongOrNull()
                ?: return@composable
            val categoryName = categories.find { it.id == categoryId }?.name.orEmpty()
            CategoryFlowScreen(
                categoryName = categoryName,
                processes = processes,
                onProcessClick = { process ->
                    viewModel.openProcess(process)
                    navController.navigate(BusinessRoute.ProcessBills.create(process.id))
                },
                onNewProcessClick = {
                    viewModel.openCreateProcess(categoryId)
                    navController.navigate(BusinessRoute.ProcessCreate.create(categoryId))
                },
            )
        }

        composable(BusinessRoute.ProcessBills.route) { entry ->
            val processId = entry.arguments?.getString(BusinessRoute.ProcessBills.ARG_PROCESS_ID)?.toLongOrNull()
                ?: return@composable
            val processName = processes.find { it.id == processId }?.name.orEmpty()
            ProcessBillsScreen(
                processName = processName.ifBlank { stringResource(R.string.workflow_ux_bills_subtitle) },
                bills = bills,
                onBillClick = { bill ->
                    viewModel.openBill(bill.id)
                    navController.navigate(BusinessRoute.BillDetail.create(bill.id))
                },
                onCreateBillClick = {
                    viewModel.startCreateBill(processId)
                    navController.navigate(BusinessRoute.BillCreate.create(processId))
                },
            )
        }

        composable(BusinessRoute.ProcessCreate.route) { entry ->
            val categoryId = entry.arguments?.getString(BusinessRoute.ProcessCreate.ARG_CATEGORY_ID)?.toLongOrNull()
                ?: return@composable
            ProcessCreateScreen(
                onProcessCreated = { name, description ->
                    viewModel.createProcess(categoryId, name, description) { processId ->
                        viewModel.bindProcessScope(processId)
                        navController.navigate(BusinessRoute.ProcessBills.create(processId)) {
                            popUpTo(BusinessRoute.CategoryFlow.create(categoryId)) { inclusive = false }
                        }
                    }
                },
            )
        }

        composable(BusinessRoute.BillCreate.route) { entry ->
            val processId = entry.arguments?.getString(BusinessRoute.BillCreate.ARG_PROCESS_ID)?.toLongOrNull()
                ?: return@composable
            BillCreateScreen(
                draft = billDraft,
                onDraftChange = { draft -> viewModel.updateBillDraft { draft } },
                onBillCreated = {
                    viewModel.continueToParticipants()
                    navController.navigate(BusinessRoute.Participants.create(processId))
                },
            )
        }

        composable(BusinessRoute.Participants.route) { entry ->
            val processId = entry.arguments?.getString(BusinessRoute.Participants.ARG_PROCESS_ID)?.toLongOrNull()
                ?: return@composable
            ParticipantsScreen(
                draft = billDraft,
                onDraftChange = { draft -> viewModel.updateBillDraft { draft } },
                onParticipantsSelected = {
                    viewModel.continueToDistribution()
                    navController.navigate(BusinessRoute.Debts.create(processId))
                },
            )
        }

        composable(BusinessRoute.Debts.route) { entry ->
            val processId = entry.arguments?.getString(BusinessRoute.Debts.ARG_PROCESS_ID)?.toLongOrNull()
                ?: return@composable
            DebtsScreen(
                draft = billDraft,
                onDraftChange = { draft -> viewModel.updateBillDraft { draft } },
                onSplitEqually = viewModel::splitDraftEqually,
                onAiDistribute = viewModel::aiDistributeDraft,
                onReset = viewModel::resetDraftAmounts,
                onDebtsConfirmed = {
                    viewModel.createBillFromDraft { billId ->
                        viewModel.openBill(billId)
                        navController.navigate(BusinessRoute.BillDetail.create(billId)) {
                            popUpTo(BusinessRoute.BillCreate.create(processId)) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(BusinessRoute.BillDetail.route) { entry ->
            val billId = entry.arguments?.getString(BusinessRoute.BillDetail.ARG_BILL_ID)?.toLongOrNull()
                ?: return@composable
            BillDebtsScreen(
                bill = currentBill,
                debts = debts,
                stats = workflowDebtStatsFrom(allDebts),
                selectedDebtIds = selectedDebtIds,
                debtFilter = debtFilter,
                onFilterChange = viewModel::setDebtFilter,
                onToggleDebt = viewModel::toggleDebtSelection,
                onSelectAll = { viewModel.selectAllDebts(debts.map { it.debt.id }) },
                onMarkPaid = viewModel::markDebtPaid,
                onSendReminders = {
                    viewModel.openMessageComposer(billId)
                    navController.navigate(BusinessRoute.MessageComposer.create(billId))
                },
            )
        }

        composable(BusinessRoute.MessageComposer.route) { entry ->
            val billId = entry.arguments?.getString(BusinessRoute.MessageComposer.ARG_BILL_ID)?.toLongOrNull()
                ?: return@composable
            val effectiveSelectedCount = selectedDebtIds.size.takeIf { it > 0 }
                ?: BusinessWorkflowDebtSelection.unpaidDebtIds(allDebts).size
            MessageComposerScreen(
                messageBody = messageBody,
                selectedCount = effectiveSelectedCount,
                onMessageChange = viewModel::updateMessageBody,
                onAiImprove = { viewModel.aiImproveMessage(billId) },
                onAiShorten = viewModel::aiShortenMessage,
                onAiFormal = { viewModel.aiFormalMessage(billId) },
                onSendWhatsApp = { onSendWhatsApp(billId) },
                onSendSms = { onSendSms(billId) },
                onSendPush = { onSendPush(billId) },
                onSendAll = { onSendAll(billId) },
            )
        }

        composable(BusinessRoute.CommunicationCenter.route) {
            CommunicationCenterScreen(
                onOpenSmsCompose = { onSelectBusinessTab(BusinessTab.Sms, BusinessSmsPanel.COMPOSE) },
                onHistoryClick = { onSelectBusinessTab(BusinessTab.Sms, BusinessSmsPanel.HISTORY) },
                onTemplatesClick = { onSelectBusinessTab(BusinessTab.Sms, BusinessSmsPanel.TEMPLATES) },
                onAutomationClick = { navController.navigate(BusinessRoute.Automation.route) },
            )
        }

        composable(BusinessRoute.Automation.route) {
            AutomationScreen {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.business_automation_title))
                    TextButton(onClick = { onSelectBusinessTab(BusinessTab.Settings, null) }) {
                        Text(stringResource(R.string.business_automation_title))
                    }
                }
            }
        }

        composable(BusinessRoute.History.route) {
            HistoryScreen {
                TextButton(onClick = { onSelectBusinessTab(BusinessTab.Sms, BusinessSmsPanel.HISTORY) }) {
                    Text(stringResource(R.string.business_ds_history_title))
                }
            }
        }

        composable(BusinessRoute.Templates.route) {
            TemplatesScreen {
                TextButton(onClick = { onSelectBusinessTab(BusinessTab.Sms, BusinessSmsPanel.TEMPLATES) }) {
                    Text(stringResource(R.string.business_ds_templates_title))
                }
            }
        }
    }
}
