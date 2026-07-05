package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.clickable
import com.example.ui.theme.Border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.Success
import com.example.ui.theme.AIBubble
import com.example.ui.theme.HintText
import com.example.ui.theme.UserBubble
import com.example.viewmodel.ChatViewModel
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentStreamText by viewModel.currentStreamText.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isChatStarted by remember { mutableStateOf(messages.isNotEmpty()) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedFileSize by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            it.let { documentUri ->
                context.contentResolver.query(documentUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        selectedFileName = cursor.getString(nameIndex)
                        val sizeBytes = cursor.getLong(sizeIndex)
                        selectedFileSize = "${sizeBytes / 1024} KB"
                    }
                }
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Fitur upload file akan tersedia pada versi berikutnya.")
            }
        }
    }

    var isListening by remember { mutableStateOf(false) }
    var showSpeechNotSupportedDialog by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    textInput += (if (textInput.isNotEmpty()) " " else "") + matches[0]
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    LaunchedEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(recognitionListener)
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            }
            speechRecognizer?.startListening(intent)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Izin mikrofon diperlukan.")
            }
        }
    }

    fun handleMicClick() {
        if (speechRecognizer == null) {
            showSpeechNotSupportedDialog = true
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (isListening) {
                speechRecognizer.stopListening()
            } else {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                speechRecognizer.startListening(intent)
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    
    // Update chat started state if messages exist
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            isChatStarted = true
        }
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val listState = rememberLazyListState()
    
    // Auto scroll when new messages arrive or streaming
    LaunchedEffect(messages.size, currentStreamText) {
        if (messages.isNotEmpty() || currentStreamText.isNotEmpty()) {
            listState.animateScrollToItem(if (currentStreamText.isNotEmpty()) messages.size else messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header / Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Menu Icon + Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .clickable { 
                                focusManager.clearFocus()
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(100)
                                    showBottomSheet = true 
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = HintText)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NullX AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .background(Border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "v1.0.0", fontSize = 10.sp, color = HintText)
                    }
                }

                // Right side: Status Pill
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                        .border(1.dp, Border, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when (connectionStatus) {
                        true -> Success
                        false -> MaterialTheme.colorScheme.error
                        null -> HintText
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (connectionStatus == true) "Connected" else "Disconnected", fontSize = 11.sp, color = HintText, fontWeight = FontWeight.Medium)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Border)
            )

            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                androidx.compose.animation.Crossfade(
                    targetState = isChatStarted || textInput.isNotEmpty(),
                    label = "chat_crossfade"
                ) { showChat ->
                    if (!showChat) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Central Logo
                                Box(modifier = Modifier.padding(16.dp)) {
                                    // Outer border
                                    Box(
                                        modifier = Modifier
                                            .size(112.dp)
                                            .align(Alignment.Center)
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                                    )
                                    // Inner Image
                                    // Ganti logo aplikasi di:
                                    // app/src/main/res/drawable/nullx_logo.png
                                    Image(
                                        painter = painterResource(id = R.drawable.nullx_logo),
                                        contentDescription = "NullX Logo",
                                        modifier = Modifier
                                            .size(96.dp)
                                            .align(Alignment.Center)
                                            .clip(RoundedCornerShape(28.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Selamat datang.",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ada yang bisa saya bantu hari ini?",
                                    color = HintText,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SuggestionPill("Tulis kode Kotlin") {
                                        viewModel.sendMessage("Tulis kode Kotlin")
                                        isChatStarted = true
                                    }
                                    SuggestionPill("Jelaskan Ollama") {
                                        viewModel.sendMessage("Jelaskan Ollama")
                                        isChatStarted = true
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                            items(messages, key = { it.id ?: it.hashCode() }) { message ->
                                AnimatedFadeIn {
                                    ChatBubble(message = message)
                                }
                            }
                            if (isGenerating && currentStreamText.isNotEmpty()) {
                                item {
                                    ChatBubble(
                                        message = ChatMessage(sessionId = "", content = currentStreamText, isUser = false),
                                        isStreaming = true
                                    )
                                }
                            } else if (isGenerating) {
                                item {
                                    AnimatedFadeIn {
                                        TypingIndicator()
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }

            // Interaction Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, Border) // Top border
                    .padding(16.dp)
            ) {
                // Utility Icons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        Icons.Rounded.AttachFile, 
                        contentDescription = "Attach", 
                        tint = HintText,
                        modifier = Modifier.clickable { 
                            filePickerLauncher.launch(arrayOf("image/*", "application/pdf", "text/plain", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        }
                    )
                    Icon(
                        Icons.Rounded.Mic, 
                        contentDescription = "Mic", 
                        tint = if (isListening) MaterialTheme.colorScheme.primary else HintText,
                        modifier = Modifier.clickable { handleMicClick() }
                    )
                }
                
                if (selectedFileName != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFileName ?: "",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = selectedFileSize ?: "",
                                fontSize = 12.sp,
                                color = HintText
                            )
                        }
                        IconButton(
                            onClick = { 
                                selectedFileUri = null
                                selectedFileName = null
                                selectedFileSize = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = HintText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Area
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = textInput,
                        onValueChange = { 
                            textInput = it 
                            if (!isChatStarted && it.isNotEmpty()) {
                                isChatStarted = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Border, RoundedCornerShape(24.dp)),
                        placeholder = { Text("Tanya apa saja...", color = HintText, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                selectedFileUri = null
                                selectedFileName = null
                                selectedFileSize = null
                            }
                        }),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank() && !isGenerating) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                selectedFileUri = null
                                selectedFileName = null
                                selectedFileSize = null
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(18.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(48.dp)
                        .height(4.dp)
                        .background(Border, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.height(24.dp))
                MenuButton("Chat Baru", "➕", isPrimary = true) {
                    showBottomSheet = false
                    viewModel.startNewSession()
                    isChatStarted = false
                }
                MenuButton("Riwayat Obrolan", "📜", isPrimary = false) {
                    showBottomSheet = false
                    onNavigateToHistory()
                }
                MenuButton("Pengaturan", "⚙️", isPrimary = false) {
                    showBottomSheet = false
                    onNavigateToSettings()
                }
                MenuButton("Tentang", "ℹ️", isPrimary = false) {
                    showBottomSheet = false
                    onNavigateToAbout()
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSpeechNotSupportedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeechNotSupportedDialog = false },
            title = { Text("Tidak Didukung") },
            text = { Text("Perangkat ini tidak mendukung Voice Recognition.") },
            confirmButton = {
                TextButton(onClick = { showSpeechNotSupportedDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun AnimatedFadeIn(content: @Composable () -> Unit) {
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(300)
        )
    }
    Box(modifier = Modifier.alpha(alpha.value)) {
        content()
    }
}

@Composable
fun MenuButton(text: String, icon: String, isPrimary: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPrimary) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Border),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatBubble(message: ChatMessage, isStreaming: Boolean = false) {
    val isUser = message.isUser
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            AiAvatar()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(if (isUser) UserBubble else AIBubble)
                .combinedClickable(
                    onLongClick = {
                        if (!isStreaming) {
                            showMenu = true
                        }
                    },
                    onClick = {}
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = message.content + if (isStreaming) " █" else "",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                if (!isUser && !isStreaming) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy",
                            tint = HintText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(message.content))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }

            androidx.compose.material3.DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Copy text") },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        AiAvatar()
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(AIBubble)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("...", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AiAvatar() {
    Image(
        painter = painterResource(id = R.drawable.nullx_ai_mascot_1783220072642),
        contentDescription = "AI Avatar",
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
    )
}

@Composable
fun SuggestionPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(com.example.ui.theme.Card)
            .border(1.dp, Border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = HintText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
