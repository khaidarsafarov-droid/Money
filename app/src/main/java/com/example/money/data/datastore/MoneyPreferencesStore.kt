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
import com.example.money.domain.model.FinanceBranch
import com.example.money.domain.sync.SnapshotVoiceSettings
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.BusinessBranchSettings
import com.example.money.settings.FinancialPsychotype
import com.example.money.settings.GoalPsychologyProfile
import com.example.money.settings.PrivacySettings
import com.example.money.settings.CalendarSettings
import com.example.money.settings.UserEmotion
import com.example.money.settings.UserGender
import com.example.money.settings.UserProfile
import com.example.money.settings.WeeklyCheckInSettings
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
    val GLOBAL_BALANCE = doublePreferencesKey("widget_global_balance")
    val TOTAL_DEPOSITS = doublePreferencesKey("widget_total_deposits")
    val TOTAL_EXPENSES = doublePreferencesKey("widget_total_expenses")
    val TOTAL_ACCUMULATED = doublePreferencesKey("widget_total_accumulated")
    val TOTAL_REMAINING_TO_GOAL = doublePreferencesKey("widget_total_remaining_to_goal")
    val OWED_TO_ME_CENTS = longPreferencesKey("widget_owed_to_me_cents")
    val OWED_TO_ME_COUNT = intPreferencesKey("widget_owed_to_me_count")
    val I_OWE_CENTS = longPreferencesKey("widget_i_owe_cents")
    val I_OWE_COUNT = intPreferencesKey("widget_i_owe_count")
    val ACTIVE_GOALS_COUNT = intPreferencesKey("widget_active_goals_count")
    val OVERDUE_DEBT_COUNT = intPreferencesKey("widget_overdue_debt_count")
    val WIDGET_HIDE_AMOUNTS = booleanPreferencesKey("widget_hide_amounts")
    val WIDGET_STREAK_COUNT = intPreferencesKey("widget_streak_count")
    val WIDGET_LAST_WIN = stringPreferencesKey("widget_last_win")

    val VOICE_REPORTS_ENABLED = booleanPreferencesKey("voice_reports_enabled")
    val VOICE_REPORT_HOUR = intPreferencesKey("voice_report_hour")
    val VOICE_REPORT_MINUTE = intPreferencesKey("voice_report_minute")
    val LAST_VOICE_REPORT_DATE = stringPreferencesKey("last_voice_report_date")
    val LAST_APP_OPEN_DATE = stringPreferencesKey("last_app_open_date")
    val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
    val VOICE_GENDER = stringPreferencesKey("voice_gender")
    val VOICE_FORCE_SILENT = booleanPreferencesKey("voice_force_silent")
    val VOICE_SETTINGS_SYNC_AT = longPreferencesKey("voice_settings_sync_at")

    val PRIVACY_HIDE_AMOUNTS = booleanPreferencesKey("privacy_hide_amounts")
    val PRIVACY_HIDE_ON_WIDGET = booleanPreferencesKey("privacy_hide_on_widget")

    val GAMIFICATION_STREAK = intPreferencesKey("gamification_streak")
    val GAMIFICATION_LAST_PAYMENT_ISO = stringPreferencesKey("gamification_last_payment_iso")
    val GAMIFICATION_UNLOCKED_BADGES_JSON = stringPreferencesKey("gamification_unlocked_badges_json")
    val GAMIFICATION_LAST_WIN = stringPreferencesKey("gamification_last_win")

    val AUTOPILOT_ENABLED = booleanPreferencesKey("autopilot_enabled")
    val AUTOPILOT_DAILY_CENTS = longPreferencesKey("autopilot_daily_cents")
    val AUTOPILOT_STRATEGY = stringPreferencesKey("autopilot_strategy")
    val AUTOPILOT_REMINDER_HOUR = intPreferencesKey("autopilot_reminder_hour")
    val AUTOPILOT_REMINDER_MINUTE = intPreferencesKey("autopilot_reminder_minute")
    val AUTOPILOT_ROUND_TO_TEN = booleanPreferencesKey("autopilot_round_to_ten")

    val WEEKLY_CHECKIN_ENABLED = booleanPreferencesKey("weekly_checkin_enabled")
    val WEEKLY_CHECKIN_HOUR = intPreferencesKey("weekly_checkin_hour")

    val ADVISOR_HISTORY_JSON = stringPreferencesKey("advisor_history_json")

    val GOAL_PSYCHOTYPE = stringPreferencesKey("goal_psychotype")
    val GOAL_PSYCHOTYPE_ANSWERS_JSON = stringPreferencesKey("goal_psychotype_answers_json")
    val GOAL_PSYCHOTYPE_COMPLETED = booleanPreferencesKey("goal_psychotype_completed")
    val GOAL_FEARS_JSON = stringPreferencesKey("goal_fears_json")
    val GOAL_WORD_POWER = booleanPreferencesKey("goal_word_power")
    val GOAL_PERSONAL_MOTIVATION = stringPreferencesKey("goal_personal_motivation")
    val GOAL_VISION_PHOTO_PATH = stringPreferencesKey("goal_vision_photo_path")
    val GOAL_PAYDAY_DAY = intPreferencesKey("goal_payday_day")
    val GOAL_PAYOFF_STRATEGY = stringPreferencesKey("goal_payoff_strategy")
    val GOAL_WEEKLY_CHECKLIST_JSON = stringPreferencesKey("goal_weekly_checklist_json")
    val GOAL_ACTIVE_CHALLENGES_JSON = stringPreferencesKey("goal_active_challenges_json")
    val GOAL_BADGES_JSON = stringPreferencesKey("goal_badges_json")

    val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
    val USER_GENDER = stringPreferencesKey("user_gender")
    val USER_AGE = intPreferencesKey("user_age")
    val USER_CUSTOM_FEAR = stringPreferencesKey("user_custom_fear")
    val USER_GOAL_TEXT = stringPreferencesKey("user_goal_text")
    val USER_EMOTION = stringPreferencesKey("user_emotion")
    val USER_PRIORITY = stringPreferencesKey("user_priority")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    val CALENDAR_AUTO_SYNC = booleanPreferencesKey("calendar_auto_sync")
    val CALENDAR_REMIND_1D = booleanPreferencesKey("calendar_remind_1d")
    val CALENDAR_REMIND_3D = booleanPreferencesKey("calendar_remind_3d")
    val CALENDAR_REMIND_DUE = booleanPreferencesKey("calendar_remind_due")
    val CALENDAR_USE_DEBTS_CAL = booleanPreferencesKey("calendar_use_debts_cal")
    val CALENDAR_DEBTS_CAL_ID = longPreferencesKey("calendar_debts_cal_id")
    val CALENDAR_PERMISSION_REQUESTED = booleanPreferencesKey("calendar_permission_requested")
    val NOTIFICATION_PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    val ACTIVE_FINANCE_BRANCH = stringPreferencesKey("active_finance_branch")
    val BUSINESS_BRANCH_NAME = stringPreferencesKey("business_branch_name")
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
                globalBalance = prefs[MoneyPreferenceKeys.GLOBAL_BALANCE] ?: 0.0,
                totalDeposits = prefs[MoneyPreferenceKeys.TOTAL_DEPOSITS] ?: 0.0,
                totalExpenses = prefs[MoneyPreferenceKeys.TOTAL_EXPENSES] ?: 0.0,
                totalAccumulated = prefs[MoneyPreferenceKeys.TOTAL_ACCUMULATED] ?: 0.0,
                totalRemainingToGoal = prefs[MoneyPreferenceKeys.TOTAL_REMAINING_TO_GOAL] ?: 0.0,
                owedToMeCents = prefs[MoneyPreferenceKeys.OWED_TO_ME_CENTS] ?: 0L,
                owedToMeCount = prefs[MoneyPreferenceKeys.OWED_TO_ME_COUNT] ?: 0,
                iOweCents = prefs[MoneyPreferenceKeys.I_OWE_CENTS] ?: 0L,
                iOweCount = prefs[MoneyPreferenceKeys.I_OWE_COUNT] ?: 0,
                activeGoalsCount = prefs[MoneyPreferenceKeys.ACTIVE_GOALS_COUNT] ?: 0,
                overdueDebtCount = prefs[MoneyPreferenceKeys.OVERDUE_DEBT_COUNT] ?: 0,
                hideAmounts = prefs[MoneyPreferenceKeys.WIDGET_HIDE_AMOUNTS] ?: false,
                streakCount = prefs[MoneyPreferenceKeys.WIDGET_STREAK_COUNT] ?: 0,
                lastWinMessage = prefs[MoneyPreferenceKeys.WIDGET_LAST_WIN],
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
            prefs[MoneyPreferenceKeys.GLOBAL_BALANCE] = snapshot.globalBalance
            prefs[MoneyPreferenceKeys.TOTAL_DEPOSITS] = snapshot.totalDeposits
            prefs[MoneyPreferenceKeys.TOTAL_EXPENSES] = snapshot.totalExpenses
            prefs[MoneyPreferenceKeys.TOTAL_ACCUMULATED] = snapshot.totalAccumulated
            prefs[MoneyPreferenceKeys.TOTAL_REMAINING_TO_GOAL] = snapshot.totalRemainingToGoal
            prefs[MoneyPreferenceKeys.OWED_TO_ME_CENTS] = snapshot.owedToMeCents
            prefs[MoneyPreferenceKeys.OWED_TO_ME_COUNT] = snapshot.owedToMeCount
            prefs[MoneyPreferenceKeys.I_OWE_CENTS] = snapshot.iOweCents
            prefs[MoneyPreferenceKeys.I_OWE_COUNT] = snapshot.iOweCount
            prefs[MoneyPreferenceKeys.ACTIVE_GOALS_COUNT] = snapshot.activeGoalsCount
            prefs[MoneyPreferenceKeys.OVERDUE_DEBT_COUNT] = snapshot.overdueDebtCount
            prefs[MoneyPreferenceKeys.WIDGET_HIDE_AMOUNTS] = snapshot.hideAmounts
            prefs[MoneyPreferenceKeys.WIDGET_STREAK_COUNT] = snapshot.streakCount
            snapshot.lastWinMessage?.let {
                prefs[MoneyPreferenceKeys.WIDGET_LAST_WIN] = it
            } ?: remove(MoneyPreferenceKeys.WIDGET_LAST_WIN)
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

    val privacySettings: Flow<PrivacySettings> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toPrivacySettings() }

    suspend fun updatePrivacySettings(transform: (PrivacySettings) -> PrivacySettings) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writePrivacySettings(transform(prefs.toPrivacySettings()))
        }
    }

    val autopilotSettings: Flow<AutopilotSettings> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toAutopilotSettings() }

    suspend fun updateAutopilotSettings(transform: (AutopilotSettings) -> AutopilotSettings) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeAutopilotSettings(transform(prefs.toAutopilotSettings()))
        }
    }

    val gamificationState: Flow<GamificationState> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toGamificationState() }

    suspend fun updateGamificationState(transform: (GamificationState) -> GamificationState) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeGamificationState(transform(prefs.toGamificationState()))
        }
    }

    val advisorHistoryJson: Flow<String> =
        context.moneyPreferencesDataStore.data.map { prefs ->
            prefs[MoneyPreferenceKeys.ADVISOR_HISTORY_JSON] ?: "[]"
        }

    suspend fun writeAdvisorHistoryJson(json: String) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs[MoneyPreferenceKeys.ADVISOR_HISTORY_JSON] = json
        }
    }

    val weeklyCheckInSettings: Flow<WeeklyCheckInSettings> =
        context.moneyPreferencesDataStore.data.map { prefs ->
            WeeklyCheckInSettings(
                enabled = prefs[MoneyPreferenceKeys.WEEKLY_CHECKIN_ENABLED] ?: false,
                hour = prefs[MoneyPreferenceKeys.WEEKLY_CHECKIN_HOUR] ?: 10,
            )
        }

    suspend fun updateWeeklyCheckInSettings(transform: (WeeklyCheckInSettings) -> WeeklyCheckInSettings) {
        context.moneyPreferencesDataStore.edit { prefs ->
            val updated = transform(
                WeeklyCheckInSettings(
                    enabled = prefs[MoneyPreferenceKeys.WEEKLY_CHECKIN_ENABLED] ?: false,
                    hour = prefs[MoneyPreferenceKeys.WEEKLY_CHECKIN_HOUR] ?: 10,
                ),
            )
            prefs[MoneyPreferenceKeys.WEEKLY_CHECKIN_ENABLED] = updated.enabled
            prefs[MoneyPreferenceKeys.WEEKLY_CHECKIN_HOUR] = updated.hour
        }
    }

    val goalPsychologyProfile: Flow<GoalPsychologyProfile> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toGoalPsychologyProfile() }

    suspend fun updateGoalPsychologyProfile(transform: (GoalPsychologyProfile) -> GoalPsychologyProfile) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeGoalPsychologyProfile(transform(prefs.toGoalPsychologyProfile()))
        }
    }

    val userProfile: Flow<UserProfile> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toUserProfile() }

    suspend fun updateUserProfile(transform: (UserProfile) -> UserProfile) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeUserProfile(transform(prefs.toUserProfile()))
        }
    }

    val calendarSettings: Flow<CalendarSettings> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toCalendarSettings() }

    suspend fun updateCalendarSettings(transform: (CalendarSettings) -> CalendarSettings) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeCalendarSettings(transform(prefs.toCalendarSettings()))
        }
    }

    val notificationPermissionRequested: Flow<Boolean> =
        context.moneyPreferencesDataStore.data.map {
            it[MoneyPreferenceKeys.NOTIFICATION_PERMISSION_REQUESTED] ?: false
        }

    suspend fun setNotificationPermissionRequested(requested: Boolean) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs[MoneyPreferenceKeys.NOTIFICATION_PERMISSION_REQUESTED] = requested
        }
    }

    val businessBranchSettings: Flow<BusinessBranchSettings> =
        context.moneyPreferencesDataStore.data.map { prefs -> prefs.toBusinessBranchSettings() }

    suspend fun updateBusinessBranchSettings(transform: (BusinessBranchSettings) -> BusinessBranchSettings) {
        context.moneyPreferencesDataStore.edit { prefs ->
            prefs.writeBusinessBranchSettings(transform(prefs.toBusinessBranchSettings()))
        }
    }

    suspend fun setActiveFinanceBranch(branch: FinanceBranch) {
        updateBusinessBranchSettings { it.copy(activeBranch = branch) }
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
                    .put("progress", item.progressFraction.toDouble())
                    .put("overdue", item.isOverdue),
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
                            isOverdue = item.optBoolean("overdue", false),
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

private fun Preferences.toPrivacySettings(): PrivacySettings =
    PrivacySettings(
        hideAmounts = this[MoneyPreferenceKeys.PRIVACY_HIDE_AMOUNTS] ?: false,
        hideOnWidget = this[MoneyPreferenceKeys.PRIVACY_HIDE_ON_WIDGET] ?: false,
    )

private fun MutablePreferences.writePrivacySettings(settings: PrivacySettings) {
    this[MoneyPreferenceKeys.PRIVACY_HIDE_AMOUNTS] = settings.hideAmounts
    this[MoneyPreferenceKeys.PRIVACY_HIDE_ON_WIDGET] = settings.hideOnWidget
}

private fun Preferences.toAutopilotSettings(): AutopilotSettings =
    AutopilotSettings(
        enabled = this[MoneyPreferenceKeys.AUTOPILOT_ENABLED] ?: false,
        dailyAmountCents = this[MoneyPreferenceKeys.AUTOPILOT_DAILY_CENTS] ?: 300L,
        strategy = AutopilotStrategy.fromStored(this[MoneyPreferenceKeys.AUTOPILOT_STRATEGY]),
        reminderHour = this[MoneyPreferenceKeys.AUTOPILOT_REMINDER_HOUR] ?: 20,
        reminderMinute = this[MoneyPreferenceKeys.AUTOPILOT_REMINDER_MINUTE] ?: 0,
        roundPaymentsToTen = this[MoneyPreferenceKeys.AUTOPILOT_ROUND_TO_TEN] ?: false,
    )

private fun MutablePreferences.writeAutopilotSettings(settings: AutopilotSettings) {
    this[MoneyPreferenceKeys.AUTOPILOT_ENABLED] = settings.enabled
    this[MoneyPreferenceKeys.AUTOPILOT_DAILY_CENTS] = settings.dailyAmountCents
    this[MoneyPreferenceKeys.AUTOPILOT_STRATEGY] = settings.strategy.name
    this[MoneyPreferenceKeys.AUTOPILOT_REMINDER_HOUR] = settings.reminderHour
    this[MoneyPreferenceKeys.AUTOPILOT_REMINDER_MINUTE] = settings.reminderMinute
    this[MoneyPreferenceKeys.AUTOPILOT_ROUND_TO_TEN] = settings.roundPaymentsToTen
}

private fun Preferences.toGamificationState(): GamificationState =
    GamificationState(
        streakCount = this[MoneyPreferenceKeys.GAMIFICATION_STREAK] ?: 0,
        lastPaymentIsoDate = this[MoneyPreferenceKeys.GAMIFICATION_LAST_PAYMENT_ISO],
        unlockedBadges = decodeStringSet(this[MoneyPreferenceKeys.GAMIFICATION_UNLOCKED_BADGES_JSON]),
        lastWinMessage = this[MoneyPreferenceKeys.GAMIFICATION_LAST_WIN],
    )

private fun MutablePreferences.writeGamificationState(state: GamificationState) {
    this[MoneyPreferenceKeys.GAMIFICATION_STREAK] = state.streakCount
    state.lastPaymentIsoDate?.let {
        this[MoneyPreferenceKeys.GAMIFICATION_LAST_PAYMENT_ISO] = it
    } ?: remove(MoneyPreferenceKeys.GAMIFICATION_LAST_PAYMENT_ISO)
    this[MoneyPreferenceKeys.GAMIFICATION_UNLOCKED_BADGES_JSON] = encodeStringSet(state.unlockedBadges)
    state.lastWinMessage?.let {
        this[MoneyPreferenceKeys.GAMIFICATION_LAST_WIN] = it
    } ?: remove(MoneyPreferenceKeys.GAMIFICATION_LAST_WIN)
}

private fun encodeStringSet(values: Set<String>): String {
    val array = JSONArray()
    values.forEach { array.put(it) }
    return array.toString()
}

private fun decodeStringSet(raw: String?): Set<String> {
    if (raw.isNullOrBlank()) return emptySet()
    return try {
        val array = JSONArray(raw)
        buildSet {
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }
    } catch (_: Exception) {
        emptySet()
    }
}

private fun Preferences.toGoalPsychologyProfile(): GoalPsychologyProfile {
    val psychotypeRaw = this[MoneyPreferenceKeys.GOAL_PSYCHOTYPE]
    return GoalPsychologyProfile(
        psychotype = psychotypeRaw?.let { FinancialPsychotype.fromStored(it) },
        psychotypeAnswersJson = this[MoneyPreferenceKeys.GOAL_PSYCHOTYPE_ANSWERS_JSON] ?: "[]",
        selectedFearIds = decodeStringSet(this[MoneyPreferenceKeys.GOAL_FEARS_JSON]),
        wordPowerEnabled = this[MoneyPreferenceKeys.GOAL_WORD_POWER] ?: true,
        personalMotivation = this[MoneyPreferenceKeys.GOAL_PERSONAL_MOTIVATION] ?: "",
        visionPhotoPath = this[MoneyPreferenceKeys.GOAL_VISION_PHOTO_PATH],
        paydayDayOfMonth = this[MoneyPreferenceKeys.GOAL_PAYDAY_DAY] ?: 25,
        payoffStrategy = this[MoneyPreferenceKeys.GOAL_PAYOFF_STRATEGY] ?: "AVALANCHE",
        weeklyChecklistJson = this[MoneyPreferenceKeys.GOAL_WEEKLY_CHECKLIST_JSON] ?: "{}",
        activeChallengesJson = this[MoneyPreferenceKeys.GOAL_ACTIVE_CHALLENGES_JSON] ?: "[]",
        goalBadges = decodeStringSet(this[MoneyPreferenceKeys.GOAL_BADGES_JSON]),
        psychotypeTestCompleted = this[MoneyPreferenceKeys.GOAL_PSYCHOTYPE_COMPLETED] ?: false,
    )
}

private fun MutablePreferences.writeGoalPsychologyProfile(profile: GoalPsychologyProfile) {
    profile.psychotype?.let {
        this[MoneyPreferenceKeys.GOAL_PSYCHOTYPE] = it.name
    } ?: remove(MoneyPreferenceKeys.GOAL_PSYCHOTYPE)
    this[MoneyPreferenceKeys.GOAL_PSYCHOTYPE_ANSWERS_JSON] = profile.psychotypeAnswersJson
    this[MoneyPreferenceKeys.GOAL_PSYCHOTYPE_COMPLETED] = profile.psychotypeTestCompleted
    this[MoneyPreferenceKeys.GOAL_FEARS_JSON] = encodeStringSet(profile.selectedFearIds)
    this[MoneyPreferenceKeys.GOAL_WORD_POWER] = profile.wordPowerEnabled
    this[MoneyPreferenceKeys.GOAL_PERSONAL_MOTIVATION] = profile.personalMotivation
    profile.visionPhotoPath?.let {
        this[MoneyPreferenceKeys.GOAL_VISION_PHOTO_PATH] = it
    } ?: remove(MoneyPreferenceKeys.GOAL_VISION_PHOTO_PATH)
    this[MoneyPreferenceKeys.GOAL_PAYDAY_DAY] = profile.paydayDayOfMonth
    this[MoneyPreferenceKeys.GOAL_PAYOFF_STRATEGY] = profile.payoffStrategy
    this[MoneyPreferenceKeys.GOAL_WEEKLY_CHECKLIST_JSON] = profile.weeklyChecklistJson
    this[MoneyPreferenceKeys.GOAL_ACTIVE_CHALLENGES_JSON] = profile.activeChallengesJson
    this[MoneyPreferenceKeys.GOAL_BADGES_JSON] = encodeStringSet(profile.goalBadges)
}

private fun Preferences.toUserProfile(): UserProfile {
    val ageRaw = this[MoneyPreferenceKeys.USER_AGE]
    return UserProfile(
        displayName = this[MoneyPreferenceKeys.USER_DISPLAY_NAME] ?: "",
        gender = UserGender.fromStored(this[MoneyPreferenceKeys.USER_GENDER]),
        age = ageRaw?.takeIf { it > 0 },
        customFear = this[MoneyPreferenceKeys.USER_CUSTOM_FEAR] ?: "",
        goalText = this[MoneyPreferenceKeys.USER_GOAL_TEXT] ?: "",
        emotion = UserEmotion.fromStored(this[MoneyPreferenceKeys.USER_EMOTION]),
        priority = this[MoneyPreferenceKeys.USER_PRIORITY] ?: "",
        onboardingCompleted = this[MoneyPreferenceKeys.ONBOARDING_COMPLETED] ?: false,
    )
}

private fun MutablePreferences.writeUserProfile(profile: UserProfile) {
    this[MoneyPreferenceKeys.USER_DISPLAY_NAME] = profile.displayName
    this[MoneyPreferenceKeys.USER_GENDER] = profile.gender.name
    profile.age?.let { this[MoneyPreferenceKeys.USER_AGE] = it } ?: remove(MoneyPreferenceKeys.USER_AGE)
    this[MoneyPreferenceKeys.USER_CUSTOM_FEAR] = profile.customFear
    this[MoneyPreferenceKeys.USER_GOAL_TEXT] = profile.goalText
    profile.emotion?.let { this[MoneyPreferenceKeys.USER_EMOTION] = it.name }
        ?: remove(MoneyPreferenceKeys.USER_EMOTION)
    this[MoneyPreferenceKeys.USER_PRIORITY] = profile.priority
    this[MoneyPreferenceKeys.ONBOARDING_COMPLETED] = profile.onboardingCompleted
}

private fun Preferences.toCalendarSettings(): CalendarSettings =
    CalendarSettings(
        autoSyncEnabled = this[MoneyPreferenceKeys.CALENDAR_AUTO_SYNC] ?: true,
        remindOneDayBefore = this[MoneyPreferenceKeys.CALENDAR_REMIND_1D] ?: true,
        remindThreeDaysBefore = this[MoneyPreferenceKeys.CALENDAR_REMIND_3D] ?: false,
        remindOnDueDay = this[MoneyPreferenceKeys.CALENDAR_REMIND_DUE] ?: true,
        useDebtsCalendar = this[MoneyPreferenceKeys.CALENDAR_USE_DEBTS_CAL] ?: true,
        debtsCalendarId = this[MoneyPreferenceKeys.CALENDAR_DEBTS_CAL_ID],
        permissionRequested = this[MoneyPreferenceKeys.CALENDAR_PERMISSION_REQUESTED] ?: false,
    )

private fun MutablePreferences.writeCalendarSettings(settings: CalendarSettings) {
    this[MoneyPreferenceKeys.CALENDAR_AUTO_SYNC] = settings.autoSyncEnabled
    this[MoneyPreferenceKeys.CALENDAR_REMIND_1D] = settings.remindOneDayBefore
    this[MoneyPreferenceKeys.CALENDAR_REMIND_3D] = settings.remindThreeDaysBefore
    this[MoneyPreferenceKeys.CALENDAR_REMIND_DUE] = settings.remindOnDueDay
    this[MoneyPreferenceKeys.CALENDAR_USE_DEBTS_CAL] = settings.useDebtsCalendar
    settings.debtsCalendarId?.let {
        this[MoneyPreferenceKeys.CALENDAR_DEBTS_CAL_ID] = it
    } ?: remove(MoneyPreferenceKeys.CALENDAR_DEBTS_CAL_ID)
    this[MoneyPreferenceKeys.CALENDAR_PERMISSION_REQUESTED] = settings.permissionRequested
}

private fun Preferences.toBusinessBranchSettings(): BusinessBranchSettings =
    BusinessBranchSettings(
        activeBranch = runCatching {
            FinanceBranch.valueOf(this[MoneyPreferenceKeys.ACTIVE_FINANCE_BRANCH] ?: FinanceBranch.PERSONAL.name)
        }.getOrDefault(FinanceBranch.PERSONAL),
        businessName = this[MoneyPreferenceKeys.BUSINESS_BRANCH_NAME] ?: "",
    )

private fun MutablePreferences.writeBusinessBranchSettings(settings: BusinessBranchSettings) {
    this[MoneyPreferenceKeys.ACTIVE_FINANCE_BRANCH] = settings.activeBranch.name
    this[MoneyPreferenceKeys.BUSINESS_BRANCH_NAME] = settings.businessName
}
