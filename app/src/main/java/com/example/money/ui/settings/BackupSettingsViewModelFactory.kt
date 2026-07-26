package com.example.money.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.money.MoneyApplication

class BackupSettingsViewModelFactory(
    private val app: MoneyApplication,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupSettingsViewModel::class.java)) {
            val container = app.container
            return BackupSettingsViewModel(
                repository = container.backupRepository,
                documentGateway = container.localBackupDocumentGateway,
                authorizer = container.googleDriveAuthorizationManager,
                providerFactory = container.googleDriveStorageProviderFactory,
                stateStore = container.backupStateStore,
                credentialCache = container.backupCredentialCache,
                scheduler = container.automaticBackupScheduler,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
