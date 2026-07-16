package com.example.money.ui.business.workflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowCategoryType
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.ui.business.designsystem.BusinessCategoryCard
import com.example.money.ui.business.designsystem.BusinessChannelCard
import com.example.money.ui.business.designsystem.BusinessEmptyState
import com.example.money.ui.business.designsystem.BusinessListRow
import com.example.money.ui.business.designsystem.BusinessScreenHeader
import com.example.money.ui.business.designsystem.BusinessStatusChip
import com.example.money.ui.business.designsystem.BusinessDesignTokens
import com.example.money.ui.business.designsystem.BusinessMessageComposerPanel
import com.example.money.ui.business.designsystem.ButtonAi
import com.example.money.ui.business.designsystem.ButtonPrimary
import com.example.money.ui.business.designsystem.BusinessOutlineActionButton
import com.example.money.ui.business.theme.BusinessColors
import com.example.money.ui.business.theme.BusinessSpacing
import com.example.money.ui.util.Formatters

data class WorkflowBreadcrumb(
    val steps: List<String>,
)

data class WorkflowDebtStats(
    val totalCents: Long,
    val paidCents: Long,
    val openCount: Int,
    val paidCount: Int,
)

fun workflowDebtStatsFrom(debts: List<WorkflowDebtWithParticipant>): WorkflowDebtStats {
    val total = debts.sumOf { it.debt.amountCents }
    val paid = debts.sumOf { it.debt.paidAmountCents }
    return WorkflowDebtStats(
        totalCents = total,
        paidCents = paid,
        openCount = debts.count { it.debt.status != WorkflowDebtStatus.PAID },
        paidCount = debts.count { it.debt.status == WorkflowDebtStatus.PAID },
    )
}

@Composable
fun WorkflowHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    BusinessScreenHeader(title = title, subtitle = subtitle, modifier = modifier)
}

