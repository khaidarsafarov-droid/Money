package com.example.money.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.util.Formatters

/**
 * The first goals and debts on the home screen. Phone and tablet share this block.
 * The surrounding chrome stays different: a drawer on the phone, a rail and detail pane on the tablet.
 */
fun LazyListScope.homeGoalAndDebtPreview(
    goals: List<SavingsGoal>,
    debtors: List<Debtor>,
    hideAmounts: Boolean,
    horizontalPadding: PaddingValues,
    onOpenGoals: () -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onExpense: (SavingsGoal) -> Unit,
    onLogGoal: (SavingsGoal) -> Unit,
    onOpenDebts: () -> Unit,
) {
    val homeGoals = goals.filter { !it.isCompleted }.take(4)
    val homeDebts = debtors.filter { !it.isPaid }.take(4)
    if (homeGoals.isEmpty() && homeDebts.isEmpty()) {
        item {
            EmptyStateMessage(
                title = stringResource(R.string.home_only_goals_debts),
                modifier = Modifier.padding(horizontalPadding),
            )
        }
    }
    if (homeGoals.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.section_goals),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontalPadding)
                    .padding(vertical = 8.dp)
                    .clickable(onClick = onOpenGoals),
            )
        }
        items(homeGoals, key = { "home-goal-${it.id}" }) { goal ->
            GoalListItem(
                goal = goal,
                onEdit = { onEditGoal(goal) },
                onOpen = { onOpenGoal(goal) },
                onDelete = { onDeleteGoal(goal) },
                onTransfer = if (goal.kind == GoalKind.MONEY) {
                    { onGoalTransfer(goal) }
                } else {
                    null
                },
                onExpense = { onExpense(goal) },
                onLog = { onLogGoal(goal) },
                hideAmounts = hideAmounts,
                modifier = Modifier
                    .padding(horizontalPadding)
                    .padding(vertical = 4.dp),
            )
        }
    }
    if (homeDebts.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.tab_debts),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontalPadding)
                    .padding(vertical = 8.dp)
                    .clickable(onClick = onOpenDebts),
            )
        }
        items(homeDebts, key = { "home-debt-${it.id}" }) { debtor ->
            HomeListCompactRow(
                title = debtor.name,
                subtitle = Formatters.currencyFromCents(
                    debtor.remainingAmountCents,
                    hide = hideAmounts,
                ),
                selected = false,
                onClick = onOpenDebts,
                modifier = Modifier
                    .padding(horizontalPadding)
                    .padding(vertical = 4.dp),
            )
        }
    }
}
