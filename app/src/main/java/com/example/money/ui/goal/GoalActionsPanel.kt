package com.example.money.ui.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.example.money.ui.motion.MoneyInWindowSheet
import com.example.money.ui.motion.moneySharedBounds
import com.example.money.ui.motion.rememberDrawFraction
import com.example.money.ui.theme.finoraGoalBrush
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.GoalProgress
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.components.bringIntoViewOnFocus
import com.example.money.ui.components.MoneyDialog
import com.example.money.ui.util.Formatters

@Composable
fun GoalDetailOverlay(
    goal: SavingsGoal,
    actions: List<GoalAction>,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
    onAddAction: (String) -> Unit,
    onToggleAction: (GoalAction, Boolean) -> Unit,
    onDeleteAction: (GoalAction) -> Unit,
) {
    MoneyInWindowSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .moneySharedBounds("goal-detail-${goal.id}")
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(finoraGoalBrush(goal.name.hashCode())),
            )
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            GoalActionsPanel(
                goal = goal,
                actions = actions,
                hideAmounts = hideAmounts,
                onAddAction = onAddAction,
                onToggleAction = onToggleAction,
                onDeleteAction = onDeleteAction,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
    }
}

@Composable
fun GoalActionsDialog(
    goal: SavingsGoal,
    actions: List<GoalAction>,
    hideAmounts: Boolean,
    onDismiss: () -> Unit,
    onAddAction: (String) -> Unit,
    onToggleAction: (GoalAction, Boolean) -> Unit,
    onDeleteAction: (GoalAction) -> Unit,
) {
    MoneyDialog(
        title = goal.name,
        onDismiss = onDismiss,
        confirmLabel = stringResource(R.string.action_close),
        onConfirm = onDismiss,
    ) {
        GoalActionsPanel(
            goal = goal,
            actions = actions,
            hideAmounts = hideAmounts,
            onAddAction = onAddAction,
            onToggleAction = onToggleAction,
            onDeleteAction = onDeleteAction,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
fun GoalActionsPanel(
    goal: SavingsGoal,
    actions: List<GoalAction>,
    hideAmounts: Boolean,
    onAddAction: (String) -> Unit,
    onToggleAction: (GoalAction, Boolean) -> Unit,
    onDeleteAction: (GoalAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(goal.id) { mutableStateOf("") }
    val done = actions.count { it.completed }
    val total = actions.size
    val fraction = if (total <= 0) 0f else done.toFloat() / total.toFloat()
    val moneyProgress = rememberDrawFraction(goal.progressFraction)
    val actionProgress = rememberDrawFraction(fraction)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (goal.kind == GoalKind.MONEY && goal.targetAmountCents > 0L) {
            Text(
                text = stringResource(
                    R.string.goal_saved_progress,
                    Formatters.currencyFromCents(goal.currentAmountCents, hide = hideAmounts),
                    Formatters.currencyFromCents(goal.targetAmountCents, hide = hideAmounts),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = moneyProgress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.goal_actions_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (total == 0) {
            Text(
                text = stringResource(R.string.goal_action_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.goal_action_progress, done, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            LinearProgressIndicator(
                progress = actionProgress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        actions.forEach { action ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = action.completed,
                    onCheckedChange = { onToggleAction(action, it) },
                )
                Text(
                    text = action.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                IconButton(onClick = { onDeleteAction(action) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(stringResource(R.string.goal_action_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewOnFocus(),
        )
        MoneyButton(
            text = stringResource(R.string.goal_action_add),
            onClick = {
                val title = draft.trim()
                if (title.isEmpty()) return@MoneyButton
                onAddAction(title)
                draft = ""
            },
            priority = MoneyButtonPriority.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (goal.kind == GoalKind.MONEY) {
            val daily = goal.dailyGoalCents
            val daysLeft = goal.deadlineMillis?.let { GoalProgress.daysUntilDeadline(it) }
            if (!goal.isCompleted && daily != null && daysLeft != null) {
                Text(
                    text = stringResource(
                        R.string.goal_pace_line,
                        Formatters.currencyFromCents(daily, hide = hideAmounts),
                        daysLeft,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}
