package com.example.money.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Обновляет Glance-виджет только при изменении снимка в DataStore — без polling Room.
 */
class GoalWidgetUpdater(
    private val context: Context,
    private val widgetGoalRepository: WidgetGoalRepository,
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            widgetGoalRepository.observeSnapshot()
                .distinctUntilChanged()
                .collect {
                    refreshAllWidgets()
                }
        }
    }

    suspend fun refreshAllWidgets() {
        withContext(Dispatchers.Main.immediate) {
            try {
                val manager = GlanceAppWidgetManager(context)
                manager.getGlanceIds(GoalProgressWidget::class.java).forEach { glanceId ->
                    GoalProgressWidget().update(context, glanceId)
                }
            } catch (exception: Exception) {
                Log.w(TAG, "Widget refresh failed", exception)
            }
        }
    }

    private companion object {
        const val TAG = "GoalWidgetUpdater"
    }
}
