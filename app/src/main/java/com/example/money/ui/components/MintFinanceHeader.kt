package com.example.money.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.R
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters

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
    val score = homeScore(goalMetrics, debtMetrics, progressPercent, hideAmounts)
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(debtMetrics.owedToMeCents, hide = hideAmounts),
                container = scheme.primaryContainer,
                onClick = onOwesMeClick.takeIf { debtMetrics.owedToMeCents > 0L },
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(debtMetrics.iOweCents, hide = hideAmounts),
                container = scheme.errorContainer,
                onClick = onIOweClick.takeIf { debtMetrics.iOweCents > 0L },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.global_balance_accumulated_label),
                value = Formatters.currencyFromCents(goalMetrics.totalAccumulatedCents, hide = hideAmounts),
                container = scheme.tertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.global_balance_remaining_label),
                value = Formatters.currencyFromCents(goalMetrics.totalRemainingToGoalCents, hide = hideAmounts),
                container = scheme.secondaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        ScoreCard(score = score.percent, caption = score.caption)
    }
}

@Composable
private fun PastelStat(
    label: String,
    value: String,
    container: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "$label $value" }
            .padding(MoneySpacing.md),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val scheme = MaterialTheme.colorScheme
        Text(text = label, color = scheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
        Text(
            text = value,
            color = scheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            maxLines = 1,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

@Composable
private fun ScoreCard(score: Int, caption: String) {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val heroStart = if (dark) scheme.primaryContainer else scheme.primary
    val heroEnd = if (dark) scheme.onPrimary else scheme.onPrimaryContainer
    val onHero = if (dark) scheme.onPrimaryContainer else scheme.onPrimary
    val progress = if (dark) scheme.onPrimaryContainer else scheme.primaryContainer
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(heroStart, heroEnd)))
            .semantics { contentDescription = caption }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_score_title),
                color = onHero,
                fontSize = 13.sp,
            )
            Text(
                text = caption,
                color = onHero,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(88.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = score.coerceIn(0, 100) / 100f,
                        range = 0f..1f,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val style = Stroke(width = stroke, cap = StrokeCap.Round)
                drawArc(
                    color = onHero.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
                drawArc(
                    color = progress,
                    startAngle = -90f,
                    sweepAngle = 360f * score / 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
            }
            Text(
                text = score.toString(),
                color = onHero,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/**
 * Phone and tablet list preview: the two debt sides stay separate, and the
 * ring uses the same percent as its caption.
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
    val score = homeScore(goalMetrics, debtMetrics, progressPercent, hideAmounts)
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(debtMetrics.owedToMeCents, hide = hideAmounts),
                container = scheme.tertiaryContainer,
                onClick = onOwesMeClick,
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(debtMetrics.iOweCents, hide = hideAmounts),
                container = scheme.errorContainer,
                onClick = onIOweClick,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.home_preview_accumulated),
                value = Formatters.currencyFromCents(goalMetrics.totalAccumulatedCents, hide = hideAmounts),
                container = scheme.primaryContainer,
                onClick = onAccumulatedClick,
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.global_balance_remaining_label),
                value = Formatters.currencyFromCents(goalMetrics.totalRemainingToGoalCents, hide = hideAmounts),
                container = scheme.secondaryContainer,
                onClick = onRemainingClick,
                modifier = Modifier.weight(1f),
            )
        }
        ScoreCard(score = score.percent, caption = score.caption)
    }
}

private data class HomeScore(val percent: Int, val caption: String)

@Composable
private fun homeScore(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    progressPercent: Int?,
    hideAmounts: Boolean,
): HomeScore {
    val goalPool = goalMetrics.totalAccumulatedCents + goalMetrics.totalRemainingToGoalCents
    val goalPercent = if (goalPool > 0L) {
        ((goalMetrics.totalAccumulatedCents * 100L) / goalPool).toInt().coerceIn(0, 100)
    } else {
        0
    }
    val usesDebts = debtMetrics.iOweActiveCount > 0
    val percent = GoalPsychologyEngines.displayedHomeProgressPercent(
        iOweActiveCount = debtMetrics.iOweActiveCount,
        debtPercent = progressPercent ?: 0,
        goalPercent = goalPercent,
    )
    val remaining = if (usesDebts) {
        debtMetrics.iOweCents
    } else {
        goalMetrics.totalRemainingToGoalCents
    }
    val caption = stringResource(
        if (usesDebts) R.string.home_score_debts else R.string.home_score_goals,
        percent,
        Formatters.currencyFromCents(remaining, hide = hideAmounts),
    )
    return HomeScore(percent, caption)
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
