package com.example.money.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.ui.theme.MoneyMotion
import com.example.money.ui.theme.MoneyRadius
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.systemMotionEnabled
import kotlin.math.abs

@Composable
fun MintFinanceHeader(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    hideAmounts: Boolean,
    onOwesMeClick: () -> Unit,
    onIOweClick: () -> Unit,
    progressPercent: Int?,
    modifier: Modifier = Modifier,
) {
    DebtBalanceCard(
        goalMetrics = goalMetrics,
        debtMetrics = debtMetrics,
        hideAmounts = hideAmounts,
        progressPercent = progressPercent,
        onOwesMeClick = onOwesMeClick,
        onIOweClick = onIOweClick,
        onGoalsClick = null,
        modifier = modifier,
    )
}

/**
 * Phone and tablet home: one balance card, two debt tiles, and goal progress.
 */
@Composable
fun HomePreviewCards(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    hideAmounts: Boolean,
    progressPercent: Int?,
    onOwesMeClick: () -> Unit,
    onIOweClick: () -> Unit,
    onAccumulatedClick: () -> Unit,
    onRemainingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DebtBalanceCard(
        goalMetrics = goalMetrics,
        debtMetrics = debtMetrics,
        hideAmounts = hideAmounts,
        progressPercent = progressPercent,
        onOwesMeClick = onOwesMeClick,
        onIOweClick = onIOweClick,
        onGoalsClick = {
            onAccumulatedClick()
            onRemainingClick()
        },
        modifier = modifier,
    )
}

@Composable
private fun DebtBalanceCard(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    hideAmounts: Boolean,
    progressPercent: Int?,
    onOwesMeClick: () -> Unit,
    onIOweClick: () -> Unit,
    onGoalsClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val net = debtMetrics.owedToMeCents - debtMetrics.iOweCents
    val netText = signedNetBalance(
        formattedAbs = Formatters.currencyFromCents(abs(net), hide = hideAmounts),
        netCents = net,
        hidden = hideAmounts,
    )
    val percent = (progressPercent ?: 0).coerceIn(0, 100)
    val motionEnabled = systemMotionEnabled(LocalContext.current)
    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = MoneyMotion.spec(motionEnabled, MoneyMotion.long),
        label = "homeGoalProgress",
    )
    val accumulated = Formatters.currencyFromCents(goalMetrics.totalAccumulatedCents, hide = hideAmounts)
    val target = Formatters.currencyFromCents(
        goalMetrics.totalAccumulatedCents + goalMetrics.totalRemainingToGoalCents,
        hide = hideAmounts,
    )
    val balanceLabel = stringResource(R.string.home_debt_balance)
    val goalsLabel = stringResource(R.string.section_goals)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MoneyRadius.xl))
            .background(scheme.surface)
            .semantics { contentDescription = "$balanceLabel $netText" }
            .padding(MoneySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MoneySpacing.xs)) {
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            Text(
                text = netText,
                style = MaterialTheme.typography.displaySmall,
                color = scheme.onSurface,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm)) {
            BalanceTile(
                label = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(debtMetrics.owedToMeCents, hide = hideAmounts),
                container = scheme.primaryContainer,
                content = scheme.onPrimaryContainer,
                onClick = onOwesMeClick,
                modifier = Modifier.weight(1f),
            )
            BalanceTile(
                label = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(debtMetrics.iOweCents, hide = hideAmounts),
                container = scheme.errorContainer,
                content = scheme.onErrorContainer,
                onClick = onIOweClick,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onGoalsClick != null) Modifier.clickable(onClick = onGoalsClick) else Modifier),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = goalsLabel, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { animated },
                modifier = Modifier.fillMaxWidth(),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = accumulated,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = target,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BalanceTile(
    label: String,
    value: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(MoneyRadius.md))
            .background(container)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$label $value" }
            .padding(MoneySpacing.md),
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.xs),
    ) {
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
        Text(
            text = value,
            color = content,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleMedium.merge(TextStyle(fontFeatureSettings = "tnum")),
            maxLines = 1,
        )
    }
}

internal fun signedNetBalance(formattedAbs: String, netCents: Long, hidden: Boolean): String {
    if (hidden || netCents == 0L) return formattedAbs
    return if (netCents > 0L) "+$formattedAbs" else "-$formattedAbs"
}

@Composable
fun FinOrb(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier = modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(scheme.primaryContainer),
        )
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(scheme.surfaceContainerLowest, scheme.primaryContainer, scheme.primary),
                    ),
                ),
        )
    }
}
