package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.util.Formatters

@Composable
fun DebtCheckInSheet(
    debtors: List<Debtor>,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val iOweActive = debtors.filter { it.debtType == DebtType.DEBTOR && !it.isPaid }
    val iOwePaid = debtors.filter { it.debtType == DebtType.DEBTOR && it.isPaid }
    val totalRemaining = iOweActive.sumOf { it.remainingAmountCents }
    val totalOriginal = (iOweActive + iOwePaid).sumOf { it.targetAmountCents }
    val totalPaid = (iOweActive + iOwePaid).sumOf { it.currentAmountCents }
    val progress = if (totalOriginal > 0L) {
        (totalPaid.toFloat() / totalOriginal).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.weekly_checkin_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.weekly_checkin_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = FinancePositive,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            )
            Text(
                text = stringResource(
                    R.string.weekly_checkin_progress,
                    (progress * 100).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    R.string.weekly_checkin_remaining,
                    Formatters.currencyFromCents(totalRemaining, hide = hideAmounts),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            if (iOwePaid.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.weekly_checkin_closed_count, iOwePaid.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = FinancePositive,
                )
            }
        }
    }
}
