package com.example.money.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.money.R
import com.example.money.navigation.AppIntents
import com.example.money.ui.theme.DarkColorScheme
import com.example.money.ui.theme.LightColorScheme
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.flow.first

class GoalProgressWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetGoalRepository(context).observeSnapshot().first()

        provideContent {
            GlanceTheme(colors = WidgetColorProviders) {
                GoalWidgetContent(context = context, snapshot = snapshot)
            }
        }
    }
}

class GoalProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalProgressWidget()
}

private fun dayNight(day: Color, night: Color): ColorProvider =
    androidx.glance.color.ColorProvider(day, night)

private val PageBackground = dayNight(LightColorScheme.background, DarkColorScheme.background)
private val Hero = dayNight(LightColorScheme.primary, DarkColorScheme.primary)
private val OnHero = dayNight(LightColorScheme.onPrimary, DarkColorScheme.onPrimary)
private val HeroAlert = dayNight(LightColorScheme.errorContainer, DarkColorScheme.errorContainer)
private val Ink = dayNight(LightColorScheme.onSurface, DarkColorScheme.onSurface)
private val InkMuted = dayNight(LightColorScheme.onSurfaceVariant, DarkColorScheme.onSurfaceVariant)
private val PastelMint = dayNight(LightColorScheme.primaryContainer, DarkColorScheme.primaryContainer)
private val PastelPeach = dayNight(LightColorScheme.errorContainer, DarkColorScheme.errorContainer)
private val PastelLilac = dayNight(LightColorScheme.tertiaryContainer, DarkColorScheme.tertiaryContainer)
private val PastelSand = dayNight(LightColorScheme.secondaryContainer, DarkColorScheme.secondaryContainer)
private val CardSurface = dayNight(LightColorScheme.surfaceContainerLowest, DarkColorScheme.surfaceContainerLowest)
private val OverdueText = dayNight(LightColorScheme.error, DarkColorScheme.error)
private val ChipOverdue = dayNight(LightColorScheme.errorContainer, DarkColorScheme.errorContainer)
private val OnChipOverdue = dayNight(LightColorScheme.onErrorContainer, DarkColorScheme.onErrorContainer)
private val OnChip = dayNight(LightColorScheme.onPrimaryContainer, DarkColorScheme.onPrimaryContainer)

private val WidgetColorProviders = ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme,
)

private enum class WidgetDensity {
    Compact,
    Standard,
    Expanded,
}

private fun widgetDensity(height: Dp): WidgetDensity = when {
    height < 150.dp -> WidgetDensity.Compact
    height < 260.dp -> WidgetDensity.Standard
    else -> WidgetDensity.Expanded
}

@Composable
private fun GoalWidgetContent(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val density = widgetDensity(LocalSize.current.height)
    val region = snapshot.currencyRegion
    val hide = snapshot.hideAmounts
    val goalsIntent = AppIntents.openGoalsIntent(context)
    val quickAddIntent = AppIntents.quickAddIntent(context)
    val owesMeIntent = AppIntents.openOwesMeIntent(context)
    val iOweIntent = AppIntents.openIOweIntent(context)
    val webBaseUrl = context.getString(R.string.web_payment_base_url).trim()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(PageBackground)
            .padding(8.dp),
    ) {
        BalanceHero(
            context = context,
            snapshot = snapshot,
            region = region,
            hide = hide,
            showFormula = density != WidgetDensity.Compact,
            onClick = actionStartActivity(goalsIntent),
        )

        if (density != WidgetDensity.Compact) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            StatGrid(
                context = context,
                snapshot = snapshot,
                region = region,
                hide = hide,
                owesMe = actionStartActivity(owesMeIntent),
                iOwe = actionStartActivity(iOweIntent),
                goals = actionStartActivity(goalsIntent),
            )
        }

        if (density == WidgetDensity.Expanded) {
            if (snapshot.activeItems.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = context.getString(R.string.widget_active_items_title),
                    style = TextStyle(
                        color = InkMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                snapshot.activeItems.take(2).forEach { item ->
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    NextItemRow(
                        context = context,
                        item = item,
                        webBaseUrl = webBaseUrl,
                        goalsIntent = goalsIntent,
                    )
                }
            } else if (snapshot.activeGoalsCount == 0 && snapshot.owedToMeCents == 0L && snapshot.iOweCents == 0L) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = context.getString(R.string.widget_remaining_empty),
                    style = TextStyle(color = InkMuted, fontSize = 12.sp),
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.height(8.dp))
            QuickAddPill(
                label = context.getString(R.string.widget_quick_add),
                onClick = actionStartActivity(quickAddIntent),
            )
        }
    }
}

