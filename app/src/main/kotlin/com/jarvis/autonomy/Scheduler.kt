package com.jarvis.autonomy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jarvis.logging.JarvisLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager wrapper for one-shot and repeating goal triggers.
 * [AlarmReceiver] resolves [GoalEngine] via Hilt entry point and resumes/runs goals.
 */
@Singleton
class Scheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val logger: JarvisLogger
) {
    private val am: AlarmManager
        get() = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleOnce(atMillis: Long, requestCode: Int, payload: String): Boolean {
        if (atMillis <= System.currentTimeMillis()) {
            logger.w(TAG, "refusing past alarm rc=$requestCode at=$atMillis")
            return false
        }
        if (payload.length > 512 || payload.any { it == '\u0000' }) {
            logger.w(TAG, "refusing invalid alarm payload rc=$requestCode")
            return false
        }
        if (!am.canScheduleExactAlarms()) {
            logger.w(TAG, "exact alarm permission unavailable rc=$requestCode")
            return false
        }
        am.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, atMillis, pending(requestCode, payload)
        )
        logger.i(TAG, "scheduled once at $atMillis rc=$requestCode")
        return true
    }

    fun scheduleRepeating(intervalMs: Long, requestCode: Int, payload: String) {
        if (intervalMs < 15 * 60 * 1000L || intervalMs > 365L * 24 * 60 * 60 * 1000L) {
            logger.w(TAG, "refusing invalid repeating interval rc=$requestCode ms=$intervalMs")
            return
        }
        if (payload.length > 512 || payload.any { it == '\u0000' }) {
            logger.w(TAG, "refusing invalid repeating payload rc=$requestCode")
            return
        }
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pending(requestCode, payload)
        )
        logger.i(TAG, "scheduled repeating every ${intervalMs}ms rc=$requestCode")
    }

    fun cancel(requestCode: Int) {
        am.cancel(pending(requestCode, ""))
    }

    private fun pending(rc: Int, payload: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).putExtra(EXTRA_PAYLOAD, payload)
        return PendingIntent.getBroadcast(
            ctx, rc, i,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    class AlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: return
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val entry = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        SchedulerEntryPoint::class.java
                    )
                    entry.logger().i(TAG, "Alarm fired payload=$payload")
                    // payload format: "goal:<id>" or "resume"
                    when {
                        payload.startsWith("goal:") -> {
                            val id = payload.removePrefix("goal:")
                            entry.goalEngine().resume(id)
                        }
                        payload == "resume" -> entry.goalEngine().resumePersisted()
                        else -> entry.logger().w(TAG, "unknown alarm payload: $payload")
                    }
                } catch (t: Throwable) {
                    entryLogger(context).e(TAG, "alarm execution failed", t)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private fun entryLogger(context: Context): JarvisLogger =
        EntryPointAccessors.fromApplication(context.applicationContext, SchedulerEntryPoint::class.java).logger()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SchedulerEntryPoint {
        fun goalEngine(): GoalEngine
        fun logger(): JarvisLogger
    }

    companion object {
        private const val TAG = "Scheduler"
        const val EXTRA_PAYLOAD = "payload"
    }
}
