package com.example.money.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.domain.sync.SnapshotVoiceSettings
import com.example.money.settings.CurrencyRegion
import com.example.money.voice.VoiceGender
import com.example.money.voice.VoiceLanguage
import com.example.money.voice.VoiceReportSettings
import com.example.money.widget.WidgetFinanceItem
import com.example.money.widget.WidgetGoalSnapshot
import com.example.money.widget.WidgetItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.moneyPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "money_preferences",
)

object MoneyPreferenceKeys {
    val CURRENT_AMOUNT = doublePreferencesKey("widget_current_amount")
    val TARGET_AMOUNT = doublePreferencesKey("widget_target_amount")
    val PROGRESS_FRACTION = doublePreferencesKey("widget_progress_fraction")
    val PROGRESS_PERCENT = intPreferencesKey("widget_progress_percent")
    val REMAINING_AMOUNT = doublePreferencesKey("widget_remaining_amount")
    val PENDING_COUNT = intPreferencesKey("widget_pending_count")
    val UPDATED_AT = longPreferencesKey("widget_updated_at")
    val CURRENCY_REGION = stringPreferencesKey("currency_region")
    val ACTIVE_ITEMS_JSON = stringPreferencesKey("widget_active_items_json")

    val VOICE_REPORTS_ENABLED = booleanPreferencesKey("voice_reports_enabled")
    val VOICE_REPORT_HOUR = intPreferencesKey("voice_report_hour")
    val VOICE_REPORT_MINUTE = intPreferencesKey("voice_report_minute")
    val LAST_VOICE_REPORT_DATE = stringPreferencesKey("last_voice_report_date")
    val LAST_APP_OPEN_DATE = stringPreferencesKey("last_app_open_date")
    val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
    val VOICE_GENDER = stringPreferencesKey("voice_gender")
    val VOICE_FORCE_SILENT = booleanPreferencesKey("voice_force_silent")
    val VOICE_SETTINGS_SYNC_AT = longPreferencesKey("voice_settings_sync_at")
}

