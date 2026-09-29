package com.jarvis.capability
import javax.inject.Inject
import javax.inject.Singleton
private abstract class Backend(final override val id:String,private val label:String,private val operations:List<String>,private val tools:List<String> = emptyList(),private val platform:Boolean=false):CapabilityBackend{
 override fun descriptors()=listOf(CapabilityDescriptor(id,label,id,operations,tools,true,platform))
}
@Singleton class AndroidIntentBackend @Inject constructor():Backend("android.intent","Android intents",listOf("app.open","url.open"),listOf("open_app","open_url"))
@Singleton class PackageBackend @Inject constructor():Backend("android.packages","Packages",listOf("app.resolve"),listOf("open_app"))
@Singleton class AccessibilityBackend @Inject constructor():Backend("android.accessibility","Accessibility",listOf("ui.inspect","ui.click","ui.type","ui.scroll","ui.back","ui.home"),listOf("accessibility_action"),true)
@Singleton class NotificationBackend @Inject constructor():Backend("android.notifications","Notifications",listOf("notification.list","notification.reply"),listOf("list_notifications","reply_notification"))
@Singleton class ContactsBackend @Inject constructor():Backend("android.contacts","Contacts",listOf("contact.lookup"),listOf("contact_lookup"))
@Singleton class CalendarBackend @Inject constructor():Backend("android.calendar","Calendar",listOf("calendar.read"),listOf("calendar_read"))
@Singleton class TelephonyBackend @Inject constructor():Backend("android.telephony","Telephony",listOf("call.place"),listOf("make_call"))
@Singleton class SmsBackend @Inject constructor():Backend("android.sms","SMS",listOf("sms.send"),listOf("send_sms"))
@Singleton class MediaBackend @Inject constructor():Backend("android.media","Media",listOf("media.control"),listOf("media_control"))
@Singleton class CameraBackend @Inject constructor():Backend("android.camera","Camera",listOf("camera.capture"),listOf("take_photo"))
@Singleton class FileBackend @Inject constructor():Backend("android.files","Files",listOf("file.read"),listOf("file_read"))
@Singleton class NetworkBackend @Inject constructor():Backend("android.network","Network",listOf("http.get","http.post","weather.get","url.open"),listOf("http_get","http_post","get_weather","open_url"))
@Singleton class AlarmBackend @Inject constructor():Backend("android.alarms","Alarms",listOf("alarm.set","timer.set"),listOf("set_alarm","set_timer"))
@Singleton class BluetoothBackend @Inject constructor():Backend("android.bluetooth","Bluetooth",listOf("bluetooth.status"),listOf("bluetooth_status"))
@Singleton class WifiBackend @Inject constructor():Backend("android.wifi","Wi-Fi",listOf("wifi.status"),listOf("wifi_status"))
@Singleton class LocationBackend @Inject constructor():Backend("android.location","Location",listOf("location.get"),listOf("get_location"))
@Singleton class FutureAppFunctionsBackend @Inject constructor():Backend("android.appfunctions","Android AppFunctions",listOf("appfunction.discover","appfunction.execute"),platform=true)
