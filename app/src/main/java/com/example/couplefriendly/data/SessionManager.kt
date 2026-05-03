package com.example.couplefriendly.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val PARTNER_ID_KEY = stringPreferencesKey("partner_id")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    // Save paired session
    suspend fun savePairedSession(userId: String, partnerId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[PARTNER_ID_KEY] = partnerId
        }
    }

    // Get partnerId if exists
    val partnerId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PARTNER_ID_KEY]
    }

    // Get userId
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    // Check if user has paired session
    val hasPairedSession: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PARTNER_ID_KEY]?.isNotEmpty() == true
    }

    // Clear session on logout
    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
