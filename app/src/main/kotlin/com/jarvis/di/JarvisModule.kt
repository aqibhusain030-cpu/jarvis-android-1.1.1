package com.jarvis.di

import android.content.Context
import com.jarvis.capability.accessibility.AccessibilityBackend
import com.jarvis.capability.accessibility.JarvisAccessibilityService
import com.jarvis.capability.android.AndroidIntentBackend
import com.jarvis.capability.android.PackageBackend
import com.jarvis.capability.security.SecurityLabBackend
import com.jarvis.capability.network.NetworkSecurityBackend
import com.jarvis.capability.security.SecurityAssessmentBackend
import com.jarvis.network.DnsResolver
import com.jarvis.network.PortScanner
import com.jarvis.network.TcpProbe
import com.jarvis.network.ServiceFingerprinter
import com.jarvis.network.NetworkMonitor
import com.jarvis.network.RouteInspector
import com.jarvis.network.UdpProbe
import com.jarvis.network.WebSocketProbe
import com.jarvis.network.TlsInspector
import com.jarvis.security.web.WebSecurityAnalyzer
import com.jarvis.friday.security.ConsentMonitor
import com.jarvis.friday.security.ExploitSimulator
import com.jarvis.friday.security.InMemoryConsentMonitor
import com.jarvis.friday.security.SecurityAssessmentEngine
import com.jarvis.friday.security.SecurityScopeRegistry
import com.jarvis.friday.security.SecurityScopeProofSigner
import com.jarvis.friday.security.FridayOperatorAuthorization
import com.jarvis.core.capability.OperatorAuthorization
import com.jarvis.core.capability.CapabilityExecutor
import com.jarvis.core.capability.DefaultCapabilityPolicy
import com.jarvis.core.capability.CapabilityRegistry
import com.jarvis.friday.FridayPrime
import com.jarvis.friday.action.FridayCapabilityGateway
import com.jarvis.friday.audit.FridayAuditSink
import com.jarvis.friday.audit.InMemoryFridayAudit
import com.jarvis.friday.core.FridayExecutive
import com.jarvis.tools.ToolRegistry
import com.jarvis.tools.impl.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JarvisModule {

    @Provides @Singleton
    fun scope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun capabilityRegistry(): CapabilityRegistry = CapabilityRegistry()

    @Provides @Singleton
    fun operatorAuthorization(unlock: com.jarvis.security.SecurityUnlockManager): OperatorAuthorization = FridayOperatorAuthorization(unlock)

    @Provides @Singleton
    fun securityScopes(signer: SecurityScopeProofSigner): SecurityScopeRegistry = SecurityScopeRegistry(signer)

    @Provides @Singleton
    fun securityAssessment(): SecurityAssessmentEngine = SecurityAssessmentEngine()

    @Provides @Singleton
    fun exploitSimulator(): ExploitSimulator = ExploitSimulator()

    @Provides @Singleton
    fun consentMonitor(): ConsentMonitor = InMemoryConsentMonitor()

    @Provides @Singleton
    fun capabilityBackends(
        @ApplicationContext context: Context,
        scopes: SecurityScopeRegistry,
        assessment: SecurityAssessmentEngine,
        simulator: ExploitSimulator,
        monitor: ConsentMonitor,
        operator: OperatorAuthorization,
        networkBackend: NetworkSecurityBackend,
        securityAssessmentBackend: SecurityAssessmentBackend,
        toolRegistry: ToolRegistry,
        toolDispatcher: com.jarvis.tools.ToolDispatcher
    ): List<com.jarvis.core.capability.CapabilityBackend> = listOf(
        AndroidIntentBackend(context),
        PackageBackend(context),
        SecurityLabBackend(scopes, assessment, simulator, monitor, operator),
        networkBackend,
        securityAssessmentBackend,
        AccessibilityBackend(JarvisAccessibilityService.controller()),
        com.jarvis.core.capability.ToolCapabilityBackend(toolRegistry, toolDispatcher)
    )

    @Provides @Singleton
    fun capabilityExecutor(
        registry: CapabilityRegistry,
        backends: List<com.jarvis.core.capability.CapabilityBackend>
    ): CapabilityExecutor {
        backends.forEachIndexed { index, backend ->
            registry.register(backend, priority = 100 - index)
        }
        return CapabilityExecutor(
            registry = registry,
            policyProvider = DefaultCapabilityPolicy()
        )
    }

    @Provides @Singleton
    fun networkSecurityBackend(
        scopes: SecurityScopeRegistry,
        events: com.jarvis.network.NetworkEventBus,
        inventory: com.jarvis.network.NetworkInventory,
        operator: OperatorAuthorization,
        monitor: NetworkMonitor,
        routes: RouteInspector
    ): NetworkSecurityBackend {
        val dns = DnsResolver()
        val tcp = TcpProbe()
        val scanner = PortScanner(tcp, scopes)
        val fingerprint = ServiceFingerprinter()
        val engine = com.jarvis.network.NetworkEngine(dns, scanner, fingerprint, scopes, inventory, events)
        return NetworkSecurityBackend(
            scopes,
            engine,
            TlsInspector(),
            inventory,
            tcp,
            fingerprint,
            operator,
            WebSecurityAnalyzer(scopes),
            monitor,
            routes,
            UdpProbe(),
            WebSocketProbe()
        )
    }

    @Provides @Singleton
    fun fridayAudit(): FridayAuditSink = InMemoryFridayAudit()

    @Provides @Singleton
    fun fridayPrime(
        capabilityExecutor: CapabilityExecutor,
        audit: FridayAuditSink
    ): FridayPrime = FridayPrime(
        executive = FridayExecutive(),
        actionGateway = FridayCapabilityGateway(capabilityExecutor),
        audit = audit
    )

    @Provides @Singleton
    fun registry(
        torch: TorchTool, battery: BatteryTool, sms: SmsTool, call: CallTool,
        contacts: ContactsTool, notif: NotificationTool, loc: LocationTool,
        vol: VolumeTool, media: MediaTool, alarm: AlarmTool, timer: TimerTool,
        cal: CalendarTool, clip: ClipboardTool, file: FileTool,
        appLauncher: AppLauncherTool, url: UrlTool, shot: ScreenshotTool,
        cam: CameraTool, wifi: WifiTool, bt: BluetoothTool, http: HttpTool,
        memory: MemoryTool, goal: GoalTool, notify: NotifyTool,
        sys: SystemTool, weather: WeatherTool, news: NewsTool,
        deviceNavigation: DeviceNavigationTool, appSchema: AppSchemaTool,
        appTask: AppTaskTool, securityCapability: SecurityCapabilityTool
    ): ToolRegistry = ToolRegistry().apply {
        register(torch); register(battery); register(sms); register(call)
        register(contacts); register(notif); register(loc); register(vol)
        register(media); register(alarm); register(timer); register(cal)
        register(clip); register(file); register(appLauncher); register(url)
        register(shot); register(cam); register(wifi); register(bt)
        register(http); register(memory); register(goal); register(notify)
        register(sys); register(weather); register(news); register(deviceNavigation)
        register(appSchema); register(appTask); register(securityCapability)
    }
}