class MoneyPreferencesStore(
    private val context: Context,
    private val syncQueueRecorder: SyncQueueRecorder? = null,
) {

    val widgetSnapshot: Flow<WidgetGoalSnapshot> =
        context.moneyPreferencesDataStore.data.map { prefs ->
            WidgetGoalSnapshot(
                currentAmount = prefs[MoneyPreferenceKeys.CURRENT_AMOUNT] ?: 0.0,
                targetAmount = prefs[MoneyPreferenceKeys.TARGET_AMOUNT] ?: 0.0,
                progressFraction = (prefs[MoneyPreferenceKeys.PROGRESS_FRACTION] ?: 0.0).toFloat(),
                progressPercent = prefs[MoneyPreferenceKeys.PROGRESS_PERCENT] ?: 0,
                remainingAmount = prefs[MoneyPreferenceKeys.REMAINING_AMOUNT] ?: 0.0,
                pendingCount = prefs[MoneyPreferenceKeys.PENDING_COUNT] ?: 0,
                updatedAt = prefs[MoneyPreferenceKeys.UPDATED_AT] ?: 0L,
                currencyRegion = CurrencyRegion.fromCode(
                    prefs[MoneyPreferenceKeys.CURRENCY_REGION] ?: CurrencyRegion.US.name,
                ),
                activeItems = decodeActiveItems(prefs[MoneyPreferenceKeys.ACTIVE_ITEMS_JSON]),
            )
        }

    val currencyRegion: Flow<CurrencyRegion> =
        context.moneyPreferencesDataStore.data.map { prefs ->
            CurrencyRegion.fromCode(
                prefs[MoneyPreferenceKeys.CURRENCY_REGION] ?: CurrencyRegion.US.name,
            )
        }

    suspend fun writeWidgetSnapshot(snapshot: WidgetGoalSnapshot) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs[MoneyPreferenceKeys.CURRENT_AMOUNT] = snapshot.currentAmount
            prefs[MoneyPreferenceKeys.TARGET_AMOUNT] = snapshot.targetAmount
            prefs[MoneyPreferenceKeys.PROGRESS_FRACTION] = snapshot.progressFraction.toDouble()
            prefs[MoneyPreferenceKeys.PROGRESS_PERCENT] = snapshot.progressPercent
            prefs[MoneyPreferenceKeys.REMAINING_AMOUNT] = snapshot.remainingAmount
            prefs[MoneyPreferenceKeys.PENDING_COUNT] = snapshot.pendingCount
            prefs[MoneyPreferenceKeys.UPDATED_AT] = snapshot.updatedAt
            prefs[MoneyPreferenceKeys.CURRENCY_REGION] = snapshot.currencyRegion.name
            prefs[MoneyPreferenceKeys.ACTIVE_ITEMS_JSON] = encodeActiveItems(snapshot.activeItems)
        }
    }

    suspend fun setCurrencyRegion(region: CurrencyRegion) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs[MoneyPreferenceKeys.CURRENCY_REGION] = region.name
        }
    }

    val voiceReportSettings: Flow<VoiceReportSettings> =
        context.moneyPreferencesDataStore.data.map { prefs ->
            prefs.toVoiceReportSettings()
        }

    suspend fun getVoiceSettingsSyncUpdatedAt(): Long? {
        val prefs = context.moneyPreferencesDataStore.data.first()
        return prefs[MoneyPreferenceKeys.VOICE_SETTINGS_SYNC_AT]
    }

    suspend fun applySyncedVoiceSettings(settings: SnapshotVoiceSettings) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeVoiceReportSettings(
                VoiceReportSettings(
                    enabled = settings.enabled,
                    reportHour = settings.reportHour,
                    reportMinute = settings.reportMinute,
                    language = VoiceLanguage.fromStored(settings.language),
                    gender = VoiceGender.fromStored(settings.gender),
                    forceInSilentMode = settings.forceInSilentMode,
                    lastVoiceReportDate = settings.lastVoiceReportDate,
                    lastAppOpenDate = settings.lastAppOpenDate,
                ),
            )
            prefs[MoneyPreferenceKeys.VOICE_SETTINGS_SYNC_AT] = settings.syncUpdatedAt
        }
    }

    suspend fun updateVoiceReportSettings(transform: (VoiceReportSettings) -> VoiceReportSettings) {
        val syncUpdatedAt = System.currentTimeMillis()
        var updatedSettings: VoiceReportSettings? = null
        context.moneyPreferencesDataStore.edit { prefs ->
            val updated = transform(prefs.toVoiceReportSettings())
            prefs.writeVoiceReportSettings(updated)
            prefs[MoneyPreferenceKeys.VOICE_SETTINGS_SYNC_AT] = syncUpdatedAt
            updatedSettings = updated
        }
        updatedSettings?.let { settings ->
            syncQueueRecorder?.recordVoiceSettings(settings, syncUpdatedAt)
        }
    }

    suspend fun markVoiceReportDelivered(isoDate: String) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs[MoneyPreferenceKeys.LAST_VOICE_REPORT_DATE] = isoDate
        }
    }

    suspend fun markAppOpenedToday(isoDate: String) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs[MoneyPreferenceKeys.LAST_APP_OPEN_DATE] = isoDate
        }
    }

    private fun encodeActiveItems(items: List<WidgetFinanceItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("payKey", item.payKey)
                    .put("type", item.type.name)
                    .put("name", item.name)
                    .put("remaining", item.remainingLabel)
                    .put("progress", item.progressFraction.toDouble()),
            )
        }
        return array.toString()
    }

    private fun decodeActiveItems(raw: String?): List<WidgetFinanceItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        WidgetFinanceItem(
                            id = item.getLong("id"),
                            payKey = if (item.has("payKey")) {
                                item.getString("payKey")
                            } else {
                                val type = WidgetItemType.valueOf(item.getString("type"))
                                "${type.name.lowercase()}-${item.getLong("id")}"
                            },
                            type = WidgetItemType.valueOf(item.getString("type")),
                            name = item.getString("name"),
                            remainingLabel = item.getString("remaining"),
                            progressFraction = item.getDouble("progress").toFloat(),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

private fun Preferences.toVoiceReportSettings(): VoiceReportSettings =
    VoiceReportSettings(
        enabled = this[MoneyPreferenceKeys.VOICE_REPORTS_ENABLED] ?: false,
        reportHour = this[MoneyPreferenceKeys.VOICE_REPORT_HOUR] ?: VoiceReportSettings.DEFAULT_HOUR,
        reportMinute = this[MoneyPreferenceKeys.VOICE_REPORT_MINUTE] ?: VoiceReportSettings.DEFAULT_MINUTE,
        language = VoiceLanguage.fromStored(this[MoneyPreferenceKeys.VOICE_LANGUAGE]),
        gender = VoiceGender.fromStored(this[MoneyPreferenceKeys.VOICE_GENDER]),
        forceInSilentMode = this[MoneyPreferenceKeys.VOICE_FORCE_SILENT] ?: false,
        lastVoiceReportDate = this[MoneyPreferenceKeys.LAST_VOICE_REPORT_DATE],
        lastAppOpenDate = this[MoneyPreferenceKeys.LAST_APP_OPEN_DATE],
    )

private fun MutablePreferences.writeVoiceReportSettings(settings: VoiceReportSettings) {
    this[MoneyPreferenceKeys.VOICE_REPORTS_ENABLED] = settings.enabled
    this[MoneyPreferenceKeys.VOICE_REPORT_HOUR] = settings.reportHour
    this[MoneyPreferenceKeys.VOICE_REPORT_MINUTE] = settings.reportMinute
    this[MoneyPreferenceKeys.VOICE_LANGUAGE] = settings.language.name
    this[MoneyPreferenceKeys.VOICE_GENDER] = settings.gender.name
    this[MoneyPreferenceKeys.VOICE_FORCE_SILENT] = settings.forceInSilentMode
    settings.lastVoiceReportDate?.let {
        this[MoneyPreferenceKeys.LAST_VOICE_REPORT_DATE] = it
    } ?: remove(MoneyPreferenceKeys.LAST_VOICE_REPORT_DATE)
    settings.lastAppOpenDate?.let {
        this[MoneyPreferenceKeys.LAST_APP_OPEN_DATE] = it
    } ?: remove(MoneyPreferenceKeys.LAST_APP_OPEN_DATE)
}
