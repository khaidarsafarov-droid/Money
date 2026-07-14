package com.example.money.domain.usecase

import com.example.money.domain.model.business.AiBusinessInsight
import com.example.money.domain.model.business.BusinessAsset
import com.example.money.domain.model.business.BusinessCategory
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessReportFilters
import com.example.money.domain.model.business.BusinessReportType
import com.example.money.domain.model.business.BusinessReportsBundle
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CashflowForecastPoint
import com.example.money.domain.model.business.CategoryType
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.domain.repository.BusinessRepository
import com.example.money.domain.util.BusinessCsvExporter
import com.example.money.domain.util.BusinessReportsEngine
import com.example.money.domain.util.ai.AiCategorySuggestionService
import com.example.money.domain.util.ai.AiCashflowPredictionService
import com.example.money.domain.util.ai.AiInvoiceRiskService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Universal Business "API" layer — mirrors REST endpoints as offline use cases.
 */
class BusinessUseCases(
    private val repository: BusinessRepository,
) {
    fun observeClients(): Flow<List<BusinessClient>> = repository.observeClients()
    suspend fun createClient(client: BusinessClient): Long = repository.addClient(client)
    suspend fun updateClient(client: BusinessClient) = repository.updateClient(client)
    suspend fun deleteClient(client: BusinessClient) = repository.deleteClient(client)

    fun observeProjects(): Flow<List<BusinessProject>> = repository.observeProjects()
    suspend fun createProject(project: BusinessProject): Long = repository.addProject(project)
    suspend fun updateProject(project: BusinessProject) = repository.updateProject(project)
    suspend fun deleteProject(project: BusinessProject) = repository.deleteProject(project)

    fun observeInvoices(): Flow<List<BusinessInvoice>> = repository.observeInvoices()
    suspend fun createInvoice(invoice: BusinessInvoice): Long = repository.addInvoice(invoice)
    suspend fun updateInvoice(invoice: BusinessInvoice) = repository.updateInvoice(invoice)
    suspend fun deleteInvoice(invoice: BusinessInvoice) = repository.deleteInvoice(invoice)

    fun observeTransactions(): Flow<List<BusinessTransaction>> = repository.observeTransactions()
    suspend fun createTransaction(transaction: BusinessTransaction): Long = repository.addTransaction(transaction)
    suspend fun updateTransaction(transaction: BusinessTransaction) = repository.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: BusinessTransaction) = repository.deleteTransaction(transaction)

    fun observeCategories(): Flow<List<BusinessCategory>> = repository.observeCategories()
    suspend fun createCategory(category: BusinessCategory): Long = repository.addCategory(category)
    suspend fun updateCategory(category: BusinessCategory) = repository.updateCategory(category)
    suspend fun deleteCategory(category: BusinessCategory) = repository.deleteCategory(category)

    fun observeAssets(): Flow<List<BusinessAsset>> = repository.observeAssets()
    suspend fun createAsset(asset: BusinessAsset): Long = repository.addAsset(asset)
    suspend fun updateAsset(asset: BusinessAsset) = repository.updateAsset(asset)
    suspend fun deleteAsset(asset: BusinessAsset) = repository.deleteAsset(asset)

    fun observeReports(filters: BusinessReportFilters): Flow<BusinessReportsBundle> =
        combine(
            repository.observeTransactions(),
            repository.observeInvoices(),
            repository.observeClients(),
            repository.observeProjects(),
        ) { tx, inv, clients, projects ->
            BusinessReportsEngine.generateAll(tx, inv, clients, projects, filters)
        }

    fun exportReportCsv(bundle: BusinessReportsBundle, type: BusinessReportType): String =
        BusinessCsvExporter.export(type, bundle)

    fun predictCashflow(horizonDays: Int = 30): Flow<List<CashflowForecastPoint>> =
        repository.observeTransactions().map { tx ->
            AiCashflowPredictionService.predict(tx, horizonDays)
        }

    fun observeInvoiceRisks(): Flow<Map<Long, InvoiceRiskAssessment>> =
        combine(repository.observeInvoices(), repository.observeClients()) { inv, clients ->
            AiInvoiceRiskService.assessAll(inv, clients)
        }

    suspend fun suggestCategory(note: String, type: BusinessTransactionType, clientId: Long?): String? {
        val tx = repository.observeTransactions().first()
        val custom = repository.observeCategories().first().map { it.name }
        return AiCategorySuggestionService.suggest(note, type, clientId, tx, custom)
    }

    fun insightsFromDashboard(summary: BusinessDashboardSummary): List<AiBusinessInsight> =
        summary.aiInsights
}
