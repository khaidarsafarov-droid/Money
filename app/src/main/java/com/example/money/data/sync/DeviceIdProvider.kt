package com.example.money.data.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID

private val Context.deviceIdDataStore by preferencesDataStore(name = "sync_device")

class DeviceIdProvider(private val context: Context) {

    suspend fun getDeviceId(): String {
        var resolved: String? = null
        context.deviceIdDataStore.edit { prefs ->
            val existing = prefs[DEVICE_ID_KEY]
            if (existing != null) {
                resolved = existing
            } else {
                val generated = UUID.randomUUID().toString()
                prefs[DEVICE_ID_KEY] = generated
                resolved = generated
            }
        }
        return resolved!!
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
