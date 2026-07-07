package com.example.money.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.dao.LuckDiaryDao
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.domain.model.Debtor
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GoalChallengeEngines
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.FinancialPsychotype
import com.example.money.settings.GoalPsychologyProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class WeeklyChecklistUiState(
    val checkedDebts: Boolean = false,
    val madePayment: Boolean = false,
    val savedEmergency: Boolean = false,
    val pleasureOrRestraint: Boolean = false,
    val luckDiaryEntry: Boolean = false,
)

class GoalHubViewModel(
    private val debtorRepository: DebtorRepository,
    private val preferencesStore: MoneyPreferencesStore,
    private val luckDiaryDao: LuckDiaryDao,
) : ViewModel() {

    private val _extraMonthlyCents = MutableStateFlow(0L)
    val extraMonthlyCents: StateFlow<Long> = _extraMonthlyCents.asStateFlow()

    val profile: StateFlow<GoalPsychologyProfile> = preferencesStore.goalPsychologyProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalPsychologyProfile(),
    )

    private val _psychotypeTestPrompt = MutableStateFlow<Boolean?>(null)
    val psychotypeTestPrompt: StateFlow<Boolean?> = _psychotypeTestPrompt.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = preferencesStore.goalPsychologyProfile.first()
            _psychotypeTestPrompt.value = !loaded.psychotypeTestCompleted
        }
    }

    val debtors: StateFlow<List<Debtor>> = debtorRepository.getAllDebtors().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val luckEntries: StateFlow<List<LuckDiaryEntryEntity>> = luckDiaryDao.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val freedomProjection: StateFlow<DebtFreedomEngine.FreedomProjection> = combine(
        debtors,
        _extraMonthlyCents,
    ) { list, extra ->
        GoalPsychologyEngines.freedomSummary(list, extra)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtFreedomEngine.FreedomProjection(
            freedomDateMillis = null,
            monthsRemaining = null,
            totalRemainingCents = 0L,
            monthlyPaymentCents = 0L,
            monthsSavedWithExtra = null,
            hasActiveDebts = false,
        ),
    )

    val payoffPlan: StateFlow<List<GoalPsychologyEngines.PayoffPlanItem>> = combine(
        debtors,
        profile,
    ) { list, prof ->
        val strategy = AutopilotStrategy.fromStored(prof.payoffStrategy)
        GoalPsychologyEngines.buildPayoffPlan(list, strategy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val activeChallenges: StateFlow<List<GoalChallengeEngines.ActiveChallenge>> = combine(
        profile,
        debtors,
    ) { prof, list ->
        val parsed = GoalChallengeEngines.parseChallenges(prof.activeChallengesJson)
        GoalChallengeEngines.syncPaymentChallengeProgress(parsed, list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            debtors
                .distinctUntilChanged()
                .collect { list ->
                    val badges = GoalChallengeEngines.computeGoalBadges(list)
                    val current = profile.value.goalBadges
                    if (badges != current) {
                        preferencesStore.updateGoalPsychologyProfile { it.copy(goalBadges = badges) }
                    }
                    syncChallengeProgressFromPayments(list)
                }
        }
    }

    fun setExtraMonthlyCents(cents: Long) {
        _extraMonthlyCents.value = cents.coerceAtLeast(0L)
    }

    fun submitPsychotypeTest(answers: List<Char>) {
        viewModelScope.launch {
            val type = GoalPsychologyEngines.determinePsychotype(answers)
            val json = JSONArray(answers.map { it.toString() }).toString()
            preferencesStore.updateGoalPsychologyProfile {
                it.copy(
                    psychotype = type,
                    psychotypeAnswersJson = json,
                    psychotypeTestCompleted = true,
                )
            }
            _psychotypeTestPrompt.value = false
        }
    }

    fun updateFears(fearIds: Set<String>) {
        viewModelScope.launch {
            preferencesStore.updateGoalPsychologyProfile { it.copy(selectedFearIds = fearIds) }
        }
    }

    fun updatePersonalMotivation(text: String) {
        viewModelScope.launch {
            preferencesStore.updateGoalPsychologyProfile { it.copy(personalMotivation = text) }
        }
    }

    fun setWordPowerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.updateGoalPsychologyProfile { it.copy(wordPowerEnabled = enabled) }
        }
    }

    fun setPayoffStrategy(strategy: AutopilotStrategy) {
        viewModelScope.launch {
            preferencesStore.updateGoalPsychologyProfile { it.copy(payoffStrategy = strategy.name) }
        }
    }

    fun updateVisionPhotoPath(path: String?) {
        viewModelScope.launch {
            preferencesStore.updateGoalPsychologyProfile { it.copy(visionPhotoPath = path) }
        }
    }

    fun startChallenge(
        templateId: GoalChallengeEngines.ChallengeTemplateId,
        debtId: Long? = null,
        debtName: String? = null,
    ) {
        viewModelScope.launch {
            val current = GoalChallengeEngines.parseChallenges(profile.value.activeChallengesJson)
            val challenge = GoalChallengeEngines.createChallenge(templateId, debtId, debtName)
            val encoded = GoalChallengeEngines.encodeChallenges(current + challenge)
            preferencesStore.updateGoalPsychologyProfile { it.copy(activeChallengesJson = encoded) }
        }
    }

    fun incrementChallengeDay(challengeId: String) {
        viewModelScope.launch {
            val current = GoalChallengeEngines.parseChallenges(profile.value.activeChallengesJson)
            val updated = current.map { challenge ->
                if (challenge.id != challengeId || challenge.completed) {
                    challenge
                } else {
                    val next = challenge.progressDays + 1
                    val target = challenge.targetDays
                    challenge.copy(
                        progressDays = next,
                        completed = target != null && next >= target,
                    )
                }
            }
            preferencesStore.updateGoalPsychologyProfile {
                it.copy(activeChallengesJson = GoalChallengeEngines.encodeChallenges(updated))
            }
        }
    }

    fun removeChallenge(challengeId: String) {
        viewModelScope.launch {
            val current = GoalChallengeEngines.parseChallenges(profile.value.activeChallengesJson)
            val updated = current.filterNot { it.id == challengeId }
            preferencesStore.updateGoalPsychologyProfile {
                it.copy(activeChallengesJson = GoalChallengeEngines.encodeChallenges(updated))
            }
        }
    }

    fun deleteLuckEntry(entry: LuckDiaryEntryEntity) {
        viewModelScope.launch {
            luckDiaryDao.delete(entry)
        }
    }

    fun addLuckEntry(note: String, amountCents: Long?, category: String) {
        viewModelScope.launch {
            luckDiaryDao.insert(
                LuckDiaryEntryEntity(
                    entryDate = System.currentTimeMillis(),
                    note = note,
                    amountCents = amountCents,
                    category = category,
                ),
            )
            updateChecklist { it.copy(luckDiaryEntry = true) }
        }
    }

    fun updateChecklist(transform: (WeeklyChecklistUiState) -> WeeklyChecklistUiState) {
        viewModelScope.launch {
            val current = decodeChecklist(profile.value.weeklyChecklistJson)
            val updated = transform(current)
            preferencesStore.updateGoalPsychologyProfile {
                it.copy(weeklyChecklistJson = encodeChecklist(updated))
            }
        }
    }

    fun monthlyLuckSumCents(now: Long = System.currentTimeMillis()): Long {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val month = calendar.get(java.util.Calendar.MONTH)
        val year = calendar.get(java.util.Calendar.YEAR)
        return luckEntries.value
            .filter { entry ->
                val c = java.util.Calendar.getInstance().apply { timeInMillis = entry.entryDate }
                c.get(java.util.Calendar.MONTH) == month && c.get(java.util.Calendar.YEAR) == year
            }
            .sumOf { it.amountCents ?: 0L }
    }

    fun progressPercent(): Int = GoalPsychologyEngines.totalDebtProgressPercent(debtors.value)

    private fun decodeChecklist(json: String): WeeklyChecklistUiState {
        if (json.isBlank() || json == "{}") return WeeklyChecklistUiState()
        return try {
            val obj = JSONObject(json)
            WeeklyChecklistUiState(
                checkedDebts = obj.optBoolean("checkedDebts"),
                madePayment = obj.optBoolean("madePayment"),
                savedEmergency = obj.optBoolean("savedEmergency"),
                pleasureOrRestraint = obj.optBoolean("pleasureOrRestraint"),
                luckDiaryEntry = obj.optBoolean("luckDiaryEntry"),
            )
        } catch (_: Exception) {
            WeeklyChecklistUiState()
        }
    }

    private fun encodeChecklist(state: WeeklyChecklistUiState): String =
        JSONObject()
            .put("checkedDebts", state.checkedDebts)
            .put("madePayment", state.madePayment)
            .put("savedEmergency", state.savedEmergency)
            .put("pleasureOrRestraint", state.pleasureOrRestraint)
            .put("luckDiaryEntry", state.luckDiaryEntry)
            .toString()

    private suspend fun syncChallengeProgressFromPayments(debtors: List<Debtor>) {
        val current = GoalChallengeEngines.parseChallenges(profile.value.activeChallengesJson)
        val synced = GoalChallengeEngines.syncPaymentChallengeProgress(current, debtors)
        if (synced != current) {
            preferencesStore.updateGoalPsychologyProfile {
                it.copy(activeChallengesJson = GoalChallengeEngines.encodeChallenges(synced))
            }
        }
    }

    class Factory(
        private val debtorRepository: DebtorRepository,
        private val preferencesStore: MoneyPreferencesStore,
        private val luckDiaryDao: LuckDiaryDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GoalHubViewModel::class.java)) {
                return GoalHubViewModel(debtorRepository, preferencesStore, luckDiaryDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
