package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    val serverIpFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_IP_KEY] ?: "192.168.1.100"
    }

    val serverPortFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_PORT_KEY] ?: "11434"
    }

    suspend fun saveSettings(ip: String, port: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_IP_KEY] = ip
            preferences[SERVER_PORT_KEY] = port
        }
    }

    companion object {
        private val SERVER_IP_KEY = stringPreferencesKey("server_ip")
        private val SERVER_PORT_KEY = stringPreferencesKey("server_port")
    }
}
