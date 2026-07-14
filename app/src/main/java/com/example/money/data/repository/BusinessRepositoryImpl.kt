package com.example.money.data.repository

import com.example.money.data.local.dao.BusinessClientDao
import com.example.money.data.local.dao.BusinessInvoiceDao
import com.example.money.data.local.dao.BusinessProjectDao
import com.example.money.data.local.dao.BusinessTransactionDao
import com.example.money.data.mapper.toDomain
import com.example.money.data.mapper.toEntity
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
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

    override fun observeDashboard(monthStartMillis: Long, monthEndMillis: Long): Flow<BusinessDashboardSummary> =
        combine(
            observeTransactions(),
            observeInvoices(),
            observeClients(),
        ) { transactions, invoices, clients ->
            BusinessFinanceEngine.computeDashboard(
                transactions = transactions,
                invoices = invoices,
                clients = clients,
                monthStartMillis = monthStartMillis,
                monthEndMillis = monthEndMillis,
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
        transactionDao.insert(
            BusinessTransaction(
                type = BusinessTransactionType.INCOME,
                amountCents = invoice.amountCents,
                category = "Revenue",
                clientId = invoice.clientId,
                projectId = invoice.projectId,
                invoiceId = invoice.id,
                date = System.currentTimeMillis(),
                note = "Invoice payment",
            ).toEntity(),
        )
    }

    override suspend fun addTransaction(transaction: BusinessTransaction): Long =
        transactionDao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: BusinessTransaction) {
        transactionDao.upsert(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: BusinessTransaction) {
        transactionDao.delete(transaction.toEntity())
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
}
