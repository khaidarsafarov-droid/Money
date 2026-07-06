package com.example.money.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.GoalProgress
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.OverdueRed
import com.example.money.ui.util.Formatters

@Composable
fun GoalListItem(
    goal: SavingsGoal,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTransfer: (() -> Unit)? = null,
    onExpense: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val progressColor = when {
        goal.isOverAchieved -> GoldPrimary
        goal.isCompleted -> MaterialTheme.colorScheme.primary
        goal.deadlineMillis != null && GoalProgress.isDeadlinePassed(goal.deadlineMillis) -> OverdueRed
        else -> GoldPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (goal.isOverAchieved) {
                    Modifier.border(1.5.dp, GoldPrimary.copy(alpha = 0.5f), MaterialTheme.shapes.large)
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (!goal.isCompleted) 6.dp else 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.goal_purpose_card_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            goal.deadlineMillis?.let { deadline ->
                Text(
                    text = stringResource(
                        R.string.goal_deadline_date,
                        Formatters.shortDate(deadline),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LinearProgressIndicator(
                    progress = { goal.progressFraction },
                    modifier = Modifier.weight(1f),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    text = "${goal.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                )
            }

            if (goal.isOverAchieved) {
                Text(
                    text = stringResource(
                        R.string.goal_over_achieved,
                        Formatters.currencyFromCents(goal.surplusAmountCents),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldPrimary,
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.goal_remaining,
                        Formatters.currencyFromCents(goal.remainingAmountCents),
                        Formatters.currencyFromCents(goal.targetAmountCents),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            goal.dailyGoalCents?.let { dailyCents ->
                Text(
                    text = stringResource(
                        R.string.goal_daily_target,
                        Formatters.currencyFromCents(dailyCents),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            goal.deadlineMillis?.let { deadline ->
                if (GoalProgress.isDeadlinePassed(deadline) && !goal.isCompleted) {
                    Text(
                        text = stringResource(R.string.goal_deadline_passed),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = OverdueRed,
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.goal_saved_progress,
                    Formatters.currencyFromCents(goal.currentAmountCents),
                    Formatters.currencyFromCents(goal.targetAmountCents),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!goal.isCompleted && onTransfer != null) {
                    OutlinedButton(
                        onClick = onTransfer,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(stringResource(R.string.goal_action_transfer))
                    }
                }
                if (onExpense != null) {
                    OutlinedButton(
                        onClick = onExpense,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(stringResource(R.string.goal_action_expense))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Text(
                        text = stringResource(R.string.action_edit),
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                OutlinedButton(
                    onClick = onDelete,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
        }
    }
}