@Composable
fun WorkflowBreadcrumbBar(
    breadcrumb: WorkflowBreadcrumb,
    modifier: Modifier = Modifier,
) {
    if (breadcrumb.steps.isEmpty()) return
    Text(
        text = breadcrumb.steps.joinToString("  ›  "),
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun WorkflowEmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BusinessEmptyState(message = message, actionLabel = actionLabel, onAction = onAction, modifier = modifier)
}

@Composable
fun WorkflowCategoryCard(
    category: WorkflowCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    BusinessCategoryCard(
        title = category.name,
        description = category.type.subtitle(),
        icon = category.type.icon(),
        accentColor = category.type.accentColor(),
        selected = selected,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun WorkflowListCard(
    title: String,
    subtitle: String? = null,
    trailing: String? = null,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BusinessListRow(
        title = title,
        subtitle = subtitle,
        trailing = trailing,
        leadingIcon = leadingIcon,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun WorkflowBillSummaryCard(
    bill: WorkflowBill?,
    stats: WorkflowDebtStats,
    modifier: Modifier = Modifier,
) {
    val progress = if (stats.totalCents > 0) {
        stats.paidCents.toFloat() / stats.totalCents.toFloat()
    } else {
        0f
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(BusinessSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
        ) {
            bill?.let {
                Text(it.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (it.periodLabel.isNotBlank()) {
                    Text(it.periodLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        stringResource(R.string.workflow_ux_total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Formatters.currencyFromCents(stats.totalCents),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.workflow_ux_collected),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        Formatters.currencyFromCents(stats.paidCents),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BusinessColors.ProfitGreen,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = BusinessColors.ProfitGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                stringResource(R.string.workflow_ux_debt_progress, stats.paidCount, stats.paidCount + stats.openCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun WorkflowDebtStatusChip(status: WorkflowDebtStatus) {
    val (label, color) = when (status) {
        WorkflowDebtStatus.PAID -> stringResource(R.string.workflow_status_paid) to BusinessDesignTokens.SuccessGreen
        WorkflowDebtStatus.PARTIAL -> stringResource(R.string.workflow_status_partial) to BusinessDesignTokens.WarningAmber
        WorkflowDebtStatus.OPEN -> stringResource(R.string.workflow_status_open) to BusinessDesignTokens.ErrorRed
    }
    BusinessStatusChip(label = label, color = color)
}

@Composable
fun WorkflowDebtRow(
    item: WorkflowDebtWithParticipant,
    selected: Boolean,
    onToggle: () -> Unit,
    onMarkPaid: () -> Unit,
) {
    val isPaid = item.debt.status == WorkflowDebtStatus.PAID
    val (statusLabel, statusColor) = when (item.debt.status) {
        WorkflowDebtStatus.PAID -> stringResource(R.string.workflow_status_paid) to BusinessDesignTokens.SuccessGreen
        WorkflowDebtStatus.PARTIAL -> stringResource(R.string.workflow_status_partial) to BusinessDesignTokens.WarningAmber
        WorkflowDebtStatus.OPEN -> stringResource(R.string.workflow_status_open) to BusinessDesignTokens.ErrorRed
    }
    val subtitle = when {
        item.participant.phone.isBlank() && item.participant.email.isBlank() ->
            stringResource(R.string.debt_remind_need_phone)
        item.participant.phone.isNotBlank() -> item.participant.phone
        else -> item.participant.email
    }
    com.example.money.ui.business.designsystem.DebtRow(
        name = item.participant.name,
        amount = Formatters.currencyFromCents(item.debt.remainingAmountCents),
        statusLabel = statusLabel,
        statusColor = statusColor,
        selected = selected,
        enabled = !isPaid,
        onToggle = onToggle,
        markPaidLabel = if (isPaid) null else stringResource(R.string.workflow_mark_paid),
        onMarkPaid = if (isPaid) null else onMarkPaid,
        subtitle = subtitle,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkflowCommunicationHub(
    messageBody: String,
    selectedCount: Int,
    onMessageChange: (String) -> Unit,
    onAiGenerate: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendSms: () -> Unit,
    onSendPush: () -> Unit,
    onSendAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = selectedCount > 0
    BusinessMessageComposerPanel(
        message = messageBody,
        onMessageChange = onMessageChange,
        messageLabel = stringResource(R.string.workflow_message_label),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.workflow_ux_communication_center),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.workflow_ux_selected_count, selectedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ButtonAi(
            text = stringResource(R.string.workflow_ai_generate),
            onClick = onAiGenerate,
            enabled = enabled,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkflowChannelButton(
                label = stringResource(R.string.workflow_send_whatsapp),
                icon = Icons.Outlined.Store,
                enabled = enabled,
                onClick = onSendWhatsApp,
            )
            WorkflowChannelButton(
                label = stringResource(R.string.workflow_send_sms),
                icon = Icons.Outlined.Sms,
                enabled = enabled,
                onClick = onSendSms,
            )
            WorkflowChannelButton(
                label = stringResource(R.string.workflow_send_push),
                icon = Icons.Outlined.Notifications,
                enabled = enabled,
                onClick = onSendPush,
            )
        }
        ButtonPrimary(
            text = stringResource(R.string.workflow_send_all),
            onClick = onSendAll,
            enabled = enabled,
        )
    }
}

@Composable
private fun WorkflowChannelButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun WorkflowSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(BusinessSpacing.md),
                content = content,
            )
        }
    }
}

@Composable
private fun WorkflowCategoryType.icon(): ImageVector = when (this) {
    WorkflowCategoryType.BUSINESS -> Icons.Outlined.People
    WorkflowCategoryType.TRADE -> Icons.Outlined.ShoppingCart
    WorkflowCategoryType.WORK_PROCESS -> Icons.Outlined.AccountTree
    WorkflowCategoryType.FAMILY -> Icons.Outlined.Home
    WorkflowCategoryType.SHARED -> Icons.Outlined.Groups
    WorkflowCategoryType.PROJECTS -> Icons.Outlined.Folder
    WorkflowCategoryType.SERVICES -> Icons.Outlined.Receipt
    WorkflowCategoryType.SUBSCRIPTIONS -> Icons.Outlined.Subscriptions
}

@Composable
private fun WorkflowCategoryType.subtitle(): String = when (this) {
    WorkflowCategoryType.BUSINESS -> stringResource(R.string.workflow_ux_cat_business)
    WorkflowCategoryType.TRADE -> stringResource(R.string.workflow_ux_cat_trade)
    WorkflowCategoryType.WORK_PROCESS -> stringResource(R.string.workflow_ux_cat_work)
    WorkflowCategoryType.FAMILY -> stringResource(R.string.workflow_ux_cat_family)
    WorkflowCategoryType.SHARED -> stringResource(R.string.workflow_ux_cat_shared)
    WorkflowCategoryType.PROJECTS -> stringResource(R.string.workflow_ux_cat_projects)
    WorkflowCategoryType.SERVICES -> stringResource(R.string.workflow_ux_cat_services)
    WorkflowCategoryType.SUBSCRIPTIONS -> stringResource(R.string.workflow_ux_cat_subscriptions)
}

private fun WorkflowCategoryType.accentColor(): Color = when (this) {
    WorkflowCategoryType.BUSINESS -> BusinessColors.PrimaryBlue
    WorkflowCategoryType.TRADE -> Color(0xFF6A1B9A)
    WorkflowCategoryType.WORK_PROCESS -> Color(0xFF00838F)
    WorkflowCategoryType.FAMILY -> Color(0xFFE65100)
    WorkflowCategoryType.SHARED -> Color(0xFF2E7D32)
    WorkflowCategoryType.PROJECTS -> Color(0xFF4527A0)
    WorkflowCategoryType.SERVICES -> Color(0xFF1565C0)
    WorkflowCategoryType.SUBSCRIPTIONS -> Color(0xFFC62828)
}
