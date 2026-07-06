package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.LogType
import com.example.viewmodel.ServerStatus
import com.example.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val serverIp by settingsViewModel.serverIp.collectAsState()
    val serverPort by settingsViewModel.serverPort.collectAsState()
    val serverStatus by settingsViewModel.serverStatus.collectAsState()
    val logs by settingsViewModel.logs.collectAsState()

    var ipInput by remember { mutableStateOf(serverIp) }
    var portInput by remember { mutableStateOf(serverPort) }

    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(serverIp, serverPort) {
        ipInput = serverIp
        portInput = serverPort
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel Kontrol Backend", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("IP Server") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = Border,
                    focusedLabelColor = PrimaryAccent,
                    cursorColor = PrimaryAccent
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Port Server") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = Border,
                    focusedLabelColor = PrimaryAccent,
                    cursorColor = PrimaryAccent
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        settingsViewModel.startServer(ipInput, portInput)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = serverStatus == ServerStatus.STOPPED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryAccent,
                        disabledContainerColor = PrimaryAccent.copy(alpha = 0.5f)
                    )
                ) {
                    Text("▶ RUN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        settingsViewModel.stopServer()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    enabled = serverStatus != ServerStatus.STOPPED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF333333),
                        disabledContainerColor = Color(0xFF222222)
                    )
                ) {
                    Text("■ STOP", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Status Server", color = SecondaryText, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Crossfade(targetState = serverStatus, label = "status_anim") { status ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (status) {
                        ServerStatus.RUNNING -> Success
                        ServerStatus.STOPPED -> ErrorColor
                        ServerStatus.CONNECTING -> Warning
                    }
                    val statusText = when (status) {
                        ServerStatus.RUNNING -> "🟢 Running"
                        ServerStatus.STOPPED -> "🔴 Stopped"
                        ServerStatus.CONNECTING -> "🟡 Connecting..."
                    }
                    val subText = when (status) {
                        ServerStatus.RUNNING -> "Backend aktif"
                        ServerStatus.STOPPED -> "Backend tidak berjalan"
                        ServerStatus.CONNECTING -> "Menghubungkan ke backend..."
                    }
                    
                    if (status == ServerStatus.CONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = statusColor,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(subText, color = SecondaryText, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Log Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Card)
                    .border(1.dp, Border, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222222))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Log Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row {
                            IconButton(
                                onClick = {
                                    val fullLog = logs.joinToString("\n") { "${it.time} ${it.message}" }
                                    clipboardManager.setText(AnnotatedString(fullLog))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Log", tint = SecondaryText, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { settingsViewModel.clearLogs() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Clear Log", tint = SecondaryText, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        items(logs) { log ->
                            val color = when (log.type) {
                                LogType.INFO -> SecondaryText
                                LogType.SUCCESS -> Success
                                LogType.WARNING -> Warning
                                LogType.ERROR -> ErrorColor
                            }
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "${log.time} ",
                                    color = HintText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = log.message,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
