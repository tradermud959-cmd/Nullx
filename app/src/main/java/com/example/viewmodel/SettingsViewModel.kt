package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val dataStore: SettingsDataStore) : ViewModel() {
    val serverIp: StateFlow<String> = dataStore.serverIpFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "192.168.1.100"
    )

    val serverPort: StateFlow<String> = dataStore.serverPortFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "11434"
    )

    fun saveSettings(ip: String, port: String) {
        viewModelScope.launch {
            dataStore.saveSettings(ip, port)
        }
    }
}
