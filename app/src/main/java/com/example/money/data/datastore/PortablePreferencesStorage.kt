package com.example.money.data.datastore

import com.example.money.domain.backup.BackupPreferences

interface PortablePreferencesStorage {
    suspend fun exportPortable(): BackupPreferences

    suspend fun importPortable(preferences: BackupPreferences)
}
