package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
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
    val subtitleRes = when (debtType) {
        DebtType.CREDITOR -> R.string.debt_overview_owes_me_subtitle
        DebtType.DEBTOR -> R.string.debt_overview_subtitle
    }
    val amountColor = when (debtType) {
        DebtType.CREDITOR -> FinancePositive
        DebtType.DEBTOR -> GoldPrimary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = Formatters.currencyFromCents(totalRemaining, hide = hideAmounts),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.debt_overview_count, activeDebts.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
