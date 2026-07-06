package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ServerStatus {
    STOPPED, CONNECTING, RUNNING
}

enum class LogType {
    INFO, SUCCESS, WARNING, ERROR
}

data class LogEntry(
    val time: String,
    val message: String,
    val type: LogType
)

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

    private val _serverStatus = MutableStateFlow(ServerStatus.STOPPED)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private var pollingJob: Job? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
        
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun saveSettings(ip: String, port: String) {
        viewModelScope.launch {
            dataStore.saveSettings(ip, port)
        }
    }
    
    fun addLog(message: String, type: LogType = LogType.INFO) {
        val time = timeFormat.format(Date())
        _logs.value = _logs.value + LogEntry("[$time]", message, type)
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun startServer(ip: String, port: String) {
        saveSettings(ip, port)
        
        if (_serverStatus.value == ServerStatus.RUNNING || _serverStatus.value == ServerStatus.CONNECTING) return
        
        _serverStatus.value = ServerStatus.CONNECTING
        addLog("Backend starting...", LogType.INFO)
        
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500) // Small delay to show Connecting state
            addLog("Connecting to $ip:$port", LogType.INFO)
            
            var isConnected = false
            var retries = 0
            
            while (!isConnected && _serverStatus.value == ServerStatus.CONNECTING) {
                try {
                    val url = "http://$ip:$port/health"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        isConnected = true
                        withContext(Dispatchers.Main) {
                            _serverStatus.value = ServerStatus.RUNNING
                            addLog("Connected to backend successfully", LogType.SUCCESS)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            addLog("Connection failed: HTTP ${response.code}", LogType.WARNING)
                        }
                    }
                    response.close()
                } catch (e: Exception) {
                    retries++
                    withContext(Dispatchers.Main) {
                        addLog("Attempt $retries failed: ${e.localizedMessage}", LogType.ERROR)
                    }
                }
                
                if (!isConnected) {
                    delay(3000)
                }
            }
            
            // Polling while running
            while (isConnected && _serverStatus.value == ServerStatus.RUNNING) {
                delay(2000)
                try {
                    val url = "http://$ip:$port/health"
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        isConnected = false
                        withContext(Dispatchers.Main) {
                            _serverStatus.value = ServerStatus.STOPPED
                            addLog("Disconnected from backend", LogType.ERROR)
                        }
                    } else {
                        // Fetch logs
                        try {
                            val logsUrl = "http://$ip:$port/logs"
                            val logsReq = Request.Builder().url(logsUrl).build()
                            val logsRes = client.newCall(logsReq).execute()
                            if (logsRes.isSuccessful) {
                                val body = logsRes.body?.string() ?: ""
                                val jsonArray = org.json.JSONArray(body)
                                val fetchedLogs = mutableListOf<LogEntry>()
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val time = obj.getString("time")
                                    val typeStr = obj.getString("type")
                                    val message = obj.getString("message")
                                    val logType = when(typeStr) {
                                        "SUCCESS" -> LogType.SUCCESS
                                        "WARNING" -> LogType.WARNING
                                        "ERROR" -> LogType.ERROR
                                        else -> LogType.INFO
                                    }
                                    fetchedLogs.add(LogEntry(time, message, logType))
                                }
                                if (fetchedLogs.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        // Merge logs: keep local logs that are not in fetchedLogs, or just append new fetchedLogs
                                        // For simplicity, just append if time + message is new
                                        val currentLogs = _logs.value.toMutableList()
                                        for (fLog in fetchedLogs) {
                                            if (!currentLogs.any { it.time == fLog.time && it.message == fLog.message }) {
                                                currentLogs.add(fLog)
                                            }
                                        }
                                        if (currentLogs.size > 500) {
                                            _logs.value = currentLogs.takeLast(500)
                                        } else {
                                            _logs.value = currentLogs
                                        }
                                    }
                                }
                            }
                            logsRes.close()
                        } catch (e: Exception) {}
                    }
                    response.close()
                } catch (e: Exception) {
                    isConnected = false
                    withContext(Dispatchers.Main) {
                        _serverStatus.value = ServerStatus.STOPPED
                        addLog("Connection lost: ${e.localizedMessage}", LogType.ERROR)
                    }
                }
            }
        }
    }

    fun stopServer() {
        if (_serverStatus.value == ServerStatus.STOPPED) return
        
        pollingJob?.cancel()
        _serverStatus.value = ServerStatus.STOPPED
        addLog("Server stopped.", LogType.ERROR)
    }
}
