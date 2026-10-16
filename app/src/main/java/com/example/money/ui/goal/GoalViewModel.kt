package com.example.money.ui.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.repository.GoalRepository
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

    val actions: StateFlow<List<GoalAction>> = goalRepository.observeActions()
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

    fun addGoal(
        name: String,
        targetAmountCents: Long,
        deadlineMillis: Long? = null,
        kind: GoalKind = GoalKind.MONEY,
    ) {
        viewModelScope.launch {
            goalRepository.addGoal(
                name = name,
                targetAmountCents = targetAmountCents,
                deadlineMillis = deadlineMillis,
                kind = kind,
            )
        }
    }

    fun updateGoal(
        goal: SavingsGoal,
        name: String,
        targetAmountCents: Long,
        currentAmountCents: Long,
        deadlineMillis: Long?,
        kind: GoalKind = goal.kind,
    ) {
        viewModelScope.launch {
            goalRepository.updateGoal(
                goal.copy(
                    name = name.trim(),
                    targetAmountCents = targetAmountCents,
                    currentAmountCents = currentAmountCents,
                    deadlineMillis = deadlineMillis,
                    kind = kind,
                ),
            )
        }
    }

    fun addAction(goalId: Long, title: String, completed: Boolean = false) {
        viewModelScope.launch {
            goalRepository.addAction(goalId, title, completed)
        }
    }

    fun setActionCompleted(actionId: Long, completed: Boolean) {
        viewModelScope.launch {
            goalRepository.setActionCompleted(actionId, completed)
        }
    }

    fun deleteAction(actionId: Long) {
        viewModelScope.launch {
            goalRepository.deleteAction(actionId)
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
