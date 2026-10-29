package com.jarvis.ui

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jarvis.R
import com.jarvis.brain.BrainBridge
import com.jarvis.memory.MemoryStore
import com.jarvis.models.ModelManager
import com.jarvis.security.KillSwitch
import com.jarvis.service.JarvisForegroundService
import com.jarvis.voice.TextToSpeechBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private data class ChatMessage(val id: Long, val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nav: NavController,
    killSwitch: KillSwitch,
    brain: BrainBridge,
    models: ModelManager,
    memory: MemoryStore,
    tts: TextToSpeechBridge
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf(ChatMessage(0, "JARVIS", "Hello. I am JARVIS. Type a command below, or install the voice model and tap Start Listening."))
    }
    var nextMessageId by remember { mutableLongStateOf(1L) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Ready for a command") }
    var llmInstalled by remember { mutableStateOf(false) }
    var sttInstalled by remember { mutableStateOf(false) }
    var llmProgress by remember { mutableStateOf("") }
    var sttProgress by remember { mutableStateOf("") }
    val killed by killSwitch.isKilled.collectAsState()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    LaunchedEffect(Unit) {
        val installed = withContext(Dispatchers.IO) {
            val llm = runCatching { models.asset("qwen2.5-3b-instruct-q4_k_m")?.let(models::isPresent) == true }.getOrDefault(false)
            val stt = runCatching { models.asset("whisper-small")?.let(models::isPresent) == true }.getOrDefault(false)
            llm to stt
        }
        llmInstalled = installed.first
        sttInstalled = installed.second
        status = when {
            !llmInstalled && !sttInstalled -> "Setup required: install the AI and speech models below"
            !llmInstalled -> "AI model not installed; chat will use limited fallback replies"
            !sttInstalled -> "Speech model not installed; text commands are available"
            else -> "Models verified"
        }
        runCatching {
            val history = withContext(Dispatchers.IO) { memory.recentContext(12) }
            history.forEach { line ->
                val split = line.indexOf(": ")
                if (split > 0) {
                    val role = line.substring(0, split)
                    val text = line.substring(split + 2)
                    if (role.equals("user", true) || role.equals("assistant", true)) {
                        messages.add(ChatMessage(nextMessageId++, if (role.equals("user", true)) "You" else "JARVIS", text))
                    }
                }
            }
        }
    }

    fun downloadModel(id: String, isLlm: Boolean) {
        scope.launch {
            status = if (isLlm) "Downloading AI model. This is about 2.1 GB." else "Downloading speech model. This is about 488 MB."
            if (isLlm) llmProgress = "Starting download…" else sttProgress = "Starting download…"
            try {
                val file = withContext(Dispatchers.IO) {
                    models.download(id) { done, total ->
                        val percent = if (total > 0) (done * 100 / total).toInt() else 0
                        val readable = String.format(Locale.US, "%d%% · %.0f / %.0f MB", percent, done / 1_000_000.0, total / 1_000_000.0)
                        mainHandler.post {
                            if (isLlm) llmProgress = readable else sttProgress = readable
                        }
                    }
                }
                val valid = file != null && withContext(Dispatchers.IO) {
                    val asset = models.asset(id)
                    asset != null && models.verify(asset).verified
                }
                if (!valid) {
                    status = "Download failed verification. Check your connection and retry."
                    if (isLlm) llmProgress = "Not installed" else sttProgress = "Not installed"
                } else {
                    if (isLlm) {
                        llmInstalled = true
                        llmProgress = "Installed and verified"
                        status = "AI model installed. Send a message to initialize the brain."
                    } else {
                        sttInstalled = true
                        sttProgress = "Installed and verified"
                        status = "Speech model installed. Start Listening uses push-to-talk style listening without a wake word."
                    }
                }
            } catch (t: Throwable) {
                status = "Model installation failed: ${t.message ?: "unknown error"}"
                if (isLlm) llmProgress = "Failed" else sttProgress = "Failed"
            }
        }
    }

    fun sendMessage() {
        val query = input.trim()
        if (query.isBlank() || busy) return
        messages.add(ChatMessage(nextMessageId++, "You", query))
        input = ""
        busy = true
        status = "Thinking…"
        scope.launch {
            try {
                runCatching { memory.appendMessage("user", query, "auto") }
                val reply = withContext(Dispatchers.IO) { brain.respond(query) }
                messages.add(ChatMessage(nextMessageId++, "JARVIS", reply.text))
                runCatching { memory.appendMessage("assistant", reply.text, reply.language) }
                status = if (!llmInstalled && reply.text.contains("model", true)) "Install the AI model below for full answers" else "Ready for a command"
                runCatching { tts.speak(reply.text) }
            } catch (t: Throwable) {
                val error = "I couldn't process that request: ${t.message ?: t.javaClass.simpleName}"
                messages.add(ChatMessage(nextMessageId++, "JARVIS", error))
                status = "Command failed. See Logs for details."
            } finally {
                busy = false
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("JARVIS", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { nav.navigate("goals") }) { Icon(Icons.Default.CheckCircle, "Goals") }
                IconButton(onClick = { nav.navigate("memory") }) { Icon(Icons.Default.FavoriteBorder, "Memory") }
                IconButton(onClick = { nav.navigate("logs") }) { Icon(Icons.Default.List, "Logs") }
                IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Default.Settings, "Settings") }
            }
        )
    }) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(if (killed) "Emergency stop is active" else status, style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("On-device AI model", fontWeight = FontWeight.SemiBold)
                            Text(if (llmInstalled) "Installed · SHA-256 verified" else "Not installed · 2.1 GB download", style = MaterialTheme.typography.bodySmall)
                            if (llmProgress.isNotBlank()) Text(llmProgress, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!llmInstalled) {
                            Button(onClick = { downloadModel("qwen2.5-3b-instruct-q4_k_m", true) }, enabled = !busy && !llmProgress.startsWith("Downloading")) { Text("Install") }
                        } else {
                            Icon(Icons.Default.CheckCircle, "Verified model", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Speech recognition model", fontWeight = FontWeight.SemiBold)
                            Text(if (sttInstalled) "Installed · SHA-256 verified" else "Not installed · 488 MB download", style = MaterialTheme.typography.bodySmall)
                            if (sttProgress.isNotBlank()) Text(sttProgress, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!sttInstalled) {
                            Button(onClick = { downloadModel("whisper-small", false) }, enabled = !busy && !sttProgress.startsWith("Downloading")) { Text("Install") }
                        } else {
                            Icon(Icons.Default.CheckCircle, "Verified model", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val userMessage = message.role == "You"
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (userMessage) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (userMessage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(message.role, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(message.text, style = MaterialTheme.typography.bodyMedium)
                                if (!userMessage) {
                                    TextButton(onClick = { scope.launch { runCatching { tts.speak(message.text) } } }) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Speak")
                                    }
                                }
                            }
                        }
                    }
                }
                if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(4000) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Ask JARVIS") },
                    placeholder = { Text("e.g. what time is it?") },
                    maxLines = 3,
                    enabled = !busy
                )
                IconButton(onClick = { sendMessage() }, enabled = input.isNotBlank() && !busy) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = !killed && sttInstalled,
                    onClick = {
                        status = "Listening. Speak a command after tapping Start Listening."
                        if (JarvisForegroundService.start(ctx)) status = "Listening started. Speak clearly, then pause."
                        else status = "Microphone permission required. Allow it in Android settings."
                    }
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start Listening")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = { JarvisForegroundService.stop(ctx); status = "Voice listening stopped" }
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Stop")
                }
            }
        }
    }
}
