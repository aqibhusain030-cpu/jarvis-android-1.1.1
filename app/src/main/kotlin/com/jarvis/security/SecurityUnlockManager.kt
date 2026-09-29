package com.jarvis.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot, in-memory authorization token for high-risk tool calls.
 * The token is consumed by exactly one high-risk operation and is never
 * persisted. A successful biometric/device-credential authentication alone
 * cannot survive process death or be replayed indefinitely.
 */
@Singleton
class SecurityUnlockManager @Inject constructor() {
    private val token = AtomicLong(0L)

    fun consume(): Boolean {
        val value = token.get()
        if (value == 0L) return false
        return token.compareAndSet(value, 0L)
    }

    fun lock() { token.set(0L) }

    fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val manager = BiometricManager.from(activity)
        if (manager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult(false); return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    token.set(System.nanoTime())
                    onResult(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    token.set(0L); onResult(false)
                }
                override fun onAuthenticationFailed() {
                    // Keep the token locked until an explicit success callback.
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("JARVIS security unlock")
            .setSubtitle("Authenticate once for one high-risk action")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }
}
