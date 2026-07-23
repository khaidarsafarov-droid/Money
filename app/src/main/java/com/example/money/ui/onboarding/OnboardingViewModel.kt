package com.example.money.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.DebtType
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.settings.FinancialPsychotype
import com.example.money.settings.UserEmotion
import com.example.money.settings.UserGender
import com.example.money.settings.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

data class OnboardingDraft(
    val displayName: String = "",
    val gender: UserGender = UserGender.UNSPECIFIED,
    val ageText: String = "",
    val selectedFearIds: Set<String> = emptySet(),
    val customFear: String = "",
    val goalText: String = "",
    val emotion: UserEmotion? = null,
    val priority: String = "",
    val psychotypeAnswers: List<Char?> = List(5) { null },
    val psychotypeResult: FinancialPsychotype? = null,
    val psychotypeQuestionIndex: Int = 0,
    val showPsychotypeResult: Boolean = false,
)

class OnboardingViewModel(
    private val preferencesStore: MoneyPreferencesStore,
    private val debtorRepository: DebtorRepository,
) : ViewModel() {

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    val activeDebtCount: StateFlow<Int> = debtorRepository.getAllDebtors()
        .map { debtors -> debtors.count { it.debtType == DebtType.DEBTOR && !it.isPaid } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    fun goToStep(index: Int) {
        _step.value = index.coerceIn(0, TOTAL_STEPS - 1)
    }

    fun nextStep() {
        if (_step.value < TOTAL_STEPS - 1) {
            _step.value += 1
        }
    }

    fun updateName(name: String) {
        _draft.value = _draft.value.copy(displayName = name)
    }

    fun updateGender(gender: UserGender) {
        _draft.value = _draft.value.copy(gender = gender)
    }

    fun updateAgeText(text: String) {
        _draft.value = _draft.value.copy(ageText = text.filter { it.isDigit() }.take(2))
    }

    fun updateFears(fearIds: Set<String>) {
        _draft.value = _draft.value.copy(selectedFearIds = fearIds)
    }

    fun updateCustomFear(text: String) {
        _draft.value = _draft.value.copy(customFear = text)
    }

    fun updateGoalText(text: String) {
        _draft.value = _draft.value.copy(goalText = text)
    }

    fun updateEmotion(emotion: UserEmotion) {
        _draft.value = _draft.value.copy(emotion = emotion)
    }

    fun updatePriority(text: String) {
        _draft.value = _draft.value.copy(priority = text)
    }

    fun selectPsychotypeAnswer(questionIndex: Int, answer: Char) {
        val updated = _draft.value.psychotypeAnswers.toMutableList()
        updated[questionIndex] = answer
        _draft.value = _draft.value.copy(psychotypeAnswers = updated)
    }

    fun nextPsychotypeQuestion() {
        val current = _draft.value
        val answer = current.psychotypeAnswers.getOrNull(current.psychotypeQuestionIndex)
        if (answer == null) return
        if (current.psychotypeQuestionIndex < PSYCHOTYPE_QUESTION_COUNT - 1) {
            _draft.value = current.copy(psychotypeQuestionIndex = current.psychotypeQuestionIndex + 1)
        } else {
            val resolved = current.psychotypeAnswers.mapNotNull { it }
            if (resolved.size < PSYCHOTYPE_QUESTION_COUNT) return
            val type = GoalPsychologyEngines.determinePsychotype(resolved)
            _draft.value = current.copy(psychotypeResult = type, showPsychotypeResult = true)
        }
    }

    fun hasPsychotypeAnswerForCurrentQuestion(): Boolean =
        _draft.value.psychotypeAnswers.getOrNull(_draft.value.psychotypeQuestionIndex) != null

    fun isValidName(): Boolean {
        val trimmed = _draft.value.displayName.trim()
        return trimmed.length >= 2 && trimmed.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' }
    }

    fun parsedAge(): Int? {
        val value = _draft.value.ageText.toIntOrNull() ?: return null
        return value.takeIf { it in 18..99 }
    }

    fun isValidAge(): Boolean = _draft.value.ageText.isBlank() || parsedAge() != null

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            val draft = _draft.value
            val name = draft.displayName.trim().ifBlank { UserProfile.DEFAULT_DISPLAY_NAME }
            val goal = draft.goalText.trim().ifBlank { UserProfile.DEFAULT_GOAL_TEXT }
            val motivation = buildString {
                append(goal)
                if (draft.priority.isNotBlank()) {
                    append(" · ")
                    append(draft.priority.trim())
                }
            }
            val psychotype = draft.psychotypeResult ?: FinancialPsychotype.BALANCED
            val answersJson = JSONArray(draft.psychotypeAnswers.mapNotNull { it?.toString() }).toString()

            preferencesStore.updateUserProfile {
                UserProfile(
                    displayName = name,
                    gender = draft.gender,
                    age = parsedAge(),
                    customFear = draft.customFear.trim(),
                    goalText = goal,
                    emotion = draft.emotion,
                    priority = draft.priority.trim(),
                    onboardingCompleted = true,
                )
            }

            preferencesStore.updateGoalPsychologyProfile {
                it.copy(
                    psychotype = psychotype,
                    psychotypeAnswersJson = answersJson,
                    psychotypeTestCompleted = true,
                    selectedFearIds = draft.selectedFearIds,
                    personalMotivation = motivation,
                )
            }

            onFinished()
        }
    }

    companion object {
        const val TOTAL_STEPS = 6
        const val PSYCHOTYPE_QUESTION_COUNT = 5
    }

    class Factory(
        private val preferencesStore: MoneyPreferencesStore,
        private val debtorRepository: DebtorRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(preferencesStore, debtorRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
