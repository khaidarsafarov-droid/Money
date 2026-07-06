package com.example.money.widget

import android.content.Context
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
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
import com.example.money.R
import com.example.money.navigation.AppIntents
import com.example.money.ui.theme.BrownMuted
import com.example.money.ui.theme.BrownText
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GaugeTrack
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.flow.first

class GoalProgressWidget : GlanceAppWidget() {

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

private val WidgetColorProviders = ColorProviders(
    lightColorScheme(
        primary = GoldPrimary,
        onPrimary = Color.White,
        background = CreamBackground,
        onBackground = BrownText,
        surface = CreamSurface,
        onSurface = BrownText,
        onSurfaceVariant = BrownMuted,
        surfaceVariant = GaugeTrack,
        tertiary = FinancePositive,
        onTertiary = Color.White,
        error = FinanceNegative,
        onError = Color.White,
    ),
)

@Composable
private fun GoalWidgetContent(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val region = snapshot.currencyRegion
    val webBaseUrl = context.getString(R.string.web_payment_base_url).trim()
    val openAppIntent = AppIntents.openAppIntent(context)
    val quickAddIntent = AppIntents.quickAddIntent(context)
    val hide = snapshot.hideAmounts

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        WidgetHeader(context = context, snapshot = snapshot)

        Spacer(modifier = GlanceModifier.height(8.dp))

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(12.dp)
                .background(GlanceTheme.colors.surface)
                .clickable(actionStartActivity(openAppIntent))
                .padding(12.dp),
        ) {
            Text(
                text = context.getString(
                    R.string.global_balance_available,
                    maskCompact(snapshot.globalBalance, region, hide),
                ),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
            )

            Text(
                text = context.getString(R.string.global_balance_income_minus_expenses),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetMetricTile(
                    label = context.getString(R.string.global_balance_accumulated_label),
                    value = maskCompact(snapshot.totalAccumulated, region, hide),
                    valueColor = GlanceTheme.colors.tertiary,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                WidgetMetricTile(
                    label = context.getString(R.string.global_balance_remaining_label),
                    value = maskCompact(snapshot.totalRemainingToGoal, region, hide),
                    valueColor = GlanceTheme.colors.onSurface,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                WidgetDebtTile(
                    context = context,
                    label = context.getString(R.string.debt_dashboard_owes_me),
                    amountCents = snapshot.owedToMeCents,
                    activeCount = snapshot.owedToMeCount,
                    positive = true,
                    region = region,
                    hide = hide,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                WidgetDebtTile(
                    context = context,
                    label = context.getString(R.string.debt_dashboard_i_owe),
                    amountCents = snapshot.iOweCents,
                    activeCount = snapshot.iOweCount,
                    positive = false,
                    region = region,
                    hide = hide,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }

            if (snapshot.totalDeposits > 0.0 || snapshot.totalExpenses > 0.0) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = context.getString(R.string.global_balance_deposits_label),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                    )
                    Text(
                        text = maskCompact(snapshot.totalDeposits, region, hide),
                        modifier = GlanceModifier.padding(start = 4.dp),
                        style = TextStyle(
                            color = GlanceTheme.colors.tertiary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = context.getString(R.string.global_balance_expenses_label),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                    )
                    Text(
                        text = maskCompact(snapshot.totalExpenses, region, hide),
                        modifier = GlanceModifier.padding(start = 4.dp),
                        style = TextStyle(
                            color = GlanceTheme.colors.error,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }

        if (snapshot.targetAmount > 0.0) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            WidgetGoalsSection(context = context, snapshot = snapshot, region = region, hide = hide)
        }

        if (snapshot.activeItems.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            WidgetActiveItemsSection(
                context = context,
                snapshot = snapshot,
                webBaseUrl = webBaseUrl,
            )
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        Button(
            text = context.getString(R.string.widget_quick_add),
            onClick = actionStartActivity(quickAddIntent),
            colors = ButtonDefaults.buttonColors(),
            modifier = GlanceModifier
                .fillMaxWidth()
                .cornerRadius(16.dp),
        )
    }
}

@Composable
private fun WidgetHeader(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = context.getString(R.string.app_name),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
            )
            if (snapshot.overdueDebtCount > 0) {
                Text(
                    text = context.getString(R.string.widget_overdue_debts, snapshot.overdueDebtCount),
                    style = TextStyle(
                        color = GlanceTheme.colors.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }

        if (snapshot.pendingCount > 0) {
            Text(
                text = context.getString(R.string.widget_pending_badge, snapshot.pendingCount),
                modifier = GlanceModifier
                    .cornerRadius(10.dp)
                    .background(GlanceTheme.colors.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                ),
            )
        }
    }
}

@Composable
private fun WidgetMetricTile(
    label: String,
    value: String,
    valueColor: androidx.glance.unit.ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .cornerRadius(8.dp)
            .background(GlanceTheme.colors.background)
            .padding(8.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
            ),
        )
        Text(
            text = value,
            style = TextStyle(
                color = valueColor,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun WidgetDebtTile(
    context: Context,
    label: String,
    amountCents: Long,
    activeCount: Int,
    positive: Boolean,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean = false,
    modifier: GlanceModifier = GlanceModifier,
) {
    val isEmpty = amountCents <= 0L
    val prefix = if (positive) "+" else "−"
    val amount = maskFromCents(amountCents, region, hide)
    val valueColor = when {
        isEmpty -> GlanceTheme.colors.onSurfaceVariant
        positive -> GlanceTheme.colors.tertiary
        else -> GlanceTheme.colors.error
    }

    Column(
        modifier = modifier
            .cornerRadius(8.dp)
            .background(
                if (isEmpty) {
                    GlanceTheme.colors.surfaceVariant
                } else {
                    GlanceTheme.colors.background
                },
            )
            .padding(8.dp),
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
            ),
        )
        Text(
            text = if (isEmpty) maskFromCents(0, region, hide) else "$prefix$amount",
            style = TextStyle(
                color = valueColor,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
        )
        Text(
            text = context.getString(R.string.debt_dashboard_active_count, activeCount),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
            ),
        )
    }
}

@Composable
private fun WidgetGoalsSection(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean = false,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_goals_section, snapshot.activeGoalsCount),
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
        )
        Text(
            text = context.getString(R.string.widget_progress_percent, snapshot.progressPercent),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            ),
        )
    }

    Spacer(modifier = GlanceModifier.height(6.dp))

    LinearProgressIndicator(
        progress = snapshot.progressFraction,
        modifier = GlanceModifier.fillMaxWidth(),
    )

    Spacer(modifier = GlanceModifier.height(4.dp))

    Text(
        text = context.getString(
            R.string.widget_remaining,
            maskCompact(snapshot.remainingAmount, region, hide),
            maskCompact(snapshot.targetAmount, region, hide),
        ),
        style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 11.sp,
        ),
    )
}

