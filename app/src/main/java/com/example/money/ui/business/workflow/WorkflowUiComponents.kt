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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            .padding(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(BusinessSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.Hub,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun WorkflowCategoryCard(
    category: WorkflowCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = category.type.accentColor()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = BusinessSpacing.cardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BusinessSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category.type.icon(),
                    contentDescription = null,
                    tint = accent,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = category.type.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = BusinessSpacing.cardElevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BusinessSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = BusinessSpacing.md),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!trailing.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(BusinessSpacing.sm))
                Text(
                    trailing,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
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
        WorkflowDebtStatus.PAID -> stringResource(R.string.workflow_status_paid) to BusinessColors.ProfitGreen
        WorkflowDebtStatus.PARTIAL -> stringResource(R.string.workflow_status_partial) to BusinessColors.WarningYellow
        WorkflowDebtStatus.OPEN -> stringResource(R.string.workflow_status_open) to BusinessColors.LossRed
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun WorkflowDebtRow(
    item: WorkflowDebtWithParticipant,
    selected: Boolean,
    onToggle: () -> Unit,
    onMarkPaid: () -> Unit,
) {
    val isPaid = item.debt.status == WorkflowDebtStatus.PAID
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isPaid) { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { if (!isPaid) onToggle() },
                enabled = !isPaid,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.participant.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    Formatters.currencyFromCents(item.debt.remainingAmountCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (item.participant.phone.isBlank() && item.participant.email.isBlank()) {
                    Text(
                        stringResource(R.string.debt_remind_need_phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (item.participant.phone.isNotBlank()) {
                    Text(
                        item.participant.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WorkflowDebtStatusChip(status = item.debt.status)
            }
            if (!isPaid) {
                TextButton(onClick = onMarkPaid) {
                    Text(stringResource(R.string.workflow_mark_paid))
                }
            }
        }
    }
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(BusinessSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
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
            OutlinedTextField(
                value = messageBody,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.workflow_message_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            FilledTonalButton(
                onClick = onAiGenerate,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.workflow_ai_generate))
            }
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
            Button(
                onClick = onSendAll,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.workflow_send_all))
            }
        }
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
