package com.example.money.widget

import android.content.Context
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    ),
)

@Composable
private fun GoalWidgetContent(
    context: Context,
    snapshot: WidgetGoalSnapshot,
) {
    val region = snapshot.currencyRegion
    val webBaseUrl = context.getString(R.string.web_payment_base_url).trim()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.background)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_goal_title),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = "${snapshot.progressPercent}%",
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        if (snapshot.pendingCount > 0) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_pending_count, snapshot.pendingCount),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        }

        if (snapshot.streakCount > 0) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.gamification_streak, snapshot.streakCount),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }

        snapshot.lastWinMessage?.let { win ->
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = win,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        }

        Spacer(modifier = GlanceModifier.height(10.dp))

        if (snapshot.targetAmount > 0) {
            LinearProgressIndicator(
                progress = snapshot.progressFraction,
                modifier = GlanceModifier.fillMaxWidth(),
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Text(
                text = if (snapshot.hideAmounts) {
                    Formatters.MASKED_AMOUNT
                } else {
                    Formatters.currencyCompact(snapshot.currentAmount, region)
                },
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )

            Text(
                text = context.getString(
                    R.string.widget_remaining,
                    if (snapshot.hideAmounts) Formatters.MASKED_AMOUNT else Formatters.currencyCompact(snapshot.remainingAmount, region),
                    if (snapshot.hideAmounts) Formatters.MASKED_AMOUNT else Formatters.currencyCompact(snapshot.targetAmount, region),
                ),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        } else {
            Text(
                text = context.getString(R.string.widget_remaining_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )
        }

        if (snapshot.activeItems.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(10.dp))
            Text(
                text = context.getString(R.string.widget_active_items_title),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            snapshot.activeItems.take(4).forEach { item ->
                Spacer(modifier = GlanceModifier.height(6.dp))
                val rowModifier = if (item.type == WidgetItemType.DEBT) {
                    val debtIntent = if (webBaseUrl.isNotEmpty()) {
                        AppIntents.openWebPaymentDeepLink(context, webBaseUrl, item.payKey)
                    } else {
                        AppIntents.openDebtPaymentIntent(context, item.id)
                    }
                    GlanceModifier
                        .fillMaxWidth()
                        .clickable(actionStartActivity(debtIntent))
                } else {
                    GlanceModifier.fillMaxWidth()
                }
                Column(modifier = rowModifier) {
                    Text(
                        text = item.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = context.getString(
                            R.string.widget_item_progress,
                            (item.progressFraction * 100).toInt(),
                            item.remainingLabel,
                        ),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    )
                    LinearProgressIndicator(
                        progress = item.progressFraction,
                        modifier = GlanceModifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.defaultWeight())

        val quickAddIntent = AppIntents.quickAddIntent(context)

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Button(
                text = "+",
                onClick = actionStartActivity(quickAddIntent),
                colors = ButtonDefaults.buttonColors(),
                modifier = GlanceModifier
                    .cornerRadius(20.dp)
                    .padding(bottom = 4.dp),
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

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
