package com.jarvis.di

import android.content.Context
import com.jarvis.capability.accessibility.AccessibilityBackend
import com.jarvis.capability.accessibility.JarvisAccessibilityService
import com.jarvis.capability.android.AndroidIntentBackend
import com.jarvis.capability.android.PackageBackend
import com.jarvis.core.capability.CapabilityExecutor
import com.jarvis.core.capability.CapabilityPolicy
import com.jarvis.core.capability.DefaultCapabilityPolicy
import com.jarvis.core.capability.CapabilityRegistry
import com.jarvis.tools.ToolRegistry
import com.jarvis.tools.impl.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun capabilityRegistry(
        context: Context
    ): CapabilityRegistry {
        // The registry is synchronous at construction time; registration itself
        // is suspendable, so bootstrap it through a dedicated coroutine scope.
        // Backends are installed by CapabilityBootstrap below.
        return CapabilityRegistry()
    }

    @Provides @Singleton
    fun capabilityBackends(
        context: Context
    ): List<com.jarvis.core.capability.CapabilityBackend> = listOf(
        AndroidIntentBackend(context),
        PackageBackend(context),
        AccessibilityBackend(JarvisAccessibilityService.controller())
    )

    @Provides @Singleton
    fun capabilityExecutor(
        registry: CapabilityRegistry,
        backends: List<com.jarvis.core.capability.CapabilityBackend>
    ): CapabilityExecutor {
        kotlinx.coroutines.runBlocking {
            backends.forEachIndexed { index, backend ->
                registry.register(backend, priority = 100 - index)
            }
        }
        return CapabilityExecutor(
            registry = registry,
            policyProvider = CapabilityPolicy(DefaultCapabilityPolicy())
        )
    }

    @Provides @Singleton
    fun registry(
        torch: TorchTool, battery: BatteryTool, sms: SmsTool, call: CallTool,
        contacts: ContactsTool, notif: NotificationTool, loc: LocationTool,
        vol: VolumeTool, media: MediaTool, alarm: AlarmTool, timer: TimerTool,
        cal: CalendarTool, clip: ClipboardTool, file: FileTool,
        appLauncher: AppLauncherTool, url: UrlTool, shot: ScreenshotTool,
        cam: CameraTool, wifi: WifiTool, bt: BluetoothTool, http: HttpTool,
        memory: MemoryTool, goal: GoalTool, notify: NotifyTool,
        sys: SystemTool, weather: WeatherTool
    ): ToolRegistry = ToolRegistry().apply {
        register(torch); register(battery); register(sms); register(call)
        register(contacts); register(notif); register(loc); register(vol)
        register(media); register(alarm); register(timer); register(cal)
        register(clip); register(file); register(appLauncher); register(url)
        register(shot); register(cam); register(wifi); register(bt)
        register(http); register(memory); register(goal); register(notify)
        register(sys); register(weather)
    }
}
