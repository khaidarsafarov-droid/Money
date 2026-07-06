package com.example.money.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.util.MoneyMath
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(
    private val goalRepository: GoalRepository,
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = goalRepository.observeAllGoals()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val activeGoals: StateFlow<List<SavingsGoal>> = goals
        .map { list -> list.filter { !it.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun addGoal(name: String, targetAmount: Double, currentAmount: Double = 0.0, deadlineMillis: Long? = null) {
        viewModelScope.launch {
            goalRepository.addGoal(
                name = name,
                targetAmountCents = MoneyMath.toCents(targetAmount),
                currentAmountCents = MoneyMath.toCents(currentAmount),
                deadlineMillis = deadlineMillis,
            )
        }
    }

    fun updateGoal(
        goal: SavingsGoal,
        name: String,
        targetAmount: Double,
        currentAmount: Double,
        deadlineMillis: Long?,
    ) {
        viewModelScope.launch {
            goalRepository.updateGoal(
                goal.copy(
                    name = name.trim(),
                    targetAmountCents = MoneyMath.toCents(targetAmount),
                    currentAmountCents = MoneyMath.toCents(currentAmount),
                    deadlineMillis = deadlineMillis,
                ),
            )
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }

    class Factory(
        private val goalRepository: GoalRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GoalViewModel::class.java)) {
                return GoalViewModel(goalRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
