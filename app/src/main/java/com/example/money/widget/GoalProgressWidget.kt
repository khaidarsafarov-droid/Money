package com.example.money.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
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

private val Hero = dayNight(LightColorScheme.primary, DarkColorScheme.primary)
private val OnHero = dayNight(LightColorScheme.onPrimary, DarkColorScheme.onPrimary)
private val HeroAlert = dayNight(LightColorScheme.errorContainer, DarkColorScheme.errorContainer)
private val PastelMint = dayNight(LightColorScheme.primaryContainer, DarkColorScheme.primaryContainer)
private val PastelPeach = dayNight(LightColorScheme.errorContainer, DarkColorScheme.errorContainer)
private val PastelLilac = dayNight(LightColorScheme.tertiaryContainer, DarkColorScheme.tertiaryContainer)
private val PastelSand = dayNight(LightColorScheme.secondaryContainer, DarkColorScheme.secondaryContainer)
private val OnPastelMint = dayNight(LightColorScheme.onPrimaryContainer, DarkColorScheme.onPrimaryContainer)
private val OnPastelPeach = dayNight(LightColorScheme.onErrorContainer, DarkColorScheme.onErrorContainer)
private val OnPastelLilac = dayNight(LightColorScheme.onTertiaryContainer, DarkColorScheme.onTertiaryContainer)
private val OnPastelSand = dayNight(LightColorScheme.onSecondaryContainer, DarkColorScheme.onSecondaryContainer)

private val WidgetColorProviders = ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme,
)

@Composable
private fun GoalWidgetContent(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val region = snapshot.currencyRegion
    val hide = snapshot.hideAmounts
    val goalsIntent = AppIntents.openGoalsIntent(context)
    val owesMeIntent = AppIntents.openOwesMeIntent(context)
    val iOweIntent = AppIntents.openIOweIntent(context)

    Column(modifier = GlanceModifier.fillMaxSize()) {
        SummaryCard(
            context = context,
            snapshot = snapshot,
            region = region,
            hide = hide,
            onCardClick = actionStartActivity(goalsIntent),
            owesMe = actionStartActivity(owesMeIntent),
            iOwe = actionStartActivity(iOweIntent),
            goals = actionStartActivity(goalsIntent),
        )
    }
}

@Composable
private fun SummaryCard(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
    onCardClick: androidx.glance.action.Action,
    owesMe: androidx.glance.action.Action,
    iOwe: androidx.glance.action.Action,
    goals: androidx.glance.action.Action,
) {
    val scoreLabel = context.getString(
        if (snapshot.scoreUsesDebts) R.string.widget_score_chip_debts else R.string.widget_score_chip_goals,
        snapshot.progressPercent,
    )
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .wrapContentHeight()
            .cornerRadius(22.dp)
            .background(Hero)
            .clickable(onCardClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_goals_debts_title),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = OnHero, fontSize = 12.sp),
                maxLines = 1,
            )
            Text(
                text = scoreLabel,
                style = TextStyle(
                    color = OnHero,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
        if (snapshot.overdueDebtCount > 0 || snapshot.pendingCount > 0) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (snapshot.overdueDebtCount > 0) {
                    Text(
                        text = context.getString(R.string.widget_overdue_debts, snapshot.overdueDebtCount),
                        style = TextStyle(
                            color = HeroAlert,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                }
                if (snapshot.overdueDebtCount > 0 && snapshot.pendingCount > 0) {
                    Text(
                        text = " · ",
                        style = TextStyle(color = OnHero, fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
                if (snapshot.pendingCount > 0) {
                    Text(
                        text = context.getString(R.string.widget_pending_badge, snapshot.pendingCount),
                        style = TextStyle(color = OnHero, fontSize = 11.sp),
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        StatRow {
            PastelStat(
                label = context.getString(R.string.debt_dashboard_owes_me),
                value = maskFromCents(snapshot.owedToMeCents, region, hide),
                container = PastelMint,
                content = OnPastelMint,
                onClick = owesMe,
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            PastelStat(
                label = context.getString(R.string.debt_dashboard_i_owe),
                value = maskFromCents(snapshot.iOweCents, region, hide),
                container = PastelPeach,
                content = OnPastelPeach,
                onClick = iOwe,
            )
        }
        Spacer(modifier = GlanceModifier.height(4.dp))
        StatRow {
            PastelStat(
                label = context.getString(R.string.global_balance_accumulated_label),
                value = maskCompact(snapshot.totalAccumulated, region, hide),
                container = PastelLilac,
                content = OnPastelLilac,
                onClick = goals,
            )
            Spacer(modifier = GlanceModifier.width(4.dp))
            PastelStat(
                label = context.getString(R.string.global_balance_remaining_label),
                value = maskCompact(snapshot.totalRemainingToGoal, region, hide),
                container = PastelSand,
                content = OnPastelSand,
                onClick = goals,
            )
        }
    }
}

@Composable
private fun StatRow(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun RowScope.PastelStat(
    label: String,
    value: String,
    container: ColorProvider,
    content: ColorProvider,
    onClick: androidx.glance.action.Action,
) {
    val amountSize = widgetAmountFontSize(LocalSize.current.width, value.length)
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .wrapContentHeight()
            .cornerRadius(12.dp)
            .background(container)
            .clickable(onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(color = content, fontSize = 11.sp),
            maxLines = 1,
        )
        Text(
            text = value,
            style = TextStyle(
                color = content,
                fontWeight = FontWeight.Bold,
                fontSize = amountSize,
            ),
            maxLines = 1,
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
