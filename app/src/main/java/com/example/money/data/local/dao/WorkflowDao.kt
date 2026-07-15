package com.example.money.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.money.data.local.entity.workflow.WorkflowBillEntity
import com.example.money.data.local.entity.workflow.WorkflowCategoryEntity
import com.example.money.data.local.entity.workflow.WorkflowDebtEntity
import com.example.money.data.local.entity.workflow.WorkflowParticipantEntity
import com.example.money.data.local.entity.workflow.WorkflowProcessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkflowCategoryDao {
    @Query("SELECT * FROM business_workflow_category ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<WorkflowCategoryEntity>>

    @Query("SELECT * FROM business_workflow_category WHERE id = :id")
    suspend fun getById(id: Long): WorkflowCategoryEntity?

    @Insert
    suspend fun insert(entity: WorkflowCategoryEntity): Long
}

@Dao
interface WorkflowProcessDao {
    @Query("SELECT * FROM business_workflow_process WHERE category_id = :categoryId ORDER BY created_at DESC")
    fun observeByCategory(categoryId: Long): Flow<List<WorkflowProcessEntity>>

    @Query("SELECT * FROM business_workflow_process WHERE id = :id")
    suspend fun getById(id: Long): WorkflowProcessEntity?

    @Insert
    suspend fun insert(entity: WorkflowProcessEntity): Long

    @Update
    suspend fun update(entity: WorkflowProcessEntity)

    @Query("DELETE FROM business_workflow_process WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface WorkflowBillDao {
    @Query("SELECT * FROM business_workflow_bill WHERE process_id = :processId ORDER BY created_at DESC")
    fun observeByProcess(processId: Long): Flow<List<WorkflowBillEntity>>

    @Query("SELECT * FROM business_workflow_bill WHERE id = :id")
    suspend fun getById(id: Long): WorkflowBillEntity?

    @Insert
    suspend fun insert(entity: WorkflowBillEntity): Long

    @Update
    suspend fun update(entity: WorkflowBillEntity)

    @Query("DELETE FROM business_workflow_bill WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface WorkflowParticipantDao {
    @Query("SELECT * FROM business_workflow_participant ORDER BY name ASC")
    fun observeAll(): Flow<List<WorkflowParticipantEntity>>

    @Query("SELECT * FROM business_workflow_participant WHERE id = :id")
    suspend fun getById(id: Long): WorkflowParticipantEntity?

    @Query("SELECT * FROM business_workflow_participant WHERE contact_lookup_key = :lookupKey LIMIT 1")
    suspend fun findByLookupKey(lookupKey: String): WorkflowParticipantEntity?

    @Insert
    suspend fun insert(entity: WorkflowParticipantEntity): Long

    @Update
    suspend fun update(entity: WorkflowParticipantEntity)
}

@Dao
interface WorkflowDebtDao {
    @Query(
        """
        SELECT d.* FROM business_workflow_debt d
        WHERE d.bill_id = :billId
        ORDER BY d.created_at ASC
        """,
    )
    fun observeByBill(billId: Long): Flow<List<WorkflowDebtEntity>>

    @Query("SELECT * FROM business_workflow_debt WHERE id = :id")
    suspend fun getById(id: Long): WorkflowDebtEntity?

    @Insert
    suspend fun insert(entity: WorkflowDebtEntity): Long

    @Update
    suspend fun update(entity: WorkflowDebtEntity)

    @Transaction
    suspend fun replaceDebtsForBill(billId: Long, debts: List<WorkflowDebtEntity>) {
        deleteByBill(billId)
        debts.forEach { insert(it) }
    }

    @Query("DELETE FROM business_workflow_debt WHERE bill_id = :billId")
    suspend fun deleteByBill(billId: Long)
}
