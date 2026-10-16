package com.example.money.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.domain.model.Client
import com.example.money.domain.model.Debtor
import com.example.money.domain.repository.ClientRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.settings.AppMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ClientSort {
    NAME,
    DEBT,
    LAST_MEETING,
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClientsViewModel(
    private val clientRepository: ClientRepository,
    private val debtorRepository: DebtorRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val sort = MutableStateFlow(ClientSort.NAME)

    val query: StateFlow<String> = searchQuery
    val clientSort: StateFlow<ClientSort> = sort

    val clients: StateFlow<List<Client>> = combine(
        searchQuery.flatMapLatest { clientRepository.observeClients(it) },
        sort,
    ) { rows, selectedSort ->
        when (selectedSort) {
            ClientSort.NAME -> rows.sortedBy { it.name.lowercase() }
            ClientSort.DEBT -> rows.sortedByDescending { it.debtCents }
            ClientSort.LAST_MEETING -> rows.sortedWith(
                compareByDescending<Client> { it.lastMeetingAt ?: Long.MIN_VALUE }
                    .thenBy { it.name.lowercase() },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(value: String) {
        searchQuery.value = value
    }

    fun setSort(value: ClientSort) {
        sort.value = value
    }

    fun observeClient(id: Long) = clientRepository.observeClient(id)

    fun observeDebts(clientId: Long) = clientRepository.observeDebts(clientId)

    fun save(client: Client, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            onSaved(clientRepository.save(client))
        }
    }

    fun markMeeting(id: Long) {
        viewModelScope.launch {
            clientRepository.markMeeting(id)
        }
    }

    fun addDebt(client: Client, amountCents: Long, currencyCode: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            debtorRepository.addDebtor(
                Debtor(
                    name = client.name,
                    targetAmountCents = amountCents,
                    currency = currencyCode,
                    dateGiven = now,
                    dateDue = now,
                    phone = client.phone,
                    email = client.email.orEmpty(),
                    clientId = client.id,
                    mode = AppMode.PROFESSIONAL,
                ),
            )
            onDone()
        }
    }

    companion object {
        fun factory(
            clientRepository: ClientRepository,
            debtorRepository: DebtorRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ClientsViewModel(clientRepository, debtorRepository) as T
        }
    }
}
