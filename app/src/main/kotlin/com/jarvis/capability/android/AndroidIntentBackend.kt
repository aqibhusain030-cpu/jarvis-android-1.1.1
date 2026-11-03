package com.jarvis.capability.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.jarvis.core.capability.*

class AndroidIntentBackend(private val context: Context) : CapabilityBackend {
    override val backendId = "android.intent"
    override val capabilities = setOf(
        Capability("android.open_url", "Open a web URL"),
        Capability("android.dial", "Open the phone dialer", Capability.RiskLevel.MEDIUM),
        Capability("android.compose_sms", "Open the SMS composer", Capability.RiskLevel.MEDIUM),
        Capability("android.open_maps", "Open a location in maps"),
        Capability("android.settings", "Open Android settings"),
        Capability("android.app_details", "Open application settings")
    )

    override suspend fun execute(r: CapabilityRequest): CapabilityResult = when (r.capabilityId) {
        "android.open_url" -> openUrl(r)
        "android.dial" -> dial(r)
        "android.compose_sms" -> sms(r)
        "android.open_maps" -> maps(r)
        "android.settings" -> settings(r)
        "android.app_details" -> appDetails(r)
        else -> rejected(r, CapabilityResult.RejectionCode.UNKNOWN_CAPABILITY, "Unsupported intent capability.")
    }

    private fun openUrl(r: CapabilityRequest): CapabilityResult {
        val raw = r.arguments["url"]?.trim() ?: return invalid(r)
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return invalid(r)
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return invalid(r)
        return start(r, Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, "Opening the website.")
    }

    private fun dial(r: CapabilityRequest): CapabilityResult {
        val phone = r.arguments["phone"]?.trim() ?: return invalid(r)
        if (!PHONE_REGEX.matches(phone)) return invalid(r)
        return start(r, Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, "Opening the dialer.")
    }

    private fun sms(r: CapabilityRequest): CapabilityResult {
        val phone = r.arguments["phone"]?.trim() ?: return invalid(r)
        val body = r.arguments["body"] ?: ""
        if (!PHONE_REGEX.matches(phone) || body.length > 4096) return invalid(r)
        return start(r, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone)).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, "Opening the message composer.")
    }

    private fun maps(r: CapabilityRequest): CapabilityResult {
        val query = r.arguments["query"]?.trim()?.takeIf { it.isNotEmpty() } ?: return invalid(r)
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        return start(r, Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, "Opening maps.")
    }

    private fun settings(r: CapabilityRequest): CapabilityResult {
        val action = when (r.arguments["target"]?.lowercase() ?: "main") {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "apps" -> Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "security" -> Settings.ACTION_SECURITY_SETTINGS
            "main" -> Settings.ACTION_SETTINGS
            else -> return rejected(r, CapabilityResult.RejectionCode.UNSUPPORTED, "That settings page is not supported.")
        }
        return start(r, Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }, "Opening Android settings.")
    }

    private fun appDetails(r: CapabilityRequest): CapabilityResult {
        val pkg = r.arguments["package"]?.trim()?.takeIf { it.isNotEmpty() } ?: return invalid(r)
        return start(r, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }, "Opening app settings.")
    }

    private fun start(r: CapabilityRequest, intent: Intent, message: String): CapabilityResult = try {
        if (context.packageManager.resolveActivity(intent, 0) == null)
            CapabilityResult.Failed(r.requestId, CapabilityResult.FailureCode.ACTIVITY_NOT_FOUND, "No compatible Android activity was found.")
        else {
            context.startActivity(intent)
            CapabilityResult.Success(r.requestId, message)
        }
    } catch (_: ActivityNotFoundException) {
        CapabilityResult.Failed(r.requestId, CapabilityResult.FailureCode.ACTIVITY_NOT_FOUND, "No compatible Android activity was found.")
    }

    private fun invalid(r: CapabilityRequest) =
        rejected(r, CapabilityResult.RejectionCode.INVALID_ARGUMENTS, "Required intent arguments are missing or invalid.")

    private fun rejected(r: CapabilityRequest, code: CapabilityResult.RejectionCode, message: String) =
        CapabilityResult.Rejected(r.requestId, code, message)

    private companion object {
        val PHONE_REGEX = Regex("""^\+?[0-9][0-9\s().-]{2,24}$""")
    }
}
