package com.example.money.ui.debtors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.Debtor
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.model.DebtType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/** List presentation state — tab, filter, search, and SQL-backed debtor list. */
class DebtListViewModel(
    private val debtorRepository: DebtorRepository,
) : ViewModel() {

    private val _debtTab = MutableStateFlow(DebtTab.OwesMe)
    val debtTab: StateFlow<DebtTab> = _debtTab.asStateFlow()

    private val _filter = MutableStateFlow(DebtorFilter.ALL)
    val filter: StateFlow<DebtorFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _extraMonthlyCents = MutableStateFlow(0L)
    val extraMonthlyCents: StateFlow<Long> = _extraMonthlyCents.asStateFlow()

    val allDebtorsList: StateFlow<List<Debtor>> = debtorRepository.getAllDebtors().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val debtors: StateFlow<List<Debtor>> = combine(_debtTab, _filter, _searchQuery) { tab, filter, query ->
        Triple(tab, filter, query)
    }.flatMapLatest { (tab, filter, query) ->
        debtorRepository.observeFilteredDebtors(tab, filter, query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val freedomProjection: StateFlow<DebtFreedomEngine.FreedomProjection> = combine(
        allDebtorsList,
        _extraMonthlyCents,
    ) { debtors, extra ->
        DebtFreedomEngine.calculateFreedomProjection(
            debts = debtors,
            debtType = DebtType.DEBTOR,
            extraMonthlyCents = extra,
        )
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

    fun setDebtTab(tab: DebtTab) {
        _debtTab.value = tab
    }

    fun setFilter(filter: DebtorFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setExtraMonthlyCents(cents: Long) {
        _extraMonthlyCents.value = cents.coerceAtLeast(0L)
    }

    class Factory(
        private val debtorRepository: DebtorRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DebtListViewModel::class.java)) {
                return DebtListViewModel(debtorRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
