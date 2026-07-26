package com.example.money.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.backupDeviceStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "backup_device_state",
)

enum class BackupProvider {
    LOCAL_DOCUMENT,
    GOOGLE_DRIVE,
}

enum class BackupErrorCode {
    NETWORK,
    AUTHORIZATION,
    CREDENTIAL,
    STORAGE,
    ENCRYPTION,
    RESTORE,
    UNKNOWN,
}

data class BackupDeviceState(
    val automaticBackupEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val lastSuccessAtEpochMillis: Long? = null,
    val lastSuccessProvider: BackupProvider? = null,
    val lastSuccessFileName: String? = null,
    val lastError: BackupErrorCode? = null,
    val lastErrorAtEpochMillis: Long? = null,
    val reconnectRequired: Boolean = false,
)

interface BackupStateStore {
    val settings: Flow<BackupDeviceState>

    suspend fun update(transform: (BackupDeviceState) -> BackupDeviceState)

    suspend fun clear() {
        update { BackupDeviceState() }
    }
}

suspend fun BackupStateStore.recordBackupSuccess(
    provider: BackupProvider,
    fileName: String,
    atEpochMillis: Long,
) {
    update {
        it.copy(
            lastSuccessAtEpochMillis = atEpochMillis,
            lastSuccessProvider = provider,
            lastSuccessFileName = fileName,
            lastError = null,
            lastErrorAtEpochMillis = null,
            reconnectRequired = false,
        )
    }
}

suspend fun BackupStateStore.recordBackupError(
    error: BackupErrorCode,
    atEpochMillis: Long,
) {
    update {
        it.copy(
            lastError = error,
            lastErrorAtEpochMillis = atEpochMillis,
        )
    }
}

suspend fun BackupStateStore.requireGoogleReconnect(atEpochMillis: Long) {
    update {
        it.copy(
            reconnectRequired = true,
            lastError = BackupErrorCode.AUTHORIZATION,
            lastErrorAtEpochMillis = atEpochMillis,
        )
    }
}

class BackupDeviceStateStore internal constructor(
    private val dataStore: DataStore<Preferences>,
) : BackupStateStore {

    constructor(context: Context) : this(context.backupDeviceStateDataStore)

    override val settings: Flow<BackupDeviceState> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map(BackupDeviceStateKeys::fromPreferences)

    override suspend fun update(transform: (BackupDeviceState) -> BackupDeviceState) {
        dataStore.edit { preferences ->
            BackupDeviceStateKeys.write(preferences, transform(BackupDeviceStateKeys.fromPreferences(preferences)))
        }
    }

    override suspend fun clear() {
        dataStore.edit(MutablePreferences::clear)
    }
}

private object BackupDeviceStateKeys {
    private val automaticEnabled = booleanPreferencesKey("automatic_enabled")
    private val wifiOnly = booleanPreferencesKey("wifi_only")
    private val lastSuccessAt = longPreferencesKey("last_success_at")
    private val lastSuccessProvider = stringPreferencesKey("last_success_provider")
    private val lastSuccessFile = stringPreferencesKey("last_success_file")
    private val lastError = stringPreferencesKey("last_error")
    private val lastErrorAt = longPreferencesKey("last_error_at")
    private val reconnectRequired = booleanPreferencesKey("reconnect_required")

    fun fromPreferences(preferences: Preferences): BackupDeviceState =
        BackupDeviceState(
            automaticBackupEnabled = preferences[automaticEnabled] ?: false,
            wifiOnly = preferences[wifiOnly] ?: true,
            lastSuccessAtEpochMillis = preferences[lastSuccessAt],
            lastSuccessProvider = preferences[lastSuccessProvider]
                ?.let { value -> BackupProvider.entries.firstOrNull { it.name == value } },
            lastSuccessFileName = preferences[lastSuccessFile],
            lastError = preferences[lastError]
                ?.let { value -> BackupErrorCode.entries.firstOrNull { it.name == value } },
            lastErrorAtEpochMillis = preferences[lastErrorAt],
            reconnectRequired = preferences[reconnectRequired] ?: false,
        )

    fun write(preferences: MutablePreferences, state: BackupDeviceState) {
        preferences[automaticEnabled] = state.automaticBackupEnabled
        preferences[wifiOnly] = state.wifiOnly
        preferences[reconnectRequired] = state.reconnectRequired
        preferences.writeNullable(lastSuccessAt, state.lastSuccessAtEpochMillis)
        preferences.writeNullable(lastSuccessProvider, state.lastSuccessProvider?.name)
        preferences.writeNullable(lastSuccessFile, state.lastSuccessFileName)
        preferences.writeNullable(lastError, state.lastError?.name)
        preferences.writeNullable(lastErrorAt, state.lastErrorAtEpochMillis)
    }

    private fun <T> MutablePreferences.writeNullable(
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value == null) {
            remove(key)
        } else {
            this[key] = value
        }
    }
}
