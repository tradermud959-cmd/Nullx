package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.repository.ChatRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSession>> = repository.getAllSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<ChatSession>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllSessions()
            } else {
                repository.searchSessions(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId != null) {
                repository.getMessagesForSession(sessionId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _currentStreamText = MutableStateFlow("")
    val currentStreamText = _currentStreamText.asStateFlow()

    private val _connectionStatus = MutableStateFlow<Boolean?>(null)
    val connectionStatus = _connectionStatus.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _connectionStatus.value = repository.checkConnection()
                kotlinx.coroutines.delay(5000)
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun startNewSession() {
        _currentSessionId.value = null
        _isGenerating.value = false
        _currentStreamText.value = ""
    }

    fun openSession(sessionId: String) {
        _currentSessionId.value = sessionId
    }

    fun updateSessionTitle(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val session = sessions.value.find { it.id == sessionId }
            if (session != null) {
                repository.updateSession(session.copy(title = newTitle))
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val session = sessions.value.find { it.id == sessionId }
            if (session != null) {
                repository.deleteSession(session)
                repository.deleteMessagesForSession(sessionId)
                if (_currentSessionId.value == sessionId) {
                    _currentSessionId.value = null
                }
            }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            _connectionStatus.value = repository.checkConnection()
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isGenerating.value) return

        viewModelScope.launch {
            var sessionId = _currentSessionId.value
            if (sessionId == null) {
                val newSession = ChatSession(title = content.take(30) + if (content.length > 30) "..." else "")
                repository.insertSession(newSession)
                sessionId = newSession.id
                _currentSessionId.value = sessionId
            }

            // Save user message
            val userMsg = ChatMessage(sessionId = sessionId, content = content, isUser = true)
            repository.saveMessage(userMsg)
            
            // Update session preview
            updateSessionPreview(sessionId, content)

            _isGenerating.value = true
            _currentStreamText.value = ""

            var fullResponse = ""
            repository.getStreamResponse(content)
                .catch { e ->
                    fullResponse = "Error: ${e.message}"
                }
                .collect { chunk ->
                    fullResponse += chunk
                    _currentStreamText.value = fullResponse
                    
                    // Simulate natural reading delay as requested
                    val delayTime = when {
                        chunk.contains(".") || chunk.contains(",") -> 150L
                        else -> 50L
                    }
                    delay(delayTime)
                }

            // Save AI message
            if (fullResponse.isNotBlank()) {
                val aiMsg = ChatMessage(sessionId = sessionId, content = fullResponse, isUser = false)
                repository.saveMessage(aiMsg)
                updateSessionPreview(sessionId, fullResponse)
            }
            _currentStreamText.value = ""
            _isGenerating.value = false
        }
    }

    private suspend fun updateSessionPreview(sessionId: String, preview: String) {
        val session = sessions.value.find { it.id == sessionId }
        if (session != null) {
            repository.updateSession(session.copy(
                lastMessagePreview = preview.take(50).replace("\n", " ") + if (preview.length > 50) "..." else "",
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _currentSessionId.value = null
        }
    }
}
