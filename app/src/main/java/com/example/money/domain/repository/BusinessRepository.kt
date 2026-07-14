package com.example.money.domain.repository

import com.example.money.domain.model.business.BusinessAsset
import com.example.money.domain.model.business.BusinessCategory
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import kotlinx.coroutines.flow.Flow

interface BusinessRepository {

    fun observeClients(): Flow<List<BusinessClient>>

    fun observeProjects(): Flow<List<BusinessProject>>

    fun observeProjectsByClient(clientId: Long): Flow<List<BusinessProject>>

    fun observeInvoices(): Flow<List<BusinessInvoice>>

    fun observeTransactions(): Flow<List<BusinessTransaction>>

    fun observeCategories(): Flow<List<BusinessCategory>>

    fun observeAssets(): Flow<List<BusinessAsset>>

    fun observeDashboard(
        monthStartMillis: Long,
        monthEndMillis: Long,
        filters: com.example.money.domain.model.business.BusinessDashboardFilters = com.example.money.domain.model.business.BusinessDashboardFilters(),
    ): Flow<BusinessDashboardSummary>

    suspend fun addClient(client: BusinessClient): Long

    suspend fun updateClient(client: BusinessClient)

    suspend fun deleteClient(client: BusinessClient)

    suspend fun addProject(project: BusinessProject): Long

    suspend fun updateProject(project: BusinessProject)

    suspend fun deleteProject(project: BusinessProject)

    suspend fun addInvoice(invoice: BusinessInvoice): Long

    suspend fun updateInvoice(invoice: BusinessInvoice)

    suspend fun deleteInvoice(invoice: BusinessInvoice)

    suspend fun markInvoicePaid(invoiceId: Long)

    suspend fun addTransaction(transaction: BusinessTransaction): Long

    suspend fun updateTransaction(transaction: BusinessTransaction)

    suspend fun deleteTransaction(transaction: BusinessTransaction)

    suspend fun addCategory(category: BusinessCategory): Long

    suspend fun updateCategory(category: BusinessCategory)

    suspend fun deleteCategory(category: BusinessCategory)

    suspend fun addAsset(asset: BusinessAsset): Long

    suspend fun updateAsset(asset: BusinessAsset)

    suspend fun deleteAsset(asset: BusinessAsset)

    suspend fun refreshInvoiceStatuses(nowMillis: Long = System.currentTimeMillis())
}
