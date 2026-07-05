package com.example.repository

import com.example.data.ChatDao
import com.example.data.SettingsDataStore
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.network.OllamaClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class ChatRepository(
    private val chatDao: ChatDao,
    private val dataStore: SettingsDataStore
) {
    fun getAllSessions(): Flow<List<ChatSession>> = chatDao.getAllSessions()
    
    fun searchSessions(query: String): Flow<List<ChatSession>> = chatDao.searchSessions(query)
    
    suspend fun insertSession(session: ChatSession) = chatDao.insertSession(session)
    
    suspend fun updateSession(session: ChatSession) = chatDao.updateSession(session)
    
    suspend fun deleteSession(session: ChatSession) = chatDao.deleteSession(session)

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> = chatDao.getMessagesForSession(sessionId)

    suspend fun saveMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteMessagesForSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    fun getStreamResponse(prompt: String): Flow<String> = flow {
        val ip = dataStore.serverIpFlow.first()
        val port = dataStore.serverPortFlow.first()
        val client = OllamaClient(ip, port)
        client.generateStream(prompt).collect { emit(it) }
    }

    suspend fun checkConnection(): Boolean {
        val ip = dataStore.serverIpFlow.first()
        val port = dataStore.serverPortFlow.first()
        return OllamaClient(ip, port).checkConnection()
    }
}
