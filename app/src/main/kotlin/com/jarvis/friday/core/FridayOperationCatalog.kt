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
        op("communicate.sms", FridayOperationSpec.Domain.COMMUNICATION, "Send SMS", "send_sms", FridayRisk.HIGH),
        op("communicate.call", FridayOperationSpec.Domain.COMMUNICATION, "Place a call", "make_call", FridayRisk.HIGH),
        op("communicate.whatsapp", FridayOperationSpec.Domain.COMMUNICATION, "Send WhatsApp message", null, FridayRisk.HIGH, false),
        op("communicate.email", FridayOperationSpec.Domain.COMMUNICATION, "Send email", null, FridayRisk.HIGH, false),
        op("device.launch_app", FridayOperationSpec.Domain.DEVICE, "Launch an installed application", "open_app"),
        op("device.screenshot", FridayOperationSpec.Domain.SENSING, "Capture a screenshot", "screenshot"),
        op("device.volume", FridayOperationSpec.Domain.DEVICE, "Control device volume", "set_volume"),
        op("device.media", FridayOperationSpec.Domain.MEDIA, "Control media playback", "media_control"),
        op("device.alarm", FridayOperationSpec.Domain.PRODUCTIVITY, "Create an alarm", "set_alarm", FridayRisk.MEDIUM),
        op("device.timer", FridayOperationSpec.Domain.PRODUCTIVITY, "Create a timer", "set_timer", FridayRisk.MEDIUM),
        op("device.calendar", FridayOperationSpec.Domain.PRODUCTIVITY, "Read calendar data", "calendar_read", FridayRisk.MEDIUM),
        op("device.clipboard", FridayOperationSpec.Domain.DEVICE, "Read clipboard", "clipboard_get", FridayRisk.MEDIUM),
        op("device.file", FridayOperationSpec.Domain.DEVICE, "Manage application files", "file_read", FridayRisk.MEDIUM),
        op("device.camera", FridayOperationSpec.Domain.SENSING, "Capture camera input", "take_photo", FridayRisk.MEDIUM),
        op("device.wifi", FridayOperationSpec.Domain.NETWORK, "Inspect or control Wi-Fi", "wifi_status"),
        op("device.bluetooth", FridayOperationSpec.Domain.NETWORK, "Inspect or control Bluetooth", "bluetooth_status"),
        op("network.http_get", FridayOperationSpec.Domain.NETWORK, "Perform an allowlisted HTTP GET", "http_get", FridayRisk.HIGH),
        op("network.dns_resolve", FridayOperationSpec.Domain.NETWORK, "Resolve DNS inside an authorized scope", "network.dns_resolve"),
        op("network.tcp_probe", FridayOperationSpec.Domain.NETWORK, "Probe TCP connectivity inside an authorized scope", "network.tcp_probe", FridayRisk.MEDIUM),
        op("network.port_scan", FridayOperationSpec.Domain.NETWORK, "Perform a bounded port scan inside an authorized scope", "network.port_scan", FridayRisk.HIGH),
        op("network.service_detect", FridayOperationSpec.Domain.NETWORK, "Identify an observed service inside an authorized scope", "network.service_detect", FridayRisk.MEDIUM),
        op("network.tls_inspect", FridayOperationSpec.Domain.NETWORK, "Inspect TLS metadata inside an authorized scope", "network.tls_inspect", FridayRisk.MEDIUM),
        op("network.inventory", FridayOperationSpec.Domain.NETWORK, "Read the authorized network inventory", "network.inventory"),
        op("network.target_profile", FridayOperationSpec.Domain.NETWORK, "Build a structured authorized target profile", "network.target_profile", FridayRisk.MEDIUM),
        op("websec.analyze", FridayOperationSpec.Domain.SECURITY, "Analyze safe web security controls in an authorized scope", "websec.analyze", FridayRisk.HIGH),
        op("network.http_post", FridayOperationSpec.Domain.NETWORK, "Perform an allowlisted HTTP POST", "http_post", FridayRisk.HIGH),
        op("network.news", FridayOperationSpec.Domain.NETWORK, "Fetch near-real-time news", "get_news", FridayRisk.LOW),
        op("device.navigation", FridayOperationSpec.Domain.DEVICE, "Perform bounded Android navigation", "device_navigation"),
        op("location.read", FridayOperationSpec.Domain.LOCATION, "Read device location", "get_location", FridayRisk.MEDIUM),
        op("location.maps", FridayOperationSpec.Domain.LOCATION, "Open a map location", "open_url"),
        op("memory.read", FridayOperationSpec.Domain.MEMORY, "Read JARVIS memory", null, FridayRisk.LOW, false),
        op("memory.write", FridayOperationSpec.Domain.MEMORY, "Write JARVIS memory", "save_memory", FridayRisk.MEDIUM),
        op("goal.manage", FridayOperationSpec.Domain.PRODUCTIVITY, "Manage JARVIS goals", "create_goal"),
        op("system.status", FridayOperationSpec.Domain.SYSTEM, "Read system status", "system_info"),
        op("system.notify", FridayOperationSpec.Domain.SYSTEM, "Create an on-device notification", "notify", FridayRisk.MEDIUM),
        op("security.assess", FridayOperationSpec.Domain.SECURITY, "Assess application security posture", "security.assess.app", FridayRisk.MEDIUM),
        op("security.simulate", FridayOperationSpec.Domain.SECURITY, "Model an authorized exploit path", "security.simulate.exploit", FridayRisk.HIGH),
        op("security.monitor", FridayOperationSpec.Domain.SECURITY, "Manage authorized security monitoring", "security.monitor.enable", FridayRisk.HIGH),
        op("security.scope", FridayOperationSpec.Domain.SECURITY, "Enroll an authorized security target", "security.scope.enroll", FridayRisk.HIGH)
    )

    private fun op(
        id: String,
        domain: FridayOperationSpec.Domain,
        description: String,
        tool: String? = null,
        risk: FridayRisk = FridayRisk.LOW,
        supported: Boolean = true
    ) = FridayOperationSpec(id, domain, description, tool, risk, supported)
}
