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
import com.example.money.settings.CurrencyRegion
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

private val Card = dayNight(LightColorScheme.surface, DarkColorScheme.surface)
private val Mint = dayNight(LightColorScheme.primaryContainer, DarkColorScheme.primaryContainer)
private val OnMint = dayNight(LightColorScheme.onPrimaryContainer, DarkColorScheme.onPrimaryContainer)
private val Peach = dayNight(LightColorScheme.errorContainer, DarkColorScheme.errorContainer)
private val OnPeach = dayNight(LightColorScheme.onErrorContainer, DarkColorScheme.onErrorContainer)
private val GoalTile = dayNight(LightColorScheme.surfaceContainerHigh, DarkColorScheme.surfaceContainerHigh)
private val OnGoal = dayNight(LightColorScheme.onSurface, DarkColorScheme.onSurface)
private val Plus = dayNight(LightColorScheme.primary, DarkColorScheme.primary)
private val OnPlus = dayNight(LightColorScheme.onPrimary, DarkColorScheme.onPrimary)

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
    val owesMe = actionStartActivity(AppIntents.openOwesMeIntent(context))
    val iOwe = actionStartActivity(AppIntents.openIOweIntent(context))
    val goals = actionStartActivity(AppIntents.openGoalsIntent(context))
    val add = actionStartActivity(AppIntents.quickAddIntent(context))
    val outer = widgetOuterRadius(context)
    val inner = (outer - 8.dp).coerceAtLeast(8.dp)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .wrapContentHeight()
            .cornerRadius(outer)
            .background(Card)
            .padding(12.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {
            WidgetTile(
                label = context.getString(R.string.debt_dashboard_owes_me),
                value = maskFromCents(snapshot.owedToMeCents, region, hide),
                container = Mint,
                content = OnMint,
                radius = inner,
                onClick = owesMe,
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            WidgetTile(
                label = context.getString(R.string.debt_dashboard_i_owe),
                value = maskFromCents(snapshot.iOweCents, region, hide),
                container = Peach,
                content = OnPeach,
                radius = inner,
                onClick = iOwe,
            )
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {
            WidgetTile(
                label = context.getString(R.string.section_goals),
                value = "${snapshot.progressPercent}%",
                container = GoalTile,
                content = OnGoal,
                radius = inner,
                onClick = goals,
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            PlusTile(
                radius = inner,
                onClick = add,
            )
        }
    }
}

@Composable
private fun RowScope.WidgetTile(
    label: String,
    value: String,
    container: ColorProvider,
    content: ColorProvider,
    radius: androidx.compose.ui.unit.Dp,
    onClick: androidx.glance.action.Action,
) {
    val amountSize = widgetAmountFontSize(LocalSize.current.width, value.length)
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .height(64.dp)
            .cornerRadius(radius)
            .background(container)
            .clickable(onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(color = content, fontSize = 12.sp),
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

@Composable
private fun RowScope.PlusTile(
    radius: androidx.compose.ui.unit.Dp,
    onClick: androidx.glance.action.Action,
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .height(64.dp)
            .cornerRadius(radius)
            .background(Plus)
            .clickable(onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "+",
            style = TextStyle(
                color = OnPlus,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
            maxLines = 1,
        )
    }
}

private fun maskFromCents(
    cents: Long,
    region: CurrencyRegion,
    hide: Boolean,
): String = Formatters.currencyFromCents(cents, region, hide)
