package com.example.money.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.navigation.AppIntents
import com.example.money.ui.theme.DarkColorScheme
import com.example.money.ui.theme.LightColorScheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    @Suppress("MissingSuperCall") // Refresh the snapshot first, then update each widget once.
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        val app = context.applicationContext as? MoneyApplication
        if (app == null) {
            pending.finish()
            return
        }
        app.applicationScope.launch {
            try {
                try {
                    app.refreshWidgetSnapshot()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.w(TAG, "Widget snapshot refresh failed", exception)
                }
                val manager = GlanceAppWidgetManager(context)
                appWidgetIds.forEach { appWidgetId ->
                    glanceAppWidget.update(context, manager.getGlanceIdBy(appWidgetId))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.w(TAG, "Widget update failed", exception)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "GoalProgressWidget"
    }
}

private fun dayNight(
    day: androidx.compose.ui.graphics.Color,
    night: androidx.compose.ui.graphics.Color,
): ColorProvider = androidx.glance.color.ColorProvider(day, night)

private val Card = dayNight(LightColorScheme.surface, DarkColorScheme.surface)
private val Primary = dayNight(LightColorScheme.primary, DarkColorScheme.primary)
private val OnPrimary = dayNight(LightColorScheme.onPrimary, DarkColorScheme.onPrimary)
private val Negative = dayNight(LightColorScheme.error, DarkColorScheme.error)
private val OnCard = dayNight(LightColorScheme.onSurface, DarkColorScheme.onSurface)
private val Muted = dayNight(LightColorScheme.onSurfaceVariant, DarkColorScheme.onSurfaceVariant)
private val Track = dayNight(LightColorScheme.primaryContainer, DarkColorScheme.primaryContainer)

private val WidgetColorProviders = androidx.glance.material3.ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme,
)

private val WidgetFont = FontFamily("onest")

private fun style(
    color: ColorProvider,
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    color = color,
    fontSize = size,
    fontWeight = weight,
    fontFamily = WidgetFont,
)

@Composable
internal fun GoalWidgetContent(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val size = LocalSize.current
    val wide = widgetUsesWideLayout(size.width)
    val tight = !wide && size.height < 140.dp
    val pad = if (tight) 6.dp else 12.dp
    val openApp = actionStartActivity(AppIntents.openAppIntent(context))
    val addDebt = actionStartActivity(AppIntents.addDebtIntent(context))
    val trackWidth = widgetGoalTrackWidth(size.width, pad, wide)
    val fillWidth = widgetProgressFillWidth(trackWidth, snapshot.featuredGoalPercent)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(widgetOuterRadius(context))
            .background(Card)
            .clickable(openApp)
            .padding(pad),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        WidgetHeader(context = context, addDebt = addDebt, tight = tight)
        Spacer(modifier = GlanceModifier.height(if (tight) 4.dp else 8.dp))
        if (wide) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.Top,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    DebtBlock(context, snapshot, tight)
                }
                if (snapshot.hasFeaturedGoal) {
                    Spacer(modifier = GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        GoalBlock(snapshot, fillWidth, tight)
                    }
                }
            }
        } else {
            DebtBlock(context, snapshot, tight)
            if (snapshot.hasFeaturedGoal) {
                Spacer(modifier = GlanceModifier.height(if (tight) 4.dp else 8.dp))
                GoalBlock(snapshot, fillWidth, tight)
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    context: Context,
    addDebt: Action,
    tight: Boolean,
) {
    val chip = if (tight) 20.dp else 28.dp
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.home_brand),
            style = style(Primary, if (tight) 12.sp else 13.sp, FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Box(
            modifier = GlanceModifier
                .width(chip)
                .height(chip)
                .cornerRadius(chip / 2)
                .background(Primary)
                .clickable(addDebt)
                .semantics { contentDescription = context.getString(R.string.widget_add_debt) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.widget_plus),
                style = style(OnPrimary, if (tight) 14.sp else 18.sp, FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DebtBlock(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    tight: Boolean,
) {
    val owes = maskFromCents(snapshot.owedToMeCents, snapshot)
    val owe = maskFromCents(snapshot.iOweCents, snapshot)
    val labelSize = if (tight) 10.sp else 11.sp
    val amountCap = if (tight) 13.sp else 18.sp
    DebtLine(
        label = context.getString(R.string.debt_dashboard_owes_me),
        amount = owes,
        amountColor = Primary,
        amountSize = cappedAmountSize(owes, amountCap),
        labelSize = labelSize,
    )
    Spacer(modifier = GlanceModifier.height(if (tight) 2.dp else 4.dp))
    DebtLine(
        label = context.getString(R.string.debt_dashboard_i_owe),
        amount = owe,
        amountColor = Negative,
        amountSize = cappedAmountSize(owe, amountCap),
        labelSize = labelSize,
    )
}

@Composable
private fun DebtLine(
    label: String,
    amount: String,
    amountColor: ColorProvider,
    amountSize: androidx.compose.ui.unit.TextUnit,
    labelSize: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = style(Muted, labelSize),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = amount,
            style = style(amountColor, amountSize, FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun GoalBlock(
    snapshot: WidgetGoalSnapshot,
    fillWidth: Dp,
    tight: Boolean,
) {
    val bar = if (tight) 4.dp else 6.dp
    Text(
        text = snapshot.featuredGoalName,
        style = style(OnCard, if (tight) 12.sp else 13.sp, FontWeight.Bold),
        maxLines = 1,
    )
    Spacer(modifier = GlanceModifier.height(2.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = featuredGoalRatio(snapshot),
            style = style(Muted, if (tight) 10.sp else 11.sp),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = featuredGoalPercentLabel(snapshot.featuredGoalPercent),
            style = style(Primary, if (tight) 12.sp else 13.sp, FontWeight.Bold),
            maxLines = 1,
        )
    }
    Spacer(modifier = GlanceModifier.height(if (tight) 3.dp else 4.dp))
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(bar)
            .cornerRadius(bar / 2)
            .background(Track),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (fillWidth >= 1.dp) {
            Box(
                modifier = GlanceModifier
                    .width(fillWidth)
                    .height(bar)
                    .cornerRadius(bar / 2)
                    .background(Primary),
            ) {}
        }
    }
}

@Composable
private fun cappedAmountSize(
    amount: String,
    cap: androidx.compose.ui.unit.TextUnit,
): androidx.compose.ui.unit.TextUnit {
    val raw = widgetAmountFontSize(LocalSize.current.width, amount.length)
    return if (raw > cap) cap else raw
}

private fun maskFromCents(cents: Long, snapshot: WidgetGoalSnapshot): String =
    com.example.money.ui.util.Formatters.currencyFromCents(
        cents,
        snapshot.currencyRegion,
        snapshot.hideAmounts,
    )
