package com.jarvis.autonomy

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.jarvis.logging.JarvisLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Periodic and event-based trigger dispatcher. */
@Singleton
class TriggerManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger,
    private val scope: CoroutineScope
) {
    private val observers = mutableListOf<(TriggerEvent) -> Unit>()
    fun register(o: (TriggerEvent) -> Unit) { observers += o }

    fun startPolling(periodMs: Long) {
        scope.launch {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            while (scope.isActive) {
                val pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val charging = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
                emit(TriggerEvent.Battery(pct, charging))
                delay(periodMs)
            }
        }
    }
    private fun emit(e: TriggerEvent) { observers.forEach { it(e) } }

    sealed class TriggerEvent {
        data class Battery(val percent: Int, val charging: Boolean) : TriggerEvent()
        data class Notification(val pkg: String, val title: String, val text: String) : TriggerEvent()
        data class SmsReceived(val number: String, val body: String) : TriggerEvent()
        data class CallReceived(val number: String) : TriggerEvent()
        data object Connectivity : TriggerEvent()
    }
    companion object { private const val TAG = "TriggerManager" }
}
