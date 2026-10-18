package com.example.money.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.data.datastore.MoneyPreferencesStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OnboardingDraft(
    val firstName: String = "",
    val lastName: String = "",
)

/** Joins a first and last name into the single stored display name. */
internal fun combinePersonName(firstName: String, lastName: String): String =
    listOf(firstName.trim(), lastName.trim()).filter { it.isNotEmpty() }.joinToString(" ")

/** A required part must be a real name. A blank optional part is accepted. */
internal fun isPersonNamePart(value: String, required: Boolean): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return !required
    return trimmed.length >= 2 && trimmed.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' }
}

class OnboardingViewModel(
    private val preferencesStore: MoneyPreferencesStore,
) : ViewModel() {

    private val _draft = MutableStateFlow(OnboardingDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    fun updateFirstName(name: String) {
        _draft.value = _draft.value.copy(firstName = name)
    }

    fun updateLastName(name: String) {
        _draft.value = _draft.value.copy(lastName = name)
    }

    fun canContinue(): Boolean {
        val draft = _draft.value
        return isPersonNamePart(draft.firstName, required = true) &&
            isPersonNamePart(draft.lastName, required = true)
    }

    /** Saves the name and leaves any previously stored answers untouched. */
    fun complete(onFinished: () -> Unit) {
        if (!canContinue()) return
        val name = combinePersonName(_draft.value.firstName, _draft.value.lastName)
        viewModelScope.launch {
            withContext(NonCancellable) {
                preferencesStore.updateUserProfile { current ->
                    current.copy(
                        displayName = name,
                        onboardingCompleted = true,
                    )
                }
            }
            onFinished()
        }
    }

    /** Finishes first launch without changing a name or stored answers. */
    fun skip(onFinished: () -> Unit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                preferencesStore.updateUserProfile { current ->
                    current.copy(onboardingCompleted = true)
                }
            }
            onFinished()
        }
    }

    class Factory(
        private val preferencesStore: MoneyPreferencesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
