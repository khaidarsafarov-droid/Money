package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.util.Formatters

@Composable
fun DebtOverviewCard(
    debtors: List<Debtor>,
    debtType: DebtType,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val activeDebts = debtors.filter { it.debtType == debtType && !it.isPaid }
    val totalRemaining = activeDebts.sumOf { it.remainingAmountCents }
    if (totalRemaining <= 0L) return

    val titleRes = when (debtType) {
        DebtType.CREDITOR -> R.string.debt_overview_owes_me_title
        DebtType.DEBTOR -> R.string.debt_overview_title
    }
    val amountColor = when (debtType) {
        DebtType.CREDITOR -> FinancePositive
        DebtType.DEBTOR -> GoldPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MoneySpacing.xs, vertical = MoneySpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "·",
            style = moneyCaptionStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = Formatters.currencyFromCents(totalRemaining, hide = hideAmounts),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = amountColor,
        )
        Text(
            text = "·",
            style = moneyCaptionStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.debt_overview_count_short, activeDebts.size),
            style = moneyCaptionStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
