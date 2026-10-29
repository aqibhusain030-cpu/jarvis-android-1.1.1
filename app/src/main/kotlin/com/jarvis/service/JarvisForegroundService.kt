package com.jarvis.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jarvis.JarvisApplication
import com.jarvis.MainActivity
import com.jarvis.R
import com.jarvis.autonomy.GoalEngine
import com.jarvis.brain.BrainBridge
import com.jarvis.config.ConfigRepository
import com.jarvis.logging.JarvisLogger
import com.jarvis.memory.MemoryStore
import com.jarvis.security.KillSwitch
import com.jarvis.tools.ConfirmationManager
import com.jarvis.voice.BargeInController
import com.jarvis.voice.SpeechRecognizerBridge
import com.jarvis.voice.TextToSpeechBridge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/** User-started microphone foreground service. It never self-starts at boot. */
@AndroidEntryPoint
class JarvisForegroundService : LifecycleService() {
    @Inject lateinit var wakeWord: WakeWordDetector
    @Inject lateinit var audio: AudioCapture
    @Inject lateinit var vad: VoiceActivityDetector
    @Inject lateinit var stt: SpeechRecognizerBridge
    @Inject lateinit var tts: TextToSpeechBridge
    @Inject lateinit var brain: BrainBridge
    @Inject lateinit var memory: MemoryStore
    @Inject lateinit var config: ConfigRepository
    @Inject lateinit var goals: GoalEngine
    @Inject lateinit var killSwitch: KillSwitch
    @Inject lateinit var logger: JarvisLogger
    @Inject lateinit var bargeIn: BargeInController
    @Inject lateinit var confirmation: ConfirmationManager
    @Inject lateinit var voiceRouter: com.jarvis.voice.VoiceCommandRouter

