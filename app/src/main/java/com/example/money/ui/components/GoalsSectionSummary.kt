package com.example.money.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.GoalProgress
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.OverdueRed
import com.example.money.ui.util.Formatters

@Composable
fun GoalsSectionSummary(
    goals: List<SavingsGoal>,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val summary = GoalProgress.summarize(goals)
    if (summary.activeCount == 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoneySpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.goals_summary_saved,
                        Formatters.currencyFromCents(summary.savedCents, hide = hideAmounts),
                        Formatters.currencyFromCents(summary.targetCents, hide = hideAmounts),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${summary.progressPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { GoalProgress.displayProgressFraction(summary.targetCents, summary.savedCents) },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            summary.dailyCents?.let { daily ->
                Text(
                    text = stringResource(
                        R.string.goals_summary_daily,
                        Formatters.currencyFromCents(daily, hide = hideAmounts),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.overdueCount > 0) {
                Text(
                    text = stringResource(R.string.goals_summary_overdue, summary.overdueCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = OverdueRed,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
