package com.example.money.widget

import android.content.Context
import com.example.money.data.datastore.MoneyPreferencesStore
import kotlinx.coroutines.flow.Flow

/**
 * Минимальный репозиторий виджета: читает прогресс цели из DataStore.
 */
class WidgetGoalRepository(context: Context) {

    private val preferencesStore = MoneyPreferencesStore(context.applicationContext)

    fun observeSnapshot(): Flow<WidgetGoalSnapshot> = preferencesStore.widgetSnapshot
}
