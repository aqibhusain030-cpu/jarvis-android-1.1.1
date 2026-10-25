package com.jarvis.config

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "jarvis_prefs")

/**
 * Persistent config with a synchronous [current] snapshot for hot paths.
 * Seeds from assets/config.json on first launch and loads all safety/autonomy fields.
 */
@Singleton
class ConfigRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger
) {
    val current = MutableStateFlow(JarvisConfig())

    suspend fun seedFromAssetIfEmpty() {
        val json = ctx.assets.open("config.json").bufferedReader().use { it.readText() }
        val seed = Defaults.parse(json)
        ctx.dataStore.edit { prefs ->
            if (!prefs.contains(KEY_CORE)) {
                prefs[KEY_CORE] = seed.coreEnabled
                prefs[KEY_USER] = seed.userName
                prefs[KEY_LANG] = seed.defaultLanguage
                prefs[KEY_WAKE_TH] = seed.wakeThreshold
                prefs[KEY_VAD] = seed.vadSilenceMs
                prefs[KEY_MAXCMD] = seed.maxCommandSeconds
                prefs[KEY_LLM] = seed.llmModel
                prefs[KEY_LLM_CTX] = seed.llmContextSize
                prefs[KEY_LLM_THR] = seed.llmThreads
                prefs[KEY_STT] = seed.sttModel
                prefs[KEY_STT_LANG] = seed.sttLanguage
                prefs[KEY_STT_THR] = seed.sttThreads
                prefs[KEY_COMM] = seed.communicationRequireConfirmation
                prefs[KEY_FILE_DEL] = seed.fileDeleteRequiresConfirmation
                prefs[KEY_HTTP_POST] = seed.httpPostRequiresConfirmation
                prefs[KEY_SHELL] = seed.shellEnabled
                prefs[KEY_PIN] = seed.pinRequiredForHighRisk
                prefs[KEY_CLOUD] = seed.cloudFallbackEnabled
                prefs[KEY_REMOTE] = seed.remoteInferenceEnabled
                prefs[KEY_REMOTE_URL] = seed.remoteInferenceBaseUrl
                prefs[KEY_REMOTE_MODEL] = seed.remoteInferenceModel
                prefs[KEY_REMOTE_TIMEOUT] = seed.remoteInferenceTimeoutMs
                prefs[KEY_KILL_PHRASE] = seed.killPhraseEnabled
                prefs[KEY_MAX_GOALS] = seed.maxActiveGoals
                prefs[KEY_MAX_TOOL_CALLS] = seed.maxToolCallsPerGoal
            }
        }
        current.value = load()
        logger.i(TAG, "config loaded user=${current.value.userName} lang=${current.value.defaultLanguage}")
    }

    fun load(): JarvisConfig = runBlocking {
        val p = ctx.dataStore.data.first()
        JarvisConfig(
            coreEnabled = p[KEY_CORE] ?: true,
            userName = p[KEY_USER] ?: "Sir",
            defaultLanguage = p[KEY_LANG] ?: "hi",
            wakeThreshold = p[KEY_WAKE_TH] ?: 0.5f,
            vadSilenceMs = p[KEY_VAD] ?: 800,
            maxCommandSeconds = p[KEY_MAXCMD] ?: 20,
            llmModel = p[KEY_LLM] ?: "Qwen2.5-3B-Instruct-Q4_K_M.gguf",
            llmContextSize = p[KEY_LLM_CTX] ?: 4096,
            llmThreads = p[KEY_LLM_THR] ?: 4,
            sttModel = p[KEY_STT] ?: "ggml-small.bin",
            sttLanguage = p[KEY_STT_LANG] ?: "auto",
            sttThreads = p[KEY_STT_THR] ?: 4,
            maxActiveGoals = p[KEY_MAX_GOALS] ?: 10,
            maxToolCallsPerGoal = p[KEY_MAX_TOOL_CALLS] ?: 200,
            communicationRequireConfirmation = p[KEY_COMM] ?: true,
            fileDeleteRequiresConfirmation = p[KEY_FILE_DEL] ?: true,
            httpPostRequiresConfirmation = p[KEY_HTTP_POST] ?: true,
            shellEnabled = p[KEY_SHELL] ?: false,
            pinRequiredForHighRisk = p[KEY_PIN] ?: true,
            cloudFallbackEnabled = p[KEY_CLOUD] ?: false,
            remoteInferenceEnabled = p[KEY_REMOTE] ?: false,
            remoteInferenceBaseUrl = p[KEY_REMOTE_URL] ?: "",
            remoteInferenceModel = p[KEY_REMOTE_MODEL] ?: "JARVIS-2T-MoE",
            remoteInferenceTimeoutMs = p[KEY_REMOTE_TIMEOUT] ?: 60000,
            killPhraseEnabled = p[KEY_KILL_PHRASE] ?: true
        )
    }

    suspend fun setRemoteInference(
        enabled: Boolean,
        baseUrl: String,
        model: String,
        timeoutMs: Int
    ) {
        require(baseUrl.length <= 2048)
        require(model.isNotBlank() && model.length <= 256)
        require(timeoutMs in 5_000..300_000)

        ctx.dataStore.edit { prefs ->
            prefs[KEY_REMOTE] = enabled
            prefs[KEY_REMOTE_URL] = baseUrl.trim()
            prefs[KEY_REMOTE_MODEL] = model.trim()
            prefs[KEY_REMOTE_TIMEOUT] = timeoutMs
        }
        current.value = load()
    }

    fun dir(name: String): File = File(ctx.filesDir, name).apply { mkdirs() }

    fun modelFile(kind: String, name: String): File =
        File(File(ctx.filesDir, "models/$kind"), name).also { it.parentFile?.mkdirs() }

    companion object {
        private const val TAG = "Config"
        private val KEY_CORE = booleanPreferencesKey("core")
        private val KEY_USER = stringPreferencesKey("user")
        private val KEY_LANG = stringPreferencesKey("lang")
        private val KEY_WAKE_TH = floatPreferencesKey("wake_th")
        private val KEY_VAD = intPreferencesKey("vad")
        private val KEY_MAXCMD = intPreferencesKey("maxcmd")
        private val KEY_LLM = stringPreferencesKey("llm")
        private val KEY_LLM_CTX = intPreferencesKey("llm_ctx")
        private val KEY_LLM_THR = intPreferencesKey("llm_thr")
        private val KEY_STT = stringPreferencesKey("stt")
        private val KEY_STT_LANG = stringPreferencesKey("stt_lang")
        private val KEY_STT_THR = intPreferencesKey("stt_thr")
        private val KEY_COMM = booleanPreferencesKey("comm")
        private val KEY_FILE_DEL = booleanPreferencesKey("file_del")
        private val KEY_HTTP_POST = booleanPreferencesKey("http_post")
        private val KEY_SHELL = booleanPreferencesKey("shell")
        private val KEY_PIN = booleanPreferencesKey("pin")
        private val KEY_CLOUD = booleanPreferencesKey("cloud")
        private val KEY_REMOTE = booleanPreferencesKey("remote_enabled")
        private val KEY_REMOTE_URL = stringPreferencesKey("remote_url")
        private val KEY_REMOTE_MODEL = stringPreferencesKey("remote_model")
        private val KEY_REMOTE_TIMEOUT = intPreferencesKey("remote_timeout_ms")
        private val KEY_MAX_GOALS = intPreferencesKey("max_goals")
        private val KEY_MAX_TOOL_CALLS = intPreferencesKey("max_tool_calls")
        private val KEY_KILL_PHRASE = booleanPreferencesKey("kill_phrase")
    }
}
