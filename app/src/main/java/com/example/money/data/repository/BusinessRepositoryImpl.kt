package com.example.money.data.repository

import com.example.money.data.local.dao.BusinessAssetDao
import com.example.money.data.local.dao.BusinessCategoryDao
import com.example.money.data.local.dao.BusinessClientDao
import com.example.money.data.local.dao.BusinessInvoiceDao
import com.example.money.data.local.dao.BusinessProjectDao
import com.example.money.data.local.dao.BusinessTransactionDao
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.domain.model.business.BusinessAsset
import com.example.money.domain.model.business.BusinessCategory
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CategoryType
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.repository.BusinessRepository
import com.example.money.domain.util.BusinessFinanceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BusinessRepositoryImpl(
    private val clientDao: BusinessClientDao,
    private val projectDao: BusinessProjectDao,
    private val invoiceDao: BusinessInvoiceDao,
    private val transactionDao: BusinessTransactionDao,
    private val categoryDao: BusinessCategoryDao,
    private val assetDao: BusinessAssetDao,
) : BusinessRepository {

    override fun observeClients(): Flow<List<BusinessClient>> =
        clientDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeProjects(): Flow<List<BusinessProject>> =
        projectDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeProjectsByClient(clientId: Long): Flow<List<BusinessProject>> =
        projectDao.observeByClient(clientId).map { rows -> rows.map { it.toDomain() } }

    override fun observeInvoices(): Flow<List<BusinessInvoice>> =
        invoiceDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeTransactions(): Flow<List<BusinessTransaction>> =
        transactionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<BusinessCategory>> =
        categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeAssets(): Flow<List<BusinessAsset>> =
        assetDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeDashboard(
        monthStartMillis: Long,
        monthEndMillis: Long,
        filters: com.example.money.domain.model.business.BusinessDashboardFilters,
    ): Flow<BusinessDashboardSummary> =
        combine(
            observeTransactions(),
            observeInvoices(),
            observeClients(),
            observeProjects(),
        ) { transactions, invoices, clients, projects ->
            BusinessFinanceEngine.computeDashboard(
                transactions = transactions,
                invoices = invoices,
                clients = clients,
                projects = projects,
                monthStartMillis = monthStartMillis,
                monthEndMillis = monthEndMillis,
                filters = filters,
            )
        }

    override suspend fun addClient(client: BusinessClient): Long =
        clientDao.insert(client.toEntity())

    override suspend fun updateClient(client: BusinessClient) {
        clientDao.upsert(client.toEntity())
    }

    override suspend fun deleteClient(client: BusinessClient) {
        clientDao.delete(client.toEntity())
    }

    override suspend fun addProject(project: BusinessProject): Long =
        projectDao.insert(project.toEntity())

    override suspend fun updateProject(project: BusinessProject) {
        projectDao.upsert(project.toEntity())
    }

    override suspend fun deleteProject(project: BusinessProject) {
        projectDao.delete(project.toEntity())
    }

    override suspend fun addInvoice(invoice: BusinessInvoice): Long =
        invoiceDao.insert(invoice.toEntity())

    override suspend fun updateInvoice(invoice: BusinessInvoice) {
        invoiceDao.upsert(invoice.toEntity())
    }

    override suspend fun deleteInvoice(invoice: BusinessInvoice) {
        invoiceDao.delete(invoice.toEntity())
    }

    override suspend fun markInvoicePaid(invoiceId: Long) {
        val invoice = invoiceDao.getById(invoiceId) ?: return
        invoiceDao.upsert(invoice.copy(status = InvoiceStatus.PAID.name))
        val categoryId = resolveCategoryId("Revenue", CategoryType.INCOME)
        transactionDao.insert(
            BusinessTransaction(
                type = BusinessTransactionType.INCOME,
                amountCents = invoice.amountCents,
                category = "Revenue",
                categoryId = categoryId,
                clientId = invoice.clientId,
                projectId = invoice.projectId,
                invoiceId = invoice.id,
                date = System.currentTimeMillis(),
                note = "Invoice payment",
            ).toEntity(),
        )
    }

    override suspend fun addTransaction(transaction: BusinessTransaction): Long {
        val resolved = transaction.copy(categoryId = transaction.categoryId ?: resolveCategoryId(transaction.category, transaction.type.toCategoryType()))
        return transactionDao.insert(resolved.toEntity())
    }

    override suspend fun updateTransaction(transaction: BusinessTransaction) {
        val resolved = transaction.copy(categoryId = transaction.categoryId ?: resolveCategoryId(transaction.category, transaction.type.toCategoryType()))
        transactionDao.upsert(resolved.toEntity())
    }

    override suspend fun deleteTransaction(transaction: BusinessTransaction) {
        transactionDao.delete(transaction.toEntity())
    }

    override suspend fun addCategory(category: BusinessCategory): Long =
        categoryDao.insert(category.toEntity())

    override suspend fun updateCategory(category: BusinessCategory) {
        categoryDao.upsert(category.toEntity())
    }

    override suspend fun deleteCategory(category: BusinessCategory) {
        categoryDao.delete(category.toEntity())
    }

    override suspend fun addAsset(asset: BusinessAsset): Long =
        assetDao.insert(asset.toEntity())

    override suspend fun updateAsset(asset: BusinessAsset) {
        assetDao.upsert(asset.toEntity())
    }

    override suspend fun deleteAsset(asset: BusinessAsset) {
        assetDao.delete(asset.toEntity())
    }

    override suspend fun refreshInvoiceStatuses(nowMillis: Long) {
        invoiceDao.getAllOnce().forEach { entity ->
            val domain = entity.toDomain()
            val resolved = BusinessFinanceEngine.resolveInvoiceStatus(domain, nowMillis)
            if (resolved != domain.status) {
                invoiceDao.upsert(entity.copy(status = resolved.name))
            }
        }
    }

    private suspend fun resolveCategoryId(name: String, type: CategoryType): Long? {
        val existing = categoryDao.findByNameAndType(name, type.name)
        if (existing != null) return existing.id
        return categoryDao.insert(BusinessCategory(name = name, type = type).toEntity())
    }

    private fun BusinessTransactionType.toCategoryType(): CategoryType = when (this) {
        BusinessTransactionType.INCOME -> CategoryType.INCOME
        BusinessTransactionType.EXPENSE -> CategoryType.EXPENSE
    }
}
