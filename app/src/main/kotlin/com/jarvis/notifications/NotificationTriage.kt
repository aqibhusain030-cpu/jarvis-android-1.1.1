package com.jarvis.notifications

import javax.inject.Inject
import javax.inject.Singleton

/** Small classifier that ranks notifications by likely urgency. */
@Singleton
class NotificationTriage @Inject constructor() {
    private val urgent = listOf(
        "missed call", "urgent", "otp", "verify", "delivered", "confirmed",
        "महत्वपूर्ण", "पैकेज", "डिलीवर", "बैंक", "बैलेंस"
    )
    fun priority(title: String, text: String): Int {
        val blob = "$title $text".lowercase()
        return urgent.count { it in blob } + if (title.isNotBlank()) 1 else 0
    }
}
