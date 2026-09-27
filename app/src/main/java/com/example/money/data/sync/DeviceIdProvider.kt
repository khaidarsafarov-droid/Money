package com.example.money.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.first

private val Context.deviceIdDataStore by preferencesDataStore(name = "sync_device")

class DeviceIdProvider(private val context: Context) {

    suspend fun getDeviceId(): String {
        val current = context.deviceIdDataStore.data.first()[DEVICE_ID_KEY]
        if (current != null) return current
        val generated = UUID.randomUUID().toString()
        var stored = generated
        context.deviceIdDataStore.edit { prefs ->
            val existing = prefs[DEVICE_ID_KEY]
            if (existing != null) {
                stored = existing
            } else {
                prefs[DEVICE_ID_KEY] = generated
            }
        }
        return stored
    }

    suspend fun clear() {
        context.deviceIdDataStore.edit { preferences ->
            preferences.remove(DEVICE_ID_KEY)
        }
    }

    private companion object {
        val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    }
}
