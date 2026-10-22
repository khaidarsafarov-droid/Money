package com.example.money.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.text.TextAlign
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

private fun fixed(color: Color): ColorProvider = androidx.glance.color.ColorProvider(color, color)

private val Mint = fixed(Color(0xFF5FD3B3))
private val Pink = fixed(Color(0xFFF2B8B5))
private val Title = fixed(Color(0xFFFFFFFF))
private val Label = fixed(Color(0xFFD7EBE4))
private val PlusFill = fixed(Color(0xFF8FE0C8))
private val PlusInk = fixed(Color(0xFF06352C))
private val Track = fixed(Color(0xFF08352C))

private val WidgetColorProviders = androidx.glance.material3.ColorProviders(
    light = LightColorScheme,
    dark = DarkColorScheme,
)

private val WidgetFont = FontFamily("onest")

private fun style(
    color: ColorProvider,
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign? = null,
) = TextStyle(
    color = color,
    fontSize = size,
    fontWeight = weight,
    textAlign = align,
    fontFamily = WidgetFont,
)

@Composable
internal fun GoalWidgetContent(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val size = LocalSize.current
    val compact = size.height < 132.dp
    val pad = widgetContentPadding(size.height)
    val openApp = actionStartActivity(AppIntents.openAppIntent(context))
    val addDebt = actionStartActivity(AppIntents.addDebtIntent(context))
    val trackWidth = widgetGoalTrackWidth(size.width, pad)
    val fillWidth = widgetProgressFillWidth(trackWidth, snapshot.featuredGoalPercent)
    val sideBySide = widgetSideBySideAmounts(size.width)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(widgetOuterRadius(context))
            .background(ImageProvider(R.drawable.widget_card_bg))
            .clickable(openApp)
            .padding(pad),
        verticalAlignment = Alignment.Vertical.Top,
    ) {
        WidgetHeader(context = context, addDebt = addDebt, compact = compact)
        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            if (sideBySide) {
                Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 10.dp))
                AmountColumns(context, snapshot, compact)
            } else if (compact) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                InlineDebts(context, snapshot)
            } else {
                Spacer(modifier = GlanceModifier.height(8.dp))
                StackedAmounts(context, snapshot)
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (snapshot.hasFeaturedGoal) {
                GoalFooter(snapshot, fillWidth, compact)
            }
        }
    }
}

@Composable
private fun WidgetHeader(
    context: Context,
    addDebt: Action,
    compact: Boolean,
) {
    val chip = widgetPlusSize(LocalSize.current.height)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.home_brand),
            style = style(Title, if (compact) 16.sp else 18.sp, FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Box(
            modifier = GlanceModifier
                .width(chip)
                .height(chip)
                .cornerRadius(chip / 2)
                .background(PlusFill)
                .clickable(addDebt)
                .semantics { contentDescription = context.getString(R.string.widget_add_debt) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = context.getString(R.string.widget_plus),
                style = style(PlusInk, if (compact) 16.sp else 20.sp, FontWeight.Bold),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AmountColumns(
    context: Context,
    snapshot: WidgetGoalSnapshot,
    compact: Boolean,
) {
    val owes = maskFromCents(snapshot.owedToMeCents, snapshot)
    val owe = maskFromCents(snapshot.iOweCents, snapshot)
    val size = LocalSize.current
    val hero = widgetHeroAmountSize(size.width, size.height, owes.length)
    val secondary = widgetSecondaryAmountSize(hero)
    val labelSize = if (compact) 12.sp else 13.sp
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = context.getString(R.string.debt_dashboard_owes_me),
                style = style(Label, labelSize),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = owes,
                style = style(Mint, hero, FontWeight.Bold),
                maxLines = 1,
            )
        }
        Column(
            modifier = GlanceModifier.defaultWeight(),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = context.getString(R.string.debt_dashboard_i_owe),
                style = style(Label, labelSize, align = TextAlign.End),
                maxLines = 1,
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = owe,
                style = style(Pink, secondary, FontWeight.Bold, TextAlign.End),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StackedAmounts(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val owes = maskFromCents(snapshot.owedToMeCents, snapshot)
    val owe = maskFromCents(snapshot.iOweCents, snapshot)
    val size = LocalSize.current
    val hero = widgetHeroAmountSize(size.width, size.height, owes.length)
    AmountStack(
        label = context.getString(R.string.debt_dashboard_owes_me),
        amount = owes,
        amountColor = Mint,
        amountSize = hero,
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    AmountStack(
        label = context.getString(R.string.debt_dashboard_i_owe),
        amount = owe,
        amountColor = Pink,
        amountSize = widgetSecondaryAmountSize(hero),
    )
}

@Composable
private fun AmountStack(
    label: String,
    amount: String,
    amountColor: ColorProvider,
    amountSize: androidx.compose.ui.unit.TextUnit,
) {
    Text(text = label, style = style(Label, 12.sp), maxLines = 1)
    Spacer(modifier = GlanceModifier.height(2.dp))
    Text(text = amount, style = style(amountColor, amountSize, FontWeight.Bold), maxLines = 1)
}

@Composable
private fun InlineDebts(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val owes = maskFromCents(snapshot.owedToMeCents, snapshot)
    val owe = maskFromCents(snapshot.iOweCents, snapshot)
    val size = LocalSize.current
    val hero = widgetHeroAmountSize(size.width, size.height, owes.length)
    InlineDebt(
        label = context.getString(R.string.debt_dashboard_owes_me),
        amount = owes,
        amountColor = Mint,
        amountSize = hero,
    )
    Spacer(modifier = GlanceModifier.height(2.dp))
    InlineDebt(
        label = context.getString(R.string.debt_dashboard_i_owe),
        amount = owe,
        amountColor = Pink,
        amountSize = widgetSecondaryAmountSize(hero),
    )
}

@Composable
private fun InlineDebt(
    label: String,
    amount: String,
    amountColor: ColorProvider,
    amountSize: androidx.compose.ui.unit.TextUnit,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = style(Label, 12.sp),
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
private fun GoalFooter(
    snapshot: WidgetGoalSnapshot,
    fillWidth: Dp,
    compact: Boolean,
) {
    val bar = if (compact) 5.dp else 8.dp
    val flag = if (compact) 12.dp else 14.dp
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.widget_goal_flag),
            contentDescription = null,
            modifier = GlanceModifier.width(flag).height(flag),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
            text = snapshot.featuredGoalName,
            style = style(Title, if (compact) 12.sp else 14.sp, FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = featuredGoalRatio(snapshot),
            style = style(Label, if (compact) 12.sp else 13.sp),
            maxLines = 1,
        )
        Text(
            text = " · ${featuredGoalPercentLabel(snapshot.featuredGoalPercent)}",
            style = style(Mint, if (compact) 12.sp else 13.sp, FontWeight.Bold),
            maxLines = 1,
        )
    }
    Spacer(modifier = GlanceModifier.height(if (compact) 4.dp else 8.dp))
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
                    .background(Mint),
            ) {}
        }
    }
}

private fun maskFromCents(cents: Long, snapshot: WidgetGoalSnapshot): String =
    com.example.money.ui.util.Formatters.currencyFromCents(
        cents,
        snapshot.currencyRegion,
        snapshot.hideAmounts,
    )
