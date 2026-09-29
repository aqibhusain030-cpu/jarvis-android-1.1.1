package com.jarvis.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reads notifications from the OS and exposes them to the tool layer. The
 * service is enabled by the user in system Settings; it stores the most recent
 * 100 entries in a bounded in-memory ring.
 */
class NotificationListener : NotificationListenerService() {

    data class Entry(val key: String, val pkg: String, val title: String,
                     val text: String, val at: Long)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val s = sbn ?: return
        val extras = s.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        push(Entry(s.key ?: "", s.packageName, title, text, System.currentTimeMillis()))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn?.key?.let { k -> ring.removeIf { it.key == k } }
    }

    private fun push(e: Entry) {
        ring.add(0, e)
        while (ring.size > 100) ring.removeAt(ring.size - 1)
    }


    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationListenerHolder.current = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (NotificationListenerHolder.current === this) {
            NotificationListenerHolder.current = null
        }
    }

    companion object {
        private val ring = CopyOnWriteArrayList<Entry>()
        fun active(): List<Entry> = ring.toList()
    }
}
