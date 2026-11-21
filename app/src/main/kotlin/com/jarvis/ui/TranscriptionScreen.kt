package com.jarvis.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(
    nav: NavController,
    viewModel: TranscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.transcribe()
        else viewModel.setError("Microphone permission is required. Audio is processed locally.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline speech recognition") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Speak naturally in Hindi, English or Arabic. JARVIS records one complete utterance, waits for silence, and transcribes it with the local Whisper model.",
                fontWeight = FontWeight.Medium
            )
            Text("No keyboard microphone. No Android speech-recognition service. No cloud upload. No AI response is generated on this screen.")
            Button(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) viewModel.transcribe()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                enabled = !state.listening,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.listening) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(4.dp))
                    Text("Listening — finish speaking")
                } else {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Text("Start recording")
                }
            }
            if (state.listening) {
                Text("Recording locally. Pause naturally when your sentence is complete; recognition starts after silence.")
            }
            if (state.error.isNotBlank()) {
                Card(colors = CardDefaults.cardColors()) {
                    Text(state.error, modifier = Modifier.padding(16.dp))
                }
            }
            if (state.transcript.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("TRANSCRIPT", fontWeight = FontWeight.Bold)
                        Text(state.language)
                        Text(state.transcript)
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val clipboard = remember(context) {
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            }
                            IconButton(onClick = {
                                clipboard.setPrimaryClip(ClipData.newPlainText("JARVIS transcript", state.transcript))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy transcript")
                            }
                            IconButton(onClick = viewModel::clear) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear transcript")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = viewModel::clear, modifier = Modifier.fillMaxWidth()) {
                Text("Clear")
            }
        }
    }
}
