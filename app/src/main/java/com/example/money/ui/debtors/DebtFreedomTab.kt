package com.example.money.ui.debtors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.ui.theme.CardBorder
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters

@Composable
fun DebtFreedomTab(
    projection: DebtFreedomEngine.FreedomProjection,
    extraMonthlyCents: Long,
    onExtraMonthlyChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hideAmounts: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        border = BorderStroke(1.dp, CardBorder),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.debt_freedom_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (!projection.hasActiveDebts) {
                Text(
                    text = stringResource(R.string.debt_freedom_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Text(
                text = stringResource(
                    R.string.debt_freedom_date,
                    projection.freedomDateMillis?.let { Formatters.shortDate(it) }
                        ?: stringResource(R.string.debt_freedom_unknown),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
            )

            Text(
                text = stringResource(
                    R.string.debt_freedom_remaining_total,
                    Formatters.currencyFromCents(projection.totalRemainingCents, hide = hideAmounts),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = stringResource(
                    R.string.debt_freedom_monthly_pace,
                    Formatters.currencyFromCents(projection.monthlyPaymentCents, hide = hideAmounts),
                    projection.monthsRemaining ?: 0,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.debt_freedom_extra_slider_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )

            val sliderSteps = 20
            val maxExtraCents = 50_000L
            Slider(
                value = extraMonthlyCents.toFloat(),
                onValueChange = { value ->
                    val stepped = (value / (maxExtraCents / sliderSteps)).toLong() * (maxExtraCents / sliderSteps)
                    onExtraMonthlyChange(stepped.coerceIn(0L, maxExtraCents))
                },
                valueRange = 0f..maxExtraCents.toFloat(),
                steps = sliderSteps - 1,
            )

            Text(
                text = stringResource(
                    R.string.debt_freedom_extra_amount,
                    Formatters.currencyFromCents(extraMonthlyCents, hide = hideAmounts),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            projection.monthsSavedWithExtra?.takeIf { it > 0 }?.let { monthsSaved ->
                Text(
                    text = stringResource(R.string.debt_freedom_months_saved, monthsSaved),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = FinancePositive,
                )
            }

            Text(
                text = stringResource(R.string.debt_freedom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