@Composable
private fun BalanceHero(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
    showFormula: Boolean,
    onClick: androidx.glance.action.Action,
) {
    val amountColor = OnHero
    val scoreLabel = context.getString(
        if (snapshot.scoreUsesDebts) R.string.widget_score_chip_debts else R.string.widget_score_chip_goals,
        snapshot.progressPercent,
    )

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(22.dp)
            .background(Hero)
            .clickable(onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_goals_debts_title),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = OnHero, fontSize = 12.sp),
            )
            Text(
                text = scoreLabel,
                style = TextStyle(
                    color = OnHero,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = Formatters.currency(snapshot.totalAccumulated, region, hide),
            style = TextStyle(
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
            ),
            maxLines = 1,
        )
        if (showFormula) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = context.getString(R.string.debt_dashboard_owes_me) +
                    " " + Formatters.currencyFromCents(snapshot.owedToMeCents, region, hide) +
                    " · " + context.getString(R.string.debt_dashboard_i_owe) +
                    " " + Formatters.currencyFromCents(snapshot.iOweCents, region, hide),
                style = TextStyle(color = OnHero, fontSize = 12.sp),
                maxLines = 2,
            )
        }
        if (snapshot.overdueDebtCount > 0 || snapshot.pendingCount > 0) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (snapshot.overdueDebtCount > 0) {
                    Text(
                        text = context.getString(R.string.widget_overdue_debts, snapshot.overdueDebtCount),
                        style = TextStyle(
                            color = HeroAlert,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                if (snapshot.overdueDebtCount > 0 && snapshot.pendingCount > 0) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                if (snapshot.pendingCount > 0) {
                    Text(
                        text = context.getString(R.string.widget_pending_badge, snapshot.pendingCount),
                        style = TextStyle(color = OnHero, fontSize = 12.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatGrid(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
    owesMe: androidx.glance.action.Action,
    iOwe: androidx.glance.action.Action,
    goals: androidx.glance.action.Action,
) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        PastelStat(
            label = context.getString(R.string.debt_dashboard_owes_me),
            value = maskFromCents(snapshot.owedToMeCents, region, hide),
            container = PastelMint,
            onClick = owesMe,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        PastelStat(
            label = context.getString(R.string.debt_dashboard_i_owe),
            value = maskFromCents(snapshot.iOweCents, region, hide),
            container = PastelPeach,
            onClick = iOwe,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
    Spacer(modifier = GlanceModifier.height(8.dp))
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        PastelStat(
            label = context.getString(R.string.global_balance_accumulated_label),
            value = maskCompact(snapshot.totalAccumulated, region, hide),
            container = PastelLilac,
            onClick = goals,
            modifier = GlanceModifier.defaultWeight(),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        PastelStat(
            label = context.getString(R.string.global_balance_remaining_label),
            value = maskCompact(snapshot.totalRemainingToGoal, region, hide),
            container = PastelSand,
            onClick = goals,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

@Composable
private fun PastelStat(
    label: String,
    value: String,
    container: ColorProvider,
    onClick: androidx.glance.action.Action,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier
            .cornerRadius(18.dp)
            .background(container)
            .clickable(onClick)
            .padding(8.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(color = InkMuted, fontSize = 12.sp),
            maxLines = 1,
        )
        Text(
            text = value,
            style = TextStyle(
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun NextItemRow(
    context: Context,
    item: WidgetFinanceItem,
    webBaseUrl: String,
    goalsIntent: android.content.Intent,
) {
    val openIntent = when (item.type) {
        WidgetItemType.DEBT -> if (webBaseUrl.isNotEmpty()) {
            AppIntents.openWebPaymentDeepLink(context, webBaseUrl, item.payKey)
        } else {
            AppIntents.openDebtPaymentIntent(context, item.id)
        }
        WidgetItemType.GOAL -> goalsIntent
    }
    val typeLabel = when (item.type) {
        WidgetItemType.GOAL -> context.getString(R.string.widget_item_type_goal)
        WidgetItemType.DEBT -> context.getString(R.string.widget_item_type_debt)
    }
    val chipColor = if (item.isOverdue) ChipOverdue else PastelMint
    val chipText = if (item.isOverdue) OnChipOverdue else OnChip

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(16.dp)
            .background(CardSurface)
            .clickable(actionStartActivity(openIntent))
            .padding(8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = typeLabel,
                modifier = GlanceModifier
                    .cornerRadius(8.dp)
                    .background(chipColor)
                    .padding(8.dp),
                style = TextStyle(
                    color = chipText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = item.name,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = if (item.isOverdue) OverdueText else Ink,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
            )
        }
        Text(
            text = context.getString(
                R.string.widget_item_progress,
                (item.progressFraction * 100).toInt().coerceIn(0, 100),
                item.remainingLabel,
            ),
            style = TextStyle(color = InkMuted, fontSize = 12.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickAddPill(
    label: String,
    onClick: androidx.glance.action.Action,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(50.dp)
            .background(Hero)
            .clickable(onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = OnHero,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        )
    }
}

private fun maskCompact(
    amount: Double,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
): String = Formatters.currencyCompact(amount, region, hide)

private fun maskFromCents(
    cents: Long,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
): String = Formatters.currencyFromCents(cents, region, hide)
