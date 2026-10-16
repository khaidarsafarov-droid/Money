package com.example.money.domain.repository

import com.example.money.domain.model.Client
import com.example.money.domain.model.Debtor
import kotlinx.coroutines.flow.Flow

interface ClientRepository {
    fun observeClients(searchQuery: String): Flow<List<Client>>

    fun observeClient(id: Long): Flow<Client?>

    fun observeDebts(clientId: Long): Flow<List<Debtor>>

    suspend fun save(client: Client): Long

    suspend fun markMeeting(id: Long, meetingAt: Long = System.currentTimeMillis())
}
