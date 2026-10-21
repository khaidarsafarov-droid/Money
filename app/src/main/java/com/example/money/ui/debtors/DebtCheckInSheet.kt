package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.ui.util.Formatters

@Composable
fun DebtCheckInSheet(
    debtors: List<Debtor>,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val iOweActive = debtors.filter { it.debtType == DebtType.DEBTOR && !it.isPaid }
    val iOwePaid = debtors.filter { it.debtType == DebtType.DEBTOR && it.isPaid }
    val totalRemaining = iOweActive.sumOf { it.remainingAmountCents }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                top = 16.dp,
                bottom = 16.dp,
                end = if (onDismiss != null) 48.dp else 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.weekly_checkin_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.action_close),
                )
            }
        }
        }
    }
}
