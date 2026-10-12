package com.example.money.ui.debtors

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.LoanCategory
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.DebtAutopilotEngine
import com.example.money.domain.util.DebtGamificationEngine
import com.example.money.domain.util.DebtMetricsEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.calendar.DebtCalendarSyncService
import com.example.money.notification.DebtReminderCoordinator
import com.example.money.notification.DebtReminderScheduler
import com.example.money.notification.PersonalDebtMessageScheduler
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.CalendarSettings
import com.example.money.settings.GamificationState
import com.example.money.settings.PersonalBranchSettings
import com.example.money.settings.PrivacySettings
import com.example.money.settings.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DebtorsViewModel(
    private val debtorRepository: DebtorRepository,
    private val debtReminderScheduler: DebtReminderScheduler,
    private val preferencesStore: MoneyPreferencesStore,
    private val debtCalendarSyncService: DebtCalendarSyncService,
    private val personalDebtMessageScheduler: PersonalDebtMessageScheduler? = null,
) : ViewModel() {

    private val _advisorMessages = MutableStateFlow<List<AdvisorChatMessage>>(emptyList())
    val advisorMessages: StateFlow<List<AdvisorChatMessage>> = _advisorMessages.asStateFlow()

    private val _celebrationDebtor = MutableStateFlow<Debtor?>(null)
    val celebrationDebtor: StateFlow<Debtor?> = _celebrationDebtor.asStateFlow()

    private val _pendingCalendarClose = MutableStateFlow<Debtor?>(null)
    val pendingCalendarClose: StateFlow<Debtor?> = _pendingCalendarClose.asStateFlow()

    private val _calendarMessage = MutableStateFlow<String?>(null)
    val calendarMessage: StateFlow<String?> = _calendarMessage.asStateFlow()
    private var calendarMessageNonce = 0

    fun dismissCalendarMessage() {
        _calendarMessage.value = null
    }

    private fun postCalendarMessage(key: String) {
        calendarMessageNonce += 1
        _calendarMessage.value = "$key#$calendarMessageNonce"
    }

    fun dismissCalendarClosePrompt() {
        _pendingCalendarClose.value = null
    }

    fun confirmRemoveCalendarEvent(remove: Boolean) {
        val debtor = _pendingCalendarClose.value ?: return
        _pendingCalendarClose.value = null
        viewModelScope.launch {
            if (remove) {
                debtCalendarSyncService.deleteDebtEvent(debtor.calendarEventId, debtor.id)
                persistCalendarState(debtor, eventId = null, failed = false)
            } else {
                val paid = debtorRepository.getDebtorById(debtor.id) ?: debtor
                syncDebtToCalendar(paid, markClosed = true)
            }
        }
    }

    fun dismissCelebration() {
        _celebrationDebtor.value = null
    }

    fun dismissCheckIn() {
        _showCheckIn.value = false
    }

    private val _showCheckIn = MutableStateFlow(false)
    val showCheckIn: StateFlow<Boolean> = _showCheckIn.asStateFlow()

    fun openCheckIn() {
        _showCheckIn.value = true
    }

    val weeklyCheckInSettings: StateFlow<com.example.money.settings.WeeklyCheckInSettings> =
        preferencesStore.weeklyCheckInSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.example.money.settings.WeeklyCheckInSettings(),
        )

    suspend fun updateWeeklyCheckInSettings(
        transform: (com.example.money.settings.WeeklyCheckInSettings) -> com.example.money.settings.WeeklyCheckInSettings,
    ) {
        preferencesStore.updateWeeklyCheckInSettings(transform)
    }

    private val allDebtors = debtorRepository.getAllDebtors()
        .catch { exception ->
            Log.e(TAG, "Failed to load debtors", exception)
            emit(emptyList())
        }

    val allDebtorsList: StateFlow<List<Debtor>> = allDebtors.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val privacySettings: StateFlow<PrivacySettings> = preferencesStore.privacySettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PrivacySettings(),
    )

    val gamificationState: StateFlow<GamificationState> = preferencesStore.gamificationState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GamificationState(),
    )

    val autopilotSettings: StateFlow<AutopilotSettings> = preferencesStore.autopilotSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutopilotSettings(),
    )

    val calendarSettings: StateFlow<CalendarSettings> = preferencesStore.calendarSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarSettings(),
    )

    val personalBranchSettings: StateFlow<PersonalBranchSettings> =
        preferencesStore.personalBranchSettings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PersonalBranchSettings(),
        )

    init {
        viewModelScope.launch {
            var reminderSignatures = emptyMap<Long, DebtReminderCoordinator.ReminderSignature>()
            combine(
                preferencesStore.personalBranchSettings,
                allDebtors,
            ) { settings, debtors ->
                settings to debtors
            }.collect { (settings, debtors) ->
                reminderSignatures = DebtReminderCoordinator.syncAdvanceReminders(
                    scheduler = debtReminderScheduler,
                    settings = settings,
                    previous = reminderSignatures,
                    debtors = debtors,
                )
            }
        }
    }

    suspend fun getDebtorById(id: Long): Debtor? = debtorRepository.getDebtorById(id)

    fun updatePersonalBranchSettings(transform: (PersonalBranchSettings) -> PersonalBranchSettings) {
        viewModelScope.launch {
            preferencesStore.updatePersonalBranchSettings(transform)
            val settings = preferencesStore.personalBranchSettings.first()
            personalDebtMessageScheduler?.syncSchedule(settings)
        }
    }

    val debtMetrics: StateFlow<DashboardDebtMetrics> = allDebtors
        .mapMetrics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardDebtMetrics(0, 0, 0, 0),
        )

    init {
        viewModelScope.launch {
            val json = preferencesStore.advisorHistoryJson.first()
            _advisorMessages.value = decodeAdvisorMessages(json)
        }
    }

    fun askAdvisor(
        promptId: String,
        rawText: String = "",
        totalAccumulatedCents: Long = 0L,
        remainingToGoalCents: Long = 0L,
    ) {
        viewModelScope.launch {
            val debts = allDebtorsList.value
            val strategy = AutopilotStrategy.fromStored(
                preferencesStore.goalPsychologyProfile.first().payoffStrategy,
            )
            val updated = buildAdvisorMessages(
                promptId = promptId,
                debts = debts,
                previous = _advisorMessages.value,
                rawText = rawText,
                totalAccumulatedCents = totalAccumulatedCents,
                remainingToGoalCents = remainingToGoalCents,
                payoffStrategy = strategy,
            )
            _advisorMessages.value = updated
            preferencesStore.writeAdvisorHistoryJson(encodeAdvisorMessages(updated))
        }
    }

    fun askAdvisorFreeText(
        text: String,
        totalAccumulatedCents: Long = 0L,
        remainingToGoalCents: Long = 0L,
    ) {
        askAdvisor(
            promptId = "",
            rawText = text,
            totalAccumulatedCents = totalAccumulatedCents,
            remainingToGoalCents = remainingToGoalCents,
        )
    }

    fun clearAdvisorHistory() {
        viewModelScope.launch {
            _advisorMessages.value = emptyList()
            preferencesStore.writeAdvisorHistoryJson("[]")
        }
    }

    fun addDebtor(
        name: String,
        amount: Double,
        dateGiven: Long,
        dateDue: Long,
        debtType: DebtType,
        affectsBalance: Boolean,
        note: String = "",
        currency: String = "USD",
        loanCategory: LoanCategory = LoanCategory.OTHER,
        interestRateBps: Int = 0,
        contactLookupKey: String? = null,
        contactUri: String? = null,
        contactPhotoUri: String? = null,
        phone: String = "",
        email: String = "",
    ) {
        viewModelScope.launch {
            try {
                val targetCents = MoneyMath.toCents(amount)
                if (targetCents <= 0L) {
                    Log.w(TAG, "Skipped debtor insert: target amount must be > 0 cents")
                    return@launch
                }

                val id = debtorRepository.addDebtor(
                    Debtor(
                        name = name,
                        targetAmountCents = targetCents,
                        currency = currency,
                        debtType = debtType,
                        affectsBalance = affectsBalance,
                        note = note,
                        dateGiven = dateGiven,
                        dateDue = dateDue,
                        loanCategory = loanCategory,
                        interestRateBps = interestRateBps,
                        contactLookupKey = contactLookupKey,
                        contactUri = contactUri,
                        contactPhotoUri = contactPhotoUri,
                        phone = phone.trim(),
                        email = email.trim(),
                    ),
                )
                val created = debtorRepository.getDebtorById(id)
                if (created != null && !created.isPaid) {
                    scheduleDebtReminders(created)
                    syncDebtToCalendar(created)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to add debtor", exception)
            }
        }
    }

    fun updateDebtor(
        debtor: Debtor,
        name: String,
        amount: Double,
        dateGiven: Long,
        dateDue: Long,
        affectsBalance: Boolean,
        note: String = "",
        loanCategory: LoanCategory = debtor.loanCategory,
        interestRateBps: Int = debtor.interestRateBps,
        contactLookupKey: String? = debtor.contactLookupKey,
        contactUri: String? = debtor.contactUri,
        contactPhotoUri: String? = debtor.contactPhotoUri,
        phone: String = debtor.phone,
        email: String = debtor.email,
    ) {
        viewModelScope.launch {
            val targetCents = MoneyMath.toCents(amount)
            val previousAffectsBalance = debtor.affectsBalance
            val updated = debtor.copy(
                name = name,
                targetAmountCents = targetCents,
                dateGiven = dateGiven,
                dateDue = dateDue,
                affectsBalance = affectsBalance,
                note = note,
                loanCategory = loanCategory,
                interestRateBps = interestRateBps,
                contactLookupKey = contactLookupKey,
                contactUri = contactUri,
                contactPhotoUri = contactPhotoUri,
                phone = phone.trim(),
                email = email.trim(),
            )
            debtorRepository.updateDebtor(updated, previousAffectsBalance)
            val latest = debtorRepository.getDebtorById(debtor.id) ?: return@launch
            if (latest.isPaid) {
                debtReminderScheduler.cancel(latest.id)
            } else {
                debtReminderScheduler.cancel(latest.id)
                scheduleDebtReminders(latest)
                syncDebtToCalendar(latest)
            }
        }
    }

    private suspend fun scheduleDebtReminders(debtor: Debtor) {
        val settings = preferencesStore.personalBranchSettings.first()
        if (!settings.debtNotificationsEnabled) {
            debtReminderScheduler.cancel(debtor.id)
            return
        }
        debtReminderScheduler.schedule(debtor)
        debtReminderScheduler.scheduleAdvanceReminders(debtor, settings)
    }

    suspend fun processPaymentSync(debtorId: Long, amount: Double, affectsBalance: Boolean): Boolean {
        val paymentCents = MoneyMath.toCents(amount)
        val success = debtorRepository.processDebtPayment(debtorId, paymentCents, affectsBalance)
        val debtor = debtorRepository.getDebtorById(debtorId)
        if (debtor?.isPaid == true) {
            debtReminderScheduler.cancel(debtorId)
            _celebrationDebtor.value = debtor
            if (!debtor.calendarEventId.isNullOrBlank()) {
                _pendingCalendarClose.value = debtor
            }
        }
        if (success && debtor != null) {
            recordPaymentGamification(debtor)
            if (!debtor.isPaid) {
                syncDebtToCalendar(debtor, force = true)
            }
        }
        return success
    }

    suspend fun applyMicroPaymentSync(): DebtAutopilotEngine.MicroPaymentPlan? {
        val settings = preferencesStore.autopilotSettings.first()
        val plan = DebtAutopilotEngine.planMicroPayment(allDebtorsList.value, settings) ?: return null
        val debtor = debtorRepository.getDebtorById(plan.debtorId) ?: return null
        val affectsBalance = debtor.debtType == DebtType.CREDITOR
        val ok = processPaymentSync(
            debtorId = plan.debtorId,
            amount = MoneyMath.fromCents(plan.amountCents),
            affectsBalance = affectsBalance,
        )
        return if (ok) plan else null
    }

    private suspend fun recordPaymentGamification(debtor: Debtor) {
        val current = preferencesStore.gamificationState.first()
        val latest = debtor.paymentHistory.maxByOrNull { it.paidAt } ?: return
        val next = DebtGamificationEngine.applyPayment(
            previous = current,
            paidAtMillis = latest.paidAt,
            dateDueMillis = debtor.dateDue,
            currentCents = debtor.currentAmountCents,
            targetCents = debtor.targetAmountCents,
            paymentCount = debtor.paymentHistory.size,
        )
        preferencesStore.updateGamificationState { next }
    }

    fun markAsPaid(id: Long) {
        viewModelScope.launch {
            val debtor = debtorRepository.getDebtorById(id) ?: return@launch
            val affectsBalance = debtor.debtType == DebtType.CREDITOR
            debtorRepository.markAsPaid(id, affectsBalance)
            debtReminderScheduler.cancel(id)
            val closed = debtorRepository.getDebtorById(id) ?: debtor.copy(status = DebtStatus.COMPLETED)
            syncDebtToCalendar(closed, markClosed = true, force = true)
            if (!closed.calendarEventId.isNullOrBlank()) {
                _pendingCalendarClose.value = closed
            }
            _celebrationDebtor.value = closed
        }
    }

    fun deleteDebtor(debtor: Debtor) {
        viewModelScope.launch {
            debtCalendarSyncService.deleteDebtEvent(debtor.calendarEventId, debtor.id)
            debtorRepository.deleteDebtor(debtor)
            debtReminderScheduler.cancel(debtor.id)
        }
    }

    fun syncDebtToCalendarManually(debtor: Debtor) {
        viewModelScope.launch {
            syncDebtToCalendar(debtor, force = true)
        }
    }

    fun removeDebtFromCalendarManually(debtor: Debtor) {
        viewModelScope.launch {
            val removed = debtCalendarSyncService.deleteDebtEvent(debtor.calendarEventId, debtor.id)
            persistCalendarState(debtor, eventId = null, failed = !removed)
            postCalendarMessage(if (removed) "calendar_removed" else "calendar_failed")
        }
    }

    suspend fun updateCalendarSettings(transform: (CalendarSettings) -> CalendarSettings) {
        preferencesStore.updateCalendarSettings(transform)
    }

    private suspend fun syncDebtToCalendar(
        debtor: Debtor,
        markClosed: Boolean = false,
        force: Boolean = false,
    ) {
        val settings = preferencesStore.calendarSettings.first()
        if (!force && !settings.autoSyncEnabled) return
        val profile = preferencesStore.userProfile.first()
        val result = debtCalendarSyncService.upsertDebtEvent(
            debtor = debtor,
            settings = settings,
            displayName = profile.resolvedDisplayName(debtCalendarSyncService.defaultDisplayName()),
            markClosed = markClosed,
            force = force,
        )
        persistCalendarState(debtor, result.eventId, result.failed)
        if (result.failed) {
            postCalendarMessage("calendar_failed")
        }
    }

    private suspend fun persistCalendarState(debtor: Debtor, eventId: String?, failed: Boolean) {
        val latest = debtorRepository.getDebtorById(debtor.id) ?: return
        debtorRepository.updateDebtor(
            latest.copy(
                calendarEventId = eventId,
                calendarSyncFailed = failed,
            ),
            latest.affectsBalance,
        )
    }

    fun retryCalendarSyncIfNeeded() {
        viewModelScope.launch {
            retryFailedCalendarSyncs()
        }
    }


    private suspend fun retryFailedCalendarSyncs() {
        val settings = preferencesStore.calendarSettings.first()
        if (!settings.autoSyncEnabled || !debtCalendarSyncService.hasPermissions()) return
        allDebtorsList.value
            .filter { !it.isPaid && (it.calendarSyncFailed || it.calendarEventId.isNullOrBlank()) }
            .forEach { syncDebtToCalendar(it) }
    }

    init {
        viewModelScope.launch {
            retryFailedCalendarSyncs()
        }
    }

    fun scheduleReminder(debtor: Debtor): Boolean =
        debtReminderScheduler.showNow(debtor)

    private companion object {
        const val TAG = "DebtorsViewModel"

        private fun kotlinx.coroutines.flow.Flow<List<Debtor>>.mapMetrics() =
            map { DebtMetricsEngine.computeMetrics(it) }

        private fun encodeAdvisorMessages(messages: List<AdvisorChatMessage>): String {
            val array = JSONArray()
            messages.forEach { message ->
                array.put(
                    JSONObject()
                        .put("role", message.role.name)
                        .put("text", message.text),
                )
            }
            return array.toString()
        }

        private fun decodeAdvisorMessages(raw: String): List<AdvisorChatMessage> {
            if (raw.isBlank()) return emptyList()
            return try {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        add(
                            AdvisorChatMessage(
                                role = AdvisorRole.valueOf(item.getString("role")),
                                text = item.getString("text"),
                            ),
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    class Factory(
        private val debtorRepository: DebtorRepository,
        private val debtReminderScheduler: DebtReminderScheduler,
        private val preferencesStore: MoneyPreferencesStore,
        private val debtCalendarSyncService: DebtCalendarSyncService,
        private val personalDebtMessageScheduler: PersonalDebtMessageScheduler? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DebtorsViewModel::class.java)) {
                return DebtorsViewModel(
                    debtorRepository,
                    debtReminderScheduler,
                    preferencesStore,
                    debtCalendarSyncService,
                    personalDebtMessageScheduler,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
