package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.money.data.local.entity.ClientEntity
import com.example.money.data.local.entity.DebtorEntity
import kotlinx.coroutines.flow.Flow

data class ClientListRow(
    val id: Long,
    val syncId: String,
    val name: String,
    val phone: String,
    val company: String,
    val email: String?,
    val lastMeetingAt: Long?,
    val note: String,
    val createdAt: Long,
    val debtCents: Long,
)

@Dao
interface ClientDao {

    @Query(
        """
        SELECT client.id AS id,
               client.sync_id AS syncId,
               client.name AS name,
               client.phone AS phone,
               client.company AS company,
               client.email AS email,
               client.last_meeting_at AS lastMeetingAt,
               client.note AS note,
               client.created_at AS createdAt,
               COALESCE(SUM(
                   CASE
                       WHEN debtor.status != 'COMPLETED'
                       THEN debtor.target_amount_cents - debtor.current_amount_cents
                       ELSE 0
                   END
               ), 0) AS debtCents
        FROM client
        LEFT JOIN debtor ON debtor.client_id = client.id AND debtor.mode = client.mode
        WHERE client.mode = :mode AND client.deleted = 0
          AND (
            :searchPattern = '%'
            OR LOWER(client.name) LIKE LOWER(:searchPattern)
            OR LOWER(client.company) LIKE LOWER(:searchPattern)
            OR client.phone LIKE :searchPattern
            OR LOWER(COALESCE(client.email, '')) LIKE LOWER(:searchPattern)
          )
        GROUP BY client.id
        """,
    )
    fun observeList(mode: String, searchPattern: String): Flow<List<ClientListRow>>

    @Query("SELECT * FROM client WHERE id = :id AND mode = :mode LIMIT 1")
    suspend fun getById(id: Long, mode: String): ClientEntity?

    @Query("SELECT * FROM client WHERE sync_id = :syncId LIMIT 1")
    suspend fun getBySyncId(syncId: String): ClientEntity?

    @Query("SELECT * FROM client")
    suspend fun getAllOnce(): List<ClientEntity>

    @Query("SELECT * FROM debtor WHERE client_id = :clientId AND mode = :mode")
    fun observeDebts(clientId: Long, mode: String): Flow<List<DebtorEntity>>

    @Insert
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: ClientEntity)

    @Query("DELETE FROM client")
    suspend fun clearAll()

    @Query("DELETE FROM client WHERE sync_id = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
