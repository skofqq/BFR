package com.skofqq.boxy.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.skofqq.boxy.data.TrafficStats
import com.skofqq.boxy.service.BoxControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Start / stop by time of day on chosen weekdays. Times are minutes after midnight, -1 = off. */
data class Schedule(
    val enabled: Boolean,
    val startAt: Int,
    val stopAt: Int,
    /** Bit 0 = Monday … bit 6 = Sunday. */
    val days: Int,
)

object Automation {
    const val ACTION_START = "com.skofqq.boxy.action.START"
    const val ACTION_STOP = "com.skofqq.boxy.action.STOP"
    const val ACTION_RESTART = "com.skofqq.boxy.action.RESTART"
    const val ACTION_TOGGLE = "com.skofqq.boxy.action.TOGGLE"
    const val ACTION_SET_CONFIG = "com.skofqq.boxy.action.SET_CONFIG"
    const val EXTRA_NAME = "name"
    val ACTIONS = listOf(ACTION_START, ACTION_STOP, ACTION_RESTART, ACTION_TOGGLE, ACTION_SET_CONFIG)

    private const val SCHEDULED_START = "com.skofqq.boxy.schedule.START"
    private const val SCHEDULED_STOP = "com.skofqq.boxy.schedule.STOP"
    private const val TAG = "BoxyAutomation"

    private fun prefs(context: Context) = context.getSharedPreferences("boxy", Context.MODE_PRIVATE)

    fun intentsAllowed(context: Context) = prefs(context).getBoolean("automation_intents", false)

    fun setIntentsAllowed(context: Context, v: Boolean) = prefs(context).edit().putBoolean("automation_intents", v).apply()

    fun schedule(context: Context): Schedule {
        val sp = prefs(context)
        return Schedule(
            sp.getBoolean("schedule_enabled", false),
            sp.getInt("schedule_start", 8 * 60),
            sp.getInt("schedule_stop", 23 * 60),
            sp.getInt("schedule_days", 0b1111111),
        )
    }

    fun setSchedule(context: Context, s: Schedule) {
        prefs(context).edit()
            .putBoolean("schedule_enabled", s.enabled)
            .putInt("schedule_start", s.startAt)
            .putInt("schedule_stop", s.stopAt)
            .putInt("schedule_days", s.days)
            .apply()
        reschedule(context)
    }

    /** Next moment [minutes] after midnight falls on an allowed weekday, strictly after now. */
    fun next(minutes: Int, days: Int, now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        if (minutes < 0 || days and 0b1111111 == 0) return null
        val time = LocalTime.of(minutes / 60, minutes % 60)
        for (i in 0..7) {
            val date: LocalDate = now.toLocalDate().plusDays(i.toLong())
            val at = date.atTime(time)
            if (at.isAfter(now) && days and (1 shl (date.dayOfWeek.value - 1)) != 0) return at
        }
        return null
    }

    fun canExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
    }

    /** Sets (or clears) the alarms for the next scheduled start and stop. */
    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val s = schedule(context)
        listOf(SCHEDULED_START to s.startAt, SCHEDULED_STOP to s.stopAt).forEach { (action, minutes) ->
            val pi = PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, ScheduleReceiver::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            am.cancel(pi)
            val at = if (s.enabled) next(minutes, s.days) else null
            if (at != null) {
                val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (canExact(context)) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                } else {
                    // Without exact alarms: a 10-minute window instead of the hour setAndAllowWhileIdle may take.
                    am.setWindow(AlarmManager.RTC_WAKEUP, millis, 10 * 60 * 1000L, pi)
                }
            }
        }
    }

    private fun run(receiver: BroadcastReceiver, context: Context, block: suspend (Context) -> Unit) {
        val pending = receiver.goAsync()
        val app = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block(app)
            } finally {
                pending.finish()
            }
        }
    }

    /** Intents from Tasker / MacroDroid / adb. Ignored until allowed in Tools → Automation. */
    class CommandReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action !in ACTIONS) return
            if (!intentsAllowed(context)) {
                Log.w(TAG, "Ignored $action: intents are turned off in Boxy")
                return
            }
            run(this, context) { app ->
                when (action) {
                    ACTION_START -> BoxControl.start(app)
                    ACTION_STOP -> BoxControl.stop(app)
                    ACTION_RESTART -> BoxControl.restart(app)
                    ACTION_TOGGLE -> BoxControl.toggle(app)
                    ACTION_SET_CONFIG -> intent.getStringExtra(EXTRA_NAME)?.takeIf { it.isNotBlank() && '/' !in it }
                        ?.let { BoxControl.setConfig(app, it) }
                }
            }
        }
    }

    /** Alarms of the schedule, plus re-arming after boot, update or a clock change. */
    class ScheduleReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                SCHEDULED_START -> run(this, context) { app -> BoxControl.start(app); reschedule(app) }
                SCHEDULED_STOP -> run(this, context) { app -> BoxControl.stop(app); reschedule(app) }
                else -> {
                    reschedule(context)
                    TrafficStats.schedule(context)
                }
            }
        }
    }
}
