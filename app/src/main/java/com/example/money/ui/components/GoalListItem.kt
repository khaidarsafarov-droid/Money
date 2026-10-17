package com.example.money.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.GoalProgress
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.OverdueRed
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.theme.moneyDisplayStyle
import com.example.money.ui.util.Formatters

@Composable
fun GoalListItem(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit = {},
    onTransfer: (() -> Unit)? = null,
    onExpense: (() -> Unit)? = null,
    onLog: (() -> Unit)? = null,
    hideAmounts: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isMoney = goal.kind == GoalKind.MONEY
    val transferAction = if (isMoney) onTransfer else null
    val expenseAction = if (isMoney) onExpense else null
    val barFraction = if (isMoney) goal.progressFraction else goal.actionProgressFraction
    val barPercent = if (isMoney) goal.progressPercent else goal.actionProgressPercent
    val isDeadlinePassed = goal.deadlineMillis != null &&
        GoalProgress.isDeadlinePassed(goal.deadlineMillis) &&
        !goal.isCompleted
    val progressColor = when {
        goal.isOverAchieved -> MaterialTheme.colorScheme.primary
        goal.isCompleted -> MaterialTheme.colorScheme.primary
        isDeadlinePassed -> OverdueRed
        else -> MaterialTheme.colorScheme.primary
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onOpen,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (goal.isOverAchieved) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!goal.isCompleted) 2.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoneySpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        GoalStatusBadge(
                            label = when {
                                goal.isCompleted -> stringResource(R.string.goal_status_completed)
                                isDeadlinePassed -> stringResource(R.string.goal_deadline_passed_short)
                                else -> stringResource(R.string.goal_status_active)
                            },
                            isCompleted = goal.isCompleted,
                            isOverdue = isDeadlinePassed,
                        )
                    }

                    if (isMoney) {
                        Text(
                            text = Formatters.currencyFromCents(goal.currentAmountCents, hide = hideAmounts),
                            style = moneyDisplayStyle(),
                        )
                        Text(
                            text = if (goal.isOverAchieved) {
                                stringResource(
                                    R.string.goal_over_achieved,
                                    Formatters.currencyFromCents(goal.surplusAmountCents, hide = hideAmounts),
                                )
                            } else {
                                stringResource(
                                    R.string.goal_remaining,
                                    Formatters.currencyFromCents(goal.remainingAmountCents, hide = hideAmounts),
                                    Formatters.currencyFromCents(goal.targetAmountCents, hide = hideAmounts),
                                )
                            },
                            style = moneyCaptionStyle(),
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.goal_kind_action),
                            style = moneyCaptionStyle(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(
                                R.string.goal_log_collected,
                                Formatters.currencyFromCents(goal.currentAmountCents, hide = hideAmounts),
                            ),
                            style = moneyDisplayStyle(),
                        )
                    }
                    if (goal.actionTotal > 0) {
                        Text(
                            text = stringResource(
                                R.string.goal_action_progress,
                                goal.actionDone,
                                goal.actionTotal,
                            ),
                            style = moneyCaptionStyle(),
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    goal.deadlineMillis?.let { deadline ->
                        Text(
                            text = stringResource(
                                R.string.goal_deadline_date,
                                Formatters.shortDate(deadline),
                            ),
                            style = moneyCaptionStyle(),
                        )
                    }
                    val dailyCents = goal.dailyGoalCents
                    val daysLeft = goal.deadlineMillis?.let { GoalProgress.daysUntilDeadline(it) }
                    if (isMoney && !goal.isCompleted && dailyCents != null && daysLeft != null) {
                        Text(
                            text = stringResource(
                                R.string.goal_pace_line,
                                Formatters.currencyFromCents(dailyCents, hide = hideAmounts),
                                daysLeft,
                            ),
                            style = moneyCaptionStyle(),
                            color = progressColor,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(MoneySpacing.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more),
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                            },
                        )
                        if (!goal.isCompleted && transferAction != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.goal_action_transfer)) },
                                onClick = {
                                    showMenu = false
                                    transferAction()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null)
                                },
                            )
                        }
                        if (expenseAction != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.goal_action_expense)) },
                                onClick = {
                                    showMenu = false
                                    expenseAction()
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Payments, contentDescription = null)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null)
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MoneySpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { barFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "$barPercent%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                )
            }

            if (onLog != null) {
                MoneyButton(
                    text = stringResource(R.string.goal_log_button),
                    onClick = onLog,
                    priority = MoneyButtonPriority.Secondary,
                    icon = Icons.Outlined.EditNote,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
