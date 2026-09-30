package com.jarvis.friday.core

data class FridayOperationSpec(
    val id: String,
    val domain: Domain,
    val description: String,
    val existingTool: String? = null,
    val risk: FridayRisk = FridayRisk.LOW,
    val supported: Boolean = true
) {
    enum class Domain {
        COMMUNICATION,
        DEVICE,
        MEDIA,
        NETWORK,
        LOCATION,
        PRODUCTIVITY,
        MEMORY,
        SECURITY,
        SYSTEM,
        SENSING
    }
}

/**
 * Canonical operation catalogue. Existing JARVIS tools remain the concrete
 * Android implementations; FRIDAY owns the semantic command surface.
 */
object FridayOperationCatalog {
    val operations: List<FridayOperationSpec> = listOf(
        op("communicate.sms", Domain.COMMUNICATION, "Send SMS", "send_sms", FridayRisk.HIGH),
        op("communicate.call", Domain.COMMUNICATION, "Place a call", "make_call", FridayRisk.HIGH),
        op("communicate.whatsapp", Domain.COMMUNICATION, "Send WhatsApp message", "send_whatsapp", FridayRisk.HIGH),
        op("communicate.email", Domain.COMMUNICATION, "Send email", "send_email", FridayRisk.HIGH),
        op("device.launch_app", Domain.DEVICE, "Launch an installed application", "app_launch"),
        op("device.screenshot", Domain.SENSING, "Capture a screenshot", "screenshot"),
        op("device.volume", Domain.DEVICE, "Control device volume", "volume"),
        op("device.media", Domain.MEDIA, "Control media playback", "media"),
        op("device.alarm", Domain.PRODUCTIVITY, "Create an alarm", "set_alarm", FridayRisk.MEDIUM),
        op("device.timer", Domain.PRODUCTIVITY, "Create a timer", "set_timer", FridayRisk.MEDIUM),
        op("device.calendar", Domain.PRODUCTIVITY, "Read calendar data", "calendar_read", FridayRisk.MEDIUM),
        op("device.clipboard", Domain.DEVICE, "Read clipboard", "clipboard_get", FridayRisk.MEDIUM),
        op("device.file", Domain.DEVICE, "Manage application files", "file_write", FridayRisk.MEDIUM),
        op("device.camera", Domain.SENSING, "Capture camera input", "camera", FridayRisk.MEDIUM),
        op("device.wifi", Domain.NETWORK, "Inspect or control Wi-Fi", "wifi"),
        op("device.bluetooth", Domain.NETWORK, "Inspect or control Bluetooth", "bluetooth"),
        op("network.http_get", Domain.NETWORK, "Perform an allowlisted HTTP GET", "http_get", FridayRisk.HIGH),
        op("network.http_post", Domain.NETWORK, "Perform an allowlisted HTTP POST", "http_post", FridayRisk.HIGH),
        op("network.news", Domain.NETWORK, "Fetch near-real-time news", "get_news", FridayRisk.MEDIUM),
        op("device.navigation", Domain.DEVICE, "Perform bounded Android navigation", "device_navigation"),
        op("location.read", Domain.LOCATION, "Read device location", "get_location", FridayRisk.MEDIUM),
        op("location.maps", Domain.LOCATION, "Open a map location", "open_url"),
        op("memory.read", Domain.MEMORY, "Read JARVIS memory", "memory_read"),
        op("memory.write", Domain.MEMORY, "Write JARVIS memory", "save_memory", FridayRisk.MEDIUM),
        op("goal.manage", Domain.PRODUCTIVITY, "Manage JARVIS goals", "goal_manage"),
        op("system.status", Domain.SYSTEM, "Read system status", "system_status"),
        op("system.notify", Domain.SYSTEM, "Create an on-device notification", "notify", FridayRisk.MEDIUM),
        op("security.assess", Domain.SECURITY, "Assess application security posture", "security.assess.app", FridayRisk.MEDIUM),
        op("security.simulate", Domain.SECURITY, "Model an authorized exploit path", "security.simulate.exploit", FridayRisk.HIGH),
        op("security.monitor", Domain.SECURITY, "Manage authorized security monitoring", "security.monitor.enable", FridayRisk.HIGH),
        op("security.scope", Domain.SECURITY, "Enroll an authorized security target", "security.scope.enroll", FridayRisk.HIGH)
    )

    private fun op(
        id: String,
        domain: FridayOperationSpec.Domain,
        description: String,
        tool: String? = null,
        risk: FridayRisk = FridayRisk.LOW
    ) = FridayOperationSpec(id, domain, description, tool, risk)
}
