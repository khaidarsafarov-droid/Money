package com.example.money.business.push

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.fcmTokenDataStore by preferencesDataStore(name = "business_fcm_tokens")

object FcmTokenStore {

    private val FCM_TOKEN = stringPreferencesKey("fcm_token")

    fun observeToken(context: Context): Flow<String?> =
        context.fcmTokenDataStore.data.map { prefs -> prefs[FCM_TOKEN] }

    suspend fun getToken(context: Context): String? = observeToken(context).first()

    suspend fun saveToken(context: Context, token: String) {
        context.fcmTokenDataStore.edit { prefs ->
            prefs[FCM_TOKEN] = token
        }
    }

    suspend fun clearToken(context: Context) {
        context.fcmTokenDataStore.edit { prefs ->
            prefs.remove(FCM_TOKEN)
        }
    }
}