    private var wakeLock: PowerManager.WakeLock? = null
    private var watchdog: JarvisWatchdog? = null
    private var voiceJob: Job? = null
    private var confirmationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        if (!hasMicPermission() || killSwitch.isKilled.value) {
            logger.w(TAG, "Service refused: mic permission missing or kill switch active")
            stopSelf()
            return
        }
        startForeground(NOTIF_ID, buildNotification(getString(R.string.svc_idle)))
        acquireWakeLock()
        watchdog = JarvisWatchdog(lifecycleScope, this) { restartVoiceLoop() }
        watchdog?.start()
        lifecycleScope.launch { goals.resumePersisted() }
        logger.i(TAG, "Foreground service started by explicit user action")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                voiceJob?.cancel()
                confirmationJob?.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_KILL -> {
                confirmation.cancel()
                killSwitch.kill()
                voiceJob?.cancel()
                confirmationJob?.cancel()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                if (voiceJob?.isActive != true) {
                    if (!stt.isModelInstalled()) {
                        updateNotification("Speech model missing. Install it from the JARVIS home screen.")
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    confirmationJob = lifecycleScope.launch { confirmationInputLoop() }
                    voiceJob = lifecycleScope.launch { runManualVoiceLoop() }
                }
            }
            ACTION_START_WAKEWORD -> {
                if (voiceJob?.isActive != true) {
                    confirmationJob = lifecycleScope.launch { confirmationInputLoop() }
                    voiceJob = lifecycleScope.launch { runVoiceLoop() }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onDestroy() {
        voiceJob?.cancel()
        confirmationJob?.cancel()
        confirmation.cancel()
        watchdog?.stop()
        runCatching { audio.stop() }
        runCatching { wakeWord.close() }
        runCatching { stt.close() }
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    /**
     * The Start button is an explicit listen action, not an always-on wake-word
     * promise. Capturing a command directly makes the app usable even when the
     * optional wake-word ONNX assets have not been provisioned.
     */
    private suspend fun runManualVoiceLoop() = coroutineScope {
        tts.speak("JARVIS is listening. Speak a command, then pause.")
        while (isActive && !killSwitch.isKilled.value) {
            try {
                updateNotification("Listening for your command")
                val pcm = vad.captureUtterance(audio)
                watchdog?.beat()
                if (pcm.isEmpty()) continue

                updateNotification("Transcribing command")
                val text = stt.transcribe(pcm).trim()
                if (text.isBlank()) {
                    tts.speak(getString(R.string.reply_didnt_catch))
                    continue
                }

                if (config.current.value.killPhraseEnabled && killSwitch.isKillPhrase(text)) {
                    confirmation.cancel()
                    killSwitch.kill()
                    updateNotification(getString(R.string.svc_killed))
                    tts.speak(getString(R.string.reply_killed))
                    stopSelf()
                    break
                }

                memory.appendMessage("user", text, config.current.value.defaultLanguage)
                updateNotification("Thinking")
                val routed = runCatching { voiceRouter.execute(text) }.getOrNull()
                val replyText = routed?.reply ?: brain.respond(text).text
                val replyLanguage = config.current.value.defaultLanguage

                updateNotification("Speaking")
                tts.speak(replyText)
                memory.appendMessage("assistant", replyText, replyLanguage)
                watchdog?.beat()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                logger.e(TAG, "manual voice loop error", t)
                updateNotification("Voice command failed: ${t.message ?: "check Logs"}")
                delay(1000)
            }
        }
    }

    private suspend fun runVoiceLoop() = coroutineScope {
        while (isActive && !killSwitch.isKilled.value) {
            try {
                updateNotification(getString(R.string.svc_listening))
                wakeWord.awaitWake()
                if (killSwitch.isKilled.value) break
                tts.speak(getString(R.string.reply_yes_sir))
                bargeIn.armBargeIn()
                val pcm = vad.captureUtterance(audio)
                if (pcm.isEmpty()) { tts.speak(getString(R.string.reply_didnt_catch)); continue }
                updateNotification(getString(R.string.svc_thinking))
                val text = stt.transcribe(pcm).trim()
                if (text.isBlank()) { tts.speak(getString(R.string.reply_didnt_catch)); continue }

                if (config.current.value.killPhraseEnabled && killSwitch.isKillPhrase(text)) {
                    confirmation.cancel()
                    killSwitch.kill()
                    updateNotification(getString(R.string.svc_killed))
                    tts.speak(getString(R.string.reply_killed))
                    stopSelf()
                    break
                }

                // A separate listener coroutine handles confirmation while brain.respond()
                // is blocked awaiting the result of ToolDispatcher.confirmation.ask().
                memory.appendMessage("user", text, config.current.value.defaultLanguage)

                val routed = runCatching { voiceRouter.execute(text) }.getOrNull()
                val replyText = routed?.reply ?: brain.respond(text).text
                val replyLanguage = config.current.value.defaultLanguage

                updateNotification(getString(R.string.svc_speaking))
                tts.speak(replyText)
                memory.appendMessage("assistant", replyText, replyLanguage)
                watchdog?.beat()
            } catch (ce: CancellationException) { throw ce }
            catch (t: Throwable) {
                logger.e(TAG, "voice loop error", t)
                runCatching { tts.speak(getString(R.string.reply_error)) }
                delay(1000)
            }
        }
    }

    private suspend fun confirmationInputLoop() {
        while (currentCoroutineContext().isActive && !killSwitch.isKilled.value) {
            if (!confirmation.hasPending()) { delay(120); continue }
            try {
                val pcm = withTimeoutOrNull(CONFIRM_CAPTURE_TIMEOUT_MS) {
                    vad.captureUtterance(audio)
                }
                if (pcm != null && pcm.isNotEmpty() && confirmation.hasPending()) {
                    val answer = stt.transcribe(pcm)
                    confirmation.submit(answer)
                }
            } catch (ce: CancellationException) { throw ce }
            catch (t: Throwable) {
                logger.w(TAG, "confirmation listener error: ${t.message}")
                delay(250)
            }
        }
    }

    private fun restartVoiceLoop() {
        if (killSwitch.isKilled.value) return
        logger.w(TAG, "Watchdog reset")
        lifecycleScope.launch {
            runCatching { wakeWord.reset() }
            runCatching { audio.reset() }
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:service")
            .also { it.acquire() }
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, JarvisForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val kill = PendingIntent.getService(this, 2,
            Intent(this, JarvisForegroundService::class.java).setAction(ACTION_KILL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, JarvisApplication.CH_FOREGROUND)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_jarvis)
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_stop), stop)
            .addAction(0, getString(R.string.action_emergency_stop), kill)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    companion object {
        const val TAG = "JarvisService"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.jarvis.STOP"
        const val ACTION_START = "com.jarvis.START"
        const val ACTION_START_WAKEWORD = "com.jarvis.START_WAKEWORD"
        const val ACTION_KILL = "com.jarvis.KILL"
        private const val CONFIRM_CAPTURE_TIMEOUT_MS = 8_000L

        /** Explicit user action only. Call from foreground UI after RECORD_AUDIO is granted. */
        fun start(ctx: Context): Boolean {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) return false
            val i = Intent(ctx, JarvisForegroundService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
            return true
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, JarvisForegroundService::class.java))
        }
    }
}
