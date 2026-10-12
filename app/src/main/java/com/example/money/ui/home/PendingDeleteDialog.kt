package com.example.money.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.components.ConfirmDeleteDialog

internal sealed class PendingDelete {
    data class DebtorItem(val debtor: Debtor) : PendingDelete()
    data class GoalItem(val goal: SavingsGoal) : PendingDelete()
}

@Composable
internal fun PendingDeleteDialog(
    pending: PendingDelete,
    onDismiss: () -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
) {
    when (pending) {
        is PendingDelete.DebtorItem -> ConfirmDeleteDialog(
            title = stringResource(R.string.delete_debtor_title),
            message = stringResource(R.string.delete_debtor_message, pending.debtor.name),
            onDismiss = onDismiss,
            onConfirm = {
                onDeleteDebtor(pending.debtor)
                onDismiss()
            },
        )
        is PendingDelete.GoalItem -> ConfirmDeleteDialog(
            title = stringResource(R.string.delete_goal_title),
            message = stringResource(R.string.delete_goal_message, pending.goal.name),
            onDismiss = onDismiss,
            onConfirm = {
                onDeleteGoal(pending.goal)
                onDismiss()
            },
        )
    }
}
