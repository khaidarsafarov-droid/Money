package com.example.money.data.repository

import com.example.money.data.local.dao.ClientDao
import com.example.money.data.local.entity.ClientEntity
import com.example.money.data.mapper.toDomain
import com.example.money.data.sync.SyncIds
import com.example.money.domain.model.Client
import com.example.money.domain.model.Debtor
import com.example.money.domain.repository.ClientRepository
import com.example.money.settings.AppMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class ClientRepositoryImpl(
    private val clientDao: ClientDao,
    private val appMode: Flow<AppMode>,
) : ClientRepository {

    override fun observeClients(searchQuery: String): Flow<List<Client>> =
        appMode.flatMapLatest { mode ->
            if (mode != AppMode.PROFESSIONAL) {
                flowOf(emptyList())
            } else {
                clientDao.observeList(mode.name, searchPattern(searchQuery)).map { rows ->
                    rows.map { row ->
                        Client(
                            id = row.id,
                            syncId = row.syncId,
                            name = row.name,
                            phone = row.phone,
                            company = row.company,
                            email = row.email,
                            lastMeetingAt = row.lastMeetingAt,
                            note = row.note,
                            createdAt = row.createdAt,
                            debtCents = row.debtCents,
                        )
                    }
                }
            }
        }

    override fun observeClient(id: Long): Flow<Client?> =
        appMode.flatMapLatest { mode ->
            if (mode != AppMode.PROFESSIONAL) {
                flowOf(null)
            } else {
                clientDao.observeList(mode.name, "%").map { rows ->
                    rows.firstOrNull { it.id == id }?.let { row ->
                        Client(
                            id = row.id,
                            syncId = row.syncId,
                            name = row.name,
                            phone = row.phone,
                            company = row.company,
                            email = row.email,
                            lastMeetingAt = row.lastMeetingAt,
                            note = row.note,
                            createdAt = row.createdAt,
                            debtCents = row.debtCents,
                        )
                    }
                }
            }
        }

    override fun observeDebts(clientId: Long): Flow<List<Debtor>> =
        appMode.flatMapLatest { mode ->
            clientDao.observeDebts(clientId, mode.name).map { debts ->
                debts.map { it.toDomain() }
            }
        }

    override suspend fun save(client: Client): Long {
        val now = System.currentTimeMillis()
        val existing = if (client.id == 0L) null else clientDao.getById(client.id, AppMode.PROFESSIONAL.name)
        val entity = ClientEntity(
            id = existing?.id ?: 0L,
            syncId = existing?.syncId ?: client.syncId.ifBlank { SyncIds.newId() },
            name = client.name.trim(),
            phone = client.phone.trim(),
            company = client.company.trim(),
            email = client.email?.trim()?.ifBlank { null },
            lastMeetingAt = client.lastMeetingAt,
            note = client.note.trim(),
            createdAt = existing?.createdAt ?: now,
            syncUpdatedAt = now,
            deleted = false,
            mode = AppMode.PROFESSIONAL.name,
        )
        return if (existing == null) {
            clientDao.insert(entity)
        } else {
            clientDao.update(entity)
            entity.id
        }
    }

    override suspend fun markMeeting(id: Long, meetingAt: Long) {
        val existing = clientDao.getById(id, AppMode.PROFESSIONAL.name) ?: return
        clientDao.update(
            existing.copy(
                lastMeetingAt = meetingAt,
                syncUpdatedAt = meetingAt,
            ),
        )
    }

    private fun searchPattern(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return "%"
        val escaped = trimmed.replace("%", "").replace("_", "")
        return "%$escaped%"
    }
}
