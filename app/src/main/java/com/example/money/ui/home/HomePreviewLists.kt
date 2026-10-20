package com.example.money.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.debtors.DebtorSort
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.systemMotionEnabled

/**
 * Two nearest unpaid debts on the home screen.
 */
@Suppress("UNUSED_PARAMETER")
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
    val homeDebts = DebtorSort.sort(debtors.filter { !it.isPaid }).take(2)
    if (homeDebts.isEmpty()) {
        item {
            EmptyStateMessage(
                title = stringResource(R.string.empty_debtors_active),
                modifier = Modifier.padding(horizontalPadding),
            )
        }
        return
    }
    item {
        Text(
            text = stringResource(R.string.home_upcoming),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(horizontalPadding)
                .padding(vertical = MoneySpacing.sm)
                .clickable(onClick = onOpenDebts),
        )
    }
    items(homeDebts, key = { "home-debt-${it.id}" }) { debtor ->
        val motion = systemMotionEnabled(LocalContext.current)
        HomeListCompactRow(
            title = debtor.name,
            subtitle = Formatters.currencyFromCents(
                debtor.remainingAmountCents,
                hide = hideAmounts,
            ),
            selected = false,
            onClick = onOpenDebts,
            modifier = Modifier
                .animateItem(
                    fadeInSpec = MoneyMotion.spec(motion),
                    placementSpec = MoneyMotion.spec(motion),
                    fadeOutSpec = MoneyMotion.spec(motion),
                )
                .padding(horizontalPadding)
                .padding(vertical = MoneySpacing.xs),
        )
    }
}
