package com.example.money.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.debtors.DebtHistorySheet
import com.example.money.ui.goal.GoalDetailOverlay
import com.example.money.ui.motion.LocalMoneyVisibility
import com.example.money.ui.theme.MoneyMotion

@Composable
internal fun HomeMotionOverlay(
    historyDebtorState: MutableState<Debtor?>,
    allDebtors: List<Debtor>,
    hideAmounts: Boolean,
    onAddAdvance: (Debtor, Long, Long, String) -> Unit,
    onAddPayment: (Debtor, Long, Long) -> Unit,
    stepsGoalId: Long?,
    onDismissGoal: () -> Unit,
    goals: List<SavingsGoal>,
    goalActions: List<GoalAction>,
    onAddAction: (Long, String) -> Unit,
    onToggleAction: (GoalAction, Boolean) -> Unit,
    onDeleteAction: (GoalAction) -> Unit,
) {
    val selected = historyDebtorState.value
    var lastDebt by remember { mutableStateOf(selected) }
    if (selected != null) lastDebt = selected
    AnimatedVisibility(
        visible = selected != null,
        enter = EnterTransition.None,
        exit = fadeOut(tween(MoneyMotion.fadeOut)),
    ) {
        CompositionLocalProvider(LocalMoneyVisibility provides this) {
            val debtor = lastDebt ?: return@CompositionLocalProvider
            val current = allDebtors.find { it.id == debtor.id } ?: debtor
            DebtHistorySheet(
                debtor = current,
                hideAmounts = hideAmounts,
                onDismiss = { historyDebtorState.value = null },
                onAddAdvance = { amountCents, givenAt, note ->
                    onAddAdvance(current, amountCents, givenAt, note)
                },
                onAddPayment = { amountCents, paidAt ->
                    onAddPayment(current, amountCents, paidAt)
                },
                inWindow = true,
            )
        }
    }

    val goal = goals.find { it.id == stepsGoalId }
    var lastGoal by remember { mutableStateOf(goal) }
    if (goal != null) lastGoal = goal
    AnimatedVisibility(
        visible = goal != null,
        enter = EnterTransition.None,
        exit = fadeOut(tween(MoneyMotion.fadeOut)),
    ) {
        CompositionLocalProvider(LocalMoneyVisibility provides this) {
            val shown = lastGoal ?: return@CompositionLocalProvider
            GoalDetailOverlay(
                goal = goals.find { it.id == shown.id } ?: shown,
                actions = goalActions.filter { it.goalId == shown.id },
                hideAmounts = hideAmounts,
                onDismiss = onDismissGoal,
                onAddAction = { title -> onAddAction(shown.id, title) },
                onToggleAction = onToggleAction,
                onDeleteAction = onDeleteAction,
            )
        }
    }
}
