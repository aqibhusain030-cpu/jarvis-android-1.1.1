package com.jarvis.di

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


@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
object CapabilityModule {
    @dagger.Provides
    @javax.inject.Singleton
    fun capabilityBackends(
        androidIntent: com.jarvis.capability.AndroidIntentBackend,
        packages: com.jarvis.capability.PackageBackend,
        accessibility: com.jarvis.capability.AccessibilityBackend,
        notifications: com.jarvis.capability.NotificationBackend,
        contacts: com.jarvis.capability.ContactsBackend,
        calendar: com.jarvis.capability.CalendarBackend,
        telephony: com.jarvis.capability.TelephonyBackend,
        sms: com.jarvis.capability.SmsBackend,
        media: com.jarvis.capability.MediaBackend,
        camera: com.jarvis.capability.CameraBackend,
        files: com.jarvis.capability.FileBackend,
        network: com.jarvis.capability.NetworkBackend,
        alarms: com.jarvis.capability.AlarmBackend,
        bluetooth: com.jarvis.capability.BluetoothBackend,
        wifi: com.jarvis.capability.WifiBackend,
        location: com.jarvis.capability.LocationBackend,
        appFunctions: com.jarvis.capability.FutureAppFunctionsBackend
    ): Set<com.jarvis.capability.CapabilityBackend> = setOf(
        androidIntent, packages, accessibility, notifications, contacts, calendar,
        telephony, sms, media, camera, files, network, alarms, bluetooth, wifi,
        location, appFunctions
    )
}
