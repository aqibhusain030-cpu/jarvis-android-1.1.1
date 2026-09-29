package com.jarvis.security

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.jarvis.tools.ConfirmationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent emergency stop. A kill survives service/process restart so a
 * watchdog or START_NOT_STICKY restart cannot silently resume the assistant.
 */
@Singleton
class KillSwitch @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val isKilled = MutableStateFlow(prefs.getBoolean(KEY_KILLED, false))

    fun kill() {
        prefs.edit().putBoolean(KEY_KILLED, true).commit()
        isKilled.value = true
    }

    fun resume() {
        prefs.edit().putBoolean(KEY_KILLED, false).commit()
        isKilled.value = false
    }

    fun isKillPhrase(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return normalized in KILL_PHRASES
    }

    @VisibleForTesting
    fun resetForTest() {
        prefs.edit().putBoolean(KEY_KILLED, false).commit()
        isKilled.value = false
    }

    companion object {
        private const val PREFS = "jarvis_security"
        private const val KEY_KILLED = "killed"
        private val KILL_PHRASES = setOf(
            "kill jarvis", "jarvis kill switch", "emergency stop", "stop all actions",
            "jarvis stop", "जार्विस बंद करो", "जार्विस बंद कर दो", "आपातकालीन रोक"
        )
    }
}
