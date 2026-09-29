package com.jarvis.notifications

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends direct replies to notifications that advertise a RemoteInput action
 * (WhatsApp, Signal, Messages). Requires the listener service to be enabled.
 */
@Singleton
class NotificationReply @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger
) {
    fun reply(key: String, text: String): Boolean {
        val listener = NotificationListenerHolder.current
            ?: run { logger.w(TAG, "listener not connected"); return false }
        val sbn: StatusBarNotification = listener.activeNotifications
            .firstOrNull { it.key == key } ?: return false
        val actions = sbn.notification?.actions ?: return false
        val action = actions.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return false
        val intent = Intent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val bundle = Bundle()
        action.remoteInputs!!.forEach { bundle.putCharSequence(it.resultKey, text) }
        RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
        return try {
            action.actionIntent.send(ctx, 0, intent); true
        } catch (t: Throwable) { logger.e(TAG, "reply failed", t); false }
    }
    companion object { private const val TAG = "NotificationReply" }
}

/** Holds a live reference to the listener service for reply flows. */
object NotificationListenerHolder {
    @Volatile var current: NotificationListenerService? = null
}
