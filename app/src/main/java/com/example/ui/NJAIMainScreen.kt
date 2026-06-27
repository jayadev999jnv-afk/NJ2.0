package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NJAIMainScreen(viewModel: NJAIViewModel = viewModel()) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isListening by viewModel.isListening
    val isSpeaking by viewModel.isSpeaking
    val amplitude by viewModel.amplitude
    val dbLevel by viewModel.dbLevel
    val peakAmplitude by viewModel.peakAmplitude
    val visualizerTheme by viewModel.visualizerTheme
    val currentMode by viewModel.currentMode
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val majorPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val majorPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    LaunchedEffect(Unit) {
        val missing = majorPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            majorPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NJ AI",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.visualizerTheme.value = if (visualizerTheme == VisualizerTheme.Professional) {
                            VisualizerTheme.Energetic
                        } else {
                            VisualizerTheme.Professional
                        }
                    }) {
                        Icon(
                            if (visualizerTheme == VisualizerTheme.Professional) Icons.Default.BusinessCenter else Icons.Default.FlashOn,
                            contentDescription = "Switch Theme",
                            tint = if (visualizerTheme == VisualizerTheme.Energetic) Color(0xFFFF00FF) else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF001524) // Dark navy background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Chat History (Subtle)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }

            // Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NJModeChip(
                    mode = NJMode.General,
                    selected = currentMode == NJMode.General,
                    onClick = { viewModel.currentMode.value = NJMode.General },
                    icon = Icons.Default.ChatBubble
                )
                Spacer(modifier = Modifier.width(8.dp))
                NJModeChip(
                    mode = NJMode.EnglishTeacher,
                    selected = currentMode == NJMode.EnglishTeacher,
                    onClick = { viewModel.currentMode.value = NJMode.EnglishTeacher },
                    icon = Icons.Default.School
                )
                Spacer(modifier = Modifier.width(8.dp))
                NJModeChip(
                    mode = NJMode.DeviceControl,
                    selected = currentMode == NJMode.DeviceControl,
                    onClick = { viewModel.currentMode.value = NJMode.DeviceControl },
                    icon = Icons.Default.Settings
                )
            }

            // Visualizer Section
            Spacer(modifier = Modifier.height(24.dp))
            VoiceVisualizer(
                isSpeaking = isSpeaking,
                isListening = isListening,
                amplitude = amplitude,
                theme = visualizerTheme
            )
            
            if (isListening || isSpeaking) {
                DecibelMeter(dbLevel = dbLevel, peakLevel = peakAmplitude)
            }
            
            Text(
                text = when {
                    isSpeaking -> "NJ is speaking..."
                    isListening -> "Listening to you, Sir..."
                    else -> "NJ is ready, Sir."
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Input Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask NJ anything, Sir...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color(0xFF00B4D8),
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        if (textInput.isNotEmpty()) {
                            IconButton(onClick = {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                            }) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color(0xFF00B4D8))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = { 
                        if (isListening) {
                            viewModel.stopListening()
                        } else {
                            when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                                PackageManager.PERMISSION_GRANTED -> viewModel.startListening()
                                else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    containerColor = if (isListening) Color.Red else Color(0xFF00B4D8),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NJModeChip(mode: NJMode, selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(mode.name.replace("([a-z])([A-Z])".toRegex(), "$1 $2"), fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF00B4D8),
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = Color.White.copy(alpha = 0.6f),
            iconColor = Color.White.copy(alpha = 0.4f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.1f),
            selectedBorderColor = Color(0xFF00B4D8)
        )
    )
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) Color(0xFF0077B6) else Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 0.dp,
                bottomEnd = if (isUser) 0.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}
