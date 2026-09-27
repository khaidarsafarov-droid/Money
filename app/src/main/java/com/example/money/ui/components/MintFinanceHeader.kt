package com.example.money.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.money.R
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters
import kotlin.math.roundToInt

private val HeroStart = Color(0xFF15967C)
private val HeroEnd = Color(0xFF0B5346)
private val PastelMint = Color(0xFFD9F6E6)
private val PastelPeach = Color(0xFFFFE4CC)
private val PastelLilac = Color(0xFFECE3FB)
private val PastelSand = Color(0xFFFFF4CF)
private val Ink = Color(0xFF12352C)
private val InkMuted = Color(0xFF5C7D72)

@Composable
fun MintFinanceHeader(
    summary: BudgetSummary,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    hideAmounts: Boolean,
    onOwesMeClick: () -> Unit,
    onIOweClick: () -> Unit,
    progressPercent: Int?,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val goalPool = goalMetrics.totalAccumulated + goalMetrics.totalRemainingToGoal
    val goalPercent = if (goalPool > 0.0) {
        ((goalMetrics.totalAccumulated / goalPool) * 100.0).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    val debtActive = debtMetrics.owedToMeActiveCount + debtMetrics.iOweActiveCount
    val debtScore = if (debtActive > 0) progressPercent else null
    val usesDebtScore = debtScore != null
    val score = debtScore?.coerceIn(0, 100) ?: goalPercent

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BalanceHero(summary = summary, hideAmounts = hideAmounts)
        if (compact) return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(debtMetrics.owedToMeCents, hide = hideAmounts),
                container = PastelMint,
                onClick = onOwesMeClick.takeIf { debtMetrics.owedToMeCents > 0L },
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(debtMetrics.iOweCents, hide = hideAmounts),
                container = PastelPeach,
                onClick = onIOweClick.takeIf { debtMetrics.iOweCents > 0L },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.global_balance_accumulated_label),
                value = Formatters.currencyCompact(goalMetrics.totalAccumulated, hide = hideAmounts),
                container = PastelLilac,
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.global_balance_remaining_label),
                value = Formatters.currencyCompact(goalMetrics.totalRemainingToGoal, hide = hideAmounts),
                container = PastelSand,
                modifier = Modifier.weight(1f),
            )
        }
        ScoreCard(
            score = score,
            caption = stringResource(
                if (usesDebtScore) R.string.home_score_debts else R.string.home_score_goals,
            ),
        )
    }
}

@Composable
private fun BalanceHero(
    summary: BudgetSummary,
    hideAmounts: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(HeroStart, HeroEnd)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.global_balance_label),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 14.sp,
        )
        Text(
            text = Formatters.currency(summary.availableBalance, hide = hideAmounts),
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.global_balance_income_minus_expenses) +
                ": " + Formatters.currency(summary.globalBalance, hide = hideAmounts),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 13.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            HeroFigure(
                label = stringResource(R.string.global_balance_deposits_label),
                value = Formatters.currency(summary.totalDeposits, hide = hideAmounts),
            )
            HeroFigure(
                label = stringResource(R.string.global_balance_expenses_label),
                value = Formatters.currency(summary.totalExpenses, hide = hideAmounts),
            )
        }
    }
}

@Composable
private fun HeroFigure(label: String, value: String) {
    Column {
        Text(text = label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
        Text(text = label, color = InkMuted, fontSize = 12.sp, maxLines = 1)
        Text(text = value, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
private fun ScoreCard(score: Int, caption: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF127863), HeroEnd)))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_score_title),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 13.sp,
            )
            Text(
                text = caption,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
        Box(modifier = Modifier.size(88.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                val style = Stroke(width = stroke, cap = StrokeCap.Round)
                drawArc(
                    color = Color.White.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = style,
                )
                drawArc(
                    color = Color(0xFFB7F5D4),
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
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            )
        }
    }
}

/**
 * Four tappable previews on the home screen. Net debt replaces the separate
 * "owes me" and "I owe" tiles; the breakdown stays inside the Debts section.
 */
@Composable
fun HomePreviewCards(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    hideAmounts: Boolean,
    progressPercent: Int?,
    onDebtsClick: () -> Unit,
    onAccumulatedClick: () -> Unit,
    onRemainingClick: () -> Unit,
    onProgressClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goalPool = goalMetrics.totalAccumulated + goalMetrics.totalRemainingToGoal
    val goalPercent = if (goalPool > 0.0) {
        ((goalMetrics.totalAccumulated / goalPool) * 100.0).roundToInt().coerceIn(0, 100)
    } else {
        0
    }
    val debtActive = debtMetrics.owedToMeActiveCount + debtMetrics.iOweActiveCount
    val score = if (debtActive > 0) {
        (progressPercent ?: 0).coerceIn(0, 100)
    } else {
        goalPercent
    }
    val net = debtMetrics.owedToMeCents - debtMetrics.iOweCents
    val netAmount = Formatters.currencyFromCents(kotlin.math.abs(net), hide = hideAmounts)
    val netText = if (!hideAmounts && net < 0L) "−$netAmount" else netAmount

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.tab_debts),
                value = netText,
                container = PastelMint,
                onClick = onDebtsClick,
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.home_preview_accumulated),
                value = Formatters.currencyCompact(goalMetrics.totalAccumulated, hide = hideAmounts),
                container = PastelLilac,
                onClick = onAccumulatedClick,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PastelStat(
                label = stringResource(R.string.global_balance_remaining_label),
                value = Formatters.currencyCompact(goalMetrics.totalRemainingToGoal, hide = hideAmounts),
                container = PastelSand,
                onClick = onRemainingClick,
                modifier = Modifier.weight(1f),
            )
            PastelStat(
                label = stringResource(R.string.home_preview_progress),
                value = stringResource(R.string.home_preview_progress_value, score),
                container = PastelPeach,
                onClick = onProgressClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun FinOrb(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0xFFB7F5D8)),
        )
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFF4FFF9), Color(0xFF5AD7A2), Color(0xFF0C6B56)),
                    ),
                ),
        )
    }
}