@Composable
private fun WidgetActiveItemsSection(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    webBaseUrl: String,
) {
    Text(
        text = context.getString(R.string.widget_active_items_title),
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
        ),
    )

    snapshot.activeItems.forEach { item ->
        Spacer(modifier = GlanceModifier.height(6.dp))
        val rowModifier = if (item.type == WidgetItemType.DEBT) {
            val debtIntent = if (webBaseUrl.isNotEmpty()) {
                AppIntents.openWebPaymentDeepLink(context, webBaseUrl, item.payKey)
            } else {
                AppIntents.openDebtPaymentIntent(context, item.id)
            }
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.surface)
                .clickable(actionStartActivity(debtIntent))
                .padding(8.dp)
        } else {
            GlanceModifier
                .fillMaxWidth()
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
        }

        Column(modifier = rowModifier) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val typeLabel = when (item.type) {
                    WidgetItemType.GOAL -> context.getString(R.string.widget_item_type_goal)
                    WidgetItemType.DEBT -> context.getString(R.string.widget_item_type_debt)
                }
                Text(
                    text = typeLabel,
                    modifier = GlanceModifier
                        .cornerRadius(6.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    style = TextStyle(
                        color = when {
                            item.isOverdue -> GlanceTheme.colors.error
                            item.type == WidgetItemType.GOAL -> GlanceTheme.colors.primary
                            else -> GlanceTheme.colors.error
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = item.name,
                    modifier = GlanceModifier.defaultWeight(),
                    style = TextStyle(
                        color = if (item.isOverdue) GlanceTheme.colors.error else GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                )
            }
            Text(
                text = context.getString(
                    R.string.widget_item_progress,
                    (item.progressFraction * 100).toInt(),
                    item.remainingLabel,
                ),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            LinearProgressIndicator(
                progress = item.progressFraction,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }
}

private fun maskCompact(
    amount: Double,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
): String = if (hide) {
    Formatters.MASKED_AMOUNT
} else {
    Formatters.currencyCompact(amount, region)
}

private fun maskFromCents(
    cents: Long,
    region: com.example.money.settings.CurrencyRegion,
    hide: Boolean,
): String = if (hide) {
    Formatters.MASKED_AMOUNT
} else {
    Formatters.currencyFromCents(cents, region)
}
