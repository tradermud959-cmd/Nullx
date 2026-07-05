package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ChatDatabase
import com.example.data.SettingsDataStore
import com.example.network.OllamaClient
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val database = ChatDatabase.getDatabase(context)
        val dataStore = SettingsDataStore(context)
        
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val repository = ChatRepository(
                chatDao = database.chatDao(),
                dataStore = dataStore
            )
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
