package com.jarvis.security

import com.jarvis.config.ConfigRepository
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central safety policy. Defaults are conservative and fail closed.
 * [ToolDispatcher] treats this class as authoritative.
 */
@Singleton
class SecurityPolicy @Inject constructor(private val config: ConfigRepository) {

    private val dangerous = setOf(
        "send_sms", "make_call", "send_whatsapp", "send_email",
        "file_delete", "file_move", "file_write", "uninstall_app",
        "http_post", "install_apk", "type_text", "reply_notification",
        "open_url", "http_get", "security_capability"
    )

    private val communication = setOf(
        "send_sms", "make_call", "send_whatsapp", "send_email", "reply_notification"
    )

    private val networkTools = setOf("http_get", "http_post", "get_weather", "get_news", "open_url")

    private val sensitiveReads = setOf(
        "clipboard_get", "list_notifications", "contact_lookup", "get_location", "calendar_read"
    )

    private val consequentialWrites = setOf(
        "set_alarm", "set_timer", "notify", "save_memory"
    )

    /** Hosts permitted for outbound HTTP when cloud/network is enabled. */
    private val networkAllowlist = setOf(
        "api.open-meteo.com",
        "huggingface.co",
        "github.com",
        "objects.githubusercontent.com",
        "api.gdeltproject.org"
    )

    fun toolAllowed(name: String): Boolean = when (name) {
        "shell" -> config.current.value.shellEnabled
        else -> true
    }

    fun requiresConfirmation(name: String): Boolean {
        val c = config.current.value
        if (name in dangerous) return true
        if (c.communicationRequireConfirmation && name in communication) return true
        if (c.httpPostRequiresConfirmation && name == "http_post") return true
        if (c.fileDeleteRequiresConfirmation && name.startsWith("file_")) return true
        if (name in networkTools && name !in setOf("get_news", "get_weather")) return true
        if (name in sensitiveReads) return true
        if (name in consequentialWrites) return true
        return false
    }

    fun requiresPin(name: String): Boolean =
        (config.current.value.pinRequiredForHighRisk && name in dangerous) ||
            name == "security_capability"

    /**
     * Network policy: default-deny. Allow only when cloud fallback is enabled
     * (or the tool is the allowlisted weather provider) and the URL host is
     * on the allowlist and uses https.
     */
    fun networkAllowed(name: String, args: Map<String, Any?>): Boolean {
        if (name !in networkTools) return true
        if (name == "get_weather" || name == "get_news") return true
        val url = (args["url"] as? String)?.trim().orEmpty()
        if (url.isEmpty()) return false

        // Browser launch is deliberately separate from app-side HTTP access.
        // It may open any public HTTPS origin, but rejects obvious local/private
        // targets so an LLM cannot use it as an arbitrary LAN navigation primitive.
        if (name == "open_url") return isSafeExternalUrl(url)

        if (!config.current.value.cloudFallbackEnabled) return false
        return isAllowedUrl(url)
    }

    fun isAllowedUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme != "https") return false
            val host = uri.host?.lowercase() ?: return false
            if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal") ||
                isBlockedIpv4(host) || isBlockedIpv6(host)) return false
            networkAllowlist.any { host == it || host.endsWith(".$it") }
        } catch (_: Exception) {
            false
        }
    }
    fun isSafeExternalUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            if (uri.scheme?.lowercase() != "https") return false
            if (!uri.userInfo.isNullOrBlank()) return false
            if (uri.fragment != null && uri.fragment.length > 2048) return false
            val host = uri.host?.trim()?.lowercase() ?: return false
            if (host.length > 253) return false
            if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal") ||
                host.endsWith(".home.arpa") || host.endsWith(".lan")) return false
            if (isBlockedIpv4(host) || isBlockedIpv6(host)) return false
            if (host.endsWith(".localhost") || host.endsWith(".test") || host.endsWith(".invalid")) return false
            true
        } catch (_: Exception) { false }
    }

    private fun isBlockedIpv4(host: String): Boolean {
        val octets = host.split('.')
        if (octets.size != 4 || !octets.all { it.matches(Regex("\\d{1,3}")) }) return false
        val a = octets[0].toIntOrNull() ?: return false
        val b = octets[1].toIntOrNull() ?: 0
        val c = octets[2].toIntOrNull() ?: 0
        if (listOf(a, b, c).any { it !in 0..255 } || octets[3].toIntOrNull() !in 0..255) return false
        return a == 0 || a == 10 || a == 127 ||
            (a == 100 && b in 64..127) ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 0 && c == 0) ||
            (a == 192 && b == 0 && c == 2) ||
            (a == 192 && b == 168) ||
            (a == 198 && b == 18) ||
            (a == 198 && b == 19) ||
            (a >= 224)
    }

    private fun isBlockedIpv6(host: String): Boolean {
        val h = host.removePrefix("[").removeSuffix("]").lowercase()
        if (!h.contains(':')) return false
        if (h == "::" || h == "::1") return true
        return h.startsWith("fc") || h.startsWith("fd") ||
            h.startsWith("fe8") || h.startsWith("fe9") || h.startsWith("fea") || h.startsWith("feb")
    }

}
