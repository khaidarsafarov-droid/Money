package com.example.money.ui.debtors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.DebtMetricsEngine
import com.example.money.domain.util.MoneyMath
import com.example.money.notification.DebtReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DebtorsViewModel(
    private val debtorRepository: DebtorRepository,
    private val debtReminderScheduler: DebtReminderScheduler,
) : ViewModel() {

    private val _debtTab = MutableStateFlow(DebtTab.OwesMe)
    val debtTab: StateFlow<DebtTab> = _debtTab.asStateFlow()

    private val _filter = MutableStateFlow(DebtorFilter.ALL)
    val filter: StateFlow<DebtorFilter> = _filter.asStateFlow()

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

    val debtors: StateFlow<List<Debtor>> = combine(
        allDebtors,
        _debtTab,
        _filter,
    ) { debtors, tab, filter ->
        if (tab == DebtTab.Freedom) {
            return@combine emptyList()
        }
        val type = when (tab) {
            DebtTab.OwesMe -> DebtType.CREDITOR
            DebtTab.IOwe -> DebtType.DEBTOR
            DebtTab.Freedom -> DebtType.DEBTOR
        }
        val now = System.currentTimeMillis()
        val byType = debtors.filter { it.debtType == type }
        val filtered = when (filter) {
            DebtorFilter.ALL -> byType
            DebtorFilter.ACTIVE -> byType.filter { !it.isPaid && it.dateDue >= now }
            DebtorFilter.OVERDUE -> byType.filter { !it.isPaid && it.dateDue < now }
            DebtorFilter.PAID -> byType.filter { it.isPaid }
        }
        DebtorSort.sort(filtered, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val debtMetrics: StateFlow<DashboardDebtMetrics> = allDebtors
        .mapMetrics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardDebtMetrics(0, 0, 0, 0),
        )

    private val _extraMonthlyCents = MutableStateFlow(0L)
    val extraMonthlyCents: StateFlow<Long> = _extraMonthlyCents.asStateFlow()

    val freedomProjection: StateFlow<DebtFreedomEngine.FreedomProjection> = combine(
        allDebtors,
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

    fun setExtraMonthlyCents(cents: Long) {
        _extraMonthlyCents.value = cents.coerceAtLeast(0L)
    }

    fun setDebtTab(tab: DebtTab) {
        _debtTab.value = tab
    }

    fun setFilter(filter: DebtorFilter) {
        _filter.value = filter
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
                    ),
                )
                val created = debtorRepository.getDebtorById(id)
                if (created != null && !created.isPaid) {
                    debtReminderScheduler.schedule(created)
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
            )
            debtorRepository.updateDebtor(updated, previousAffectsBalance)
            if (!updated.isPaid) {
                debtReminderScheduler.cancel(debtor.id)
                debtReminderScheduler.schedule(updated)
            }
        }
    }

    suspend fun processPaymentSync(debtorId: Long, amount: Double, affectsBalance: Boolean): Boolean {
        val paymentCents = MoneyMath.toCents(amount)
        val success = debtorRepository.processDebtPayment(debtorId, paymentCents, affectsBalance)
        val debtor = debtorRepository.getDebtorById(debtorId)
        if (debtor?.isPaid == true) {
            debtReminderScheduler.cancel(debtorId)
        }
        return success
    }

    fun markAsPaid(id: Long) {
        viewModelScope.launch {
            debtorRepository.markAsPaid(id)
            debtReminderScheduler.cancel(id)
        }
    }

    fun deleteDebtor(debtor: Debtor) {
        viewModelScope.launch {
            debtorRepository.deleteDebtor(debtor)
            debtReminderScheduler.cancel(debtor.id)
        }
    }

    fun scheduleReminder(debtor: Debtor): Boolean =
        debtReminderScheduler.showNow(debtor)

    private companion object {
        const val TAG = "DebtorsViewModel"

        private fun kotlinx.coroutines.flow.Flow<List<Debtor>>.mapMetrics() =
            map { DebtMetricsEngine.computeMetrics(it) }
    }

    class Factory(
        private val debtorRepository: DebtorRepository,
        private val debtReminderScheduler: DebtReminderScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DebtorsViewModel::class.java)) {
                return DebtorsViewModel(debtorRepository, debtReminderScheduler) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
