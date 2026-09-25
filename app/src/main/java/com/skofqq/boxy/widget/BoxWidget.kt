package com.skofqq.boxy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.skofqq.boxy.MainActivity
import com.skofqq.boxy.R
import com.skofqq.boxy.data.TrafficBucket
import com.skofqq.boxy.data.TrafficStats
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.ServiceState
import com.skofqq.boxy.service.BoxControl
import com.skofqq.boxy.util.Format
import com.skofqq.boxy.util.withAppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Detailed widget (4×1): state, core and mode, start time, today's traffic, Restart and Start / Stop.
 * Nothing ticks: it is redrawn only when something happens (start / stop from any place, a traffic sample,
 * the 30-minute system update), so it costs no battery while idle. The start time is shown instead of a
 * running uptime for the same reason.
 */
open class BoxWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                Widgets.render(context, runCatching { BoxModule.state() }.getOrNull(), busy = null)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action != Widgets.ACTION_TOGGLE && action != Widgets.ACTION_RESTART) return
        val pending = goAsync()
        scope.launch {
            try {
                val running = runCatching { BoxModule.state().running }.getOrDefault(false)
                when {
                    action == Widgets.ACTION_RESTART -> {
                        Widgets.render(context, null, busy = R.string.status_restarting)
                        BoxControl.restart(context)
                    }
                    running -> {
                        Widgets.render(context, null, busy = R.string.status_stopping)
                        BoxControl.stop(context)
                    }
                    else -> {
                        Widgets.render(context, null, busy = R.string.status_starting)
                        BoxControl.start(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Re-reads the service state and redraws every placed widget (no-op when none is placed). */
        fun refresh(context: Context) {
            val app = context.applicationContext
            if (!Widgets.any(app)) return
            scope.launch { Widgets.render(app, runCatching { BoxModule.state() }.getOrNull(), busy = null) }
        }

        /** Redraw with a state already read (traffic sampling), without another root call. */
        fun refresh(context: Context, state: ServiceState) {
            val app = context.applicationContext
            if (Widgets.any(app)) Widgets.render(app, state, busy = null)
        }
    }
}

/** Compact widget (2×1): power button, state and core. */
class BoxWidgetCompact : BoxWidget()

internal object Widgets {
    const val ACTION_TOGGLE = "com.skofqq.boxy.widget.TOGGLE"
    const val ACTION_RESTART = "com.skofqq.boxy.widget.RESTART"

    private fun ids(context: Context, cls: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls))

    fun any(context: Context) = ids(context, BoxWidget::class.java).isNotEmpty() || ids(context, BoxWidgetCompact::class.java).isNotEmpty()

    fun render(context: Context, state: ServiceState?, busy: Int?) {
        val manager = AppWidgetManager.getInstance(context)
        val res = context.withAppLocale()
        val running = state?.running == true && busy == null
        val status = res.getString(
            busy ?: when {
                state == null -> R.string.status_checking
                state.running -> R.string.status_running
                else -> R.string.status_stopped
            },
        )
        val detail = listOfNotNull(state?.core, state?.mode).joinToString(" · ").ifEmpty { res.getString(R.string.app_name) }
        val toggle = broadcast(context, BoxWidget::class.java, ACTION_TOGGLE)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        ids(context, BoxWidgetCompact::class.java).takeIf { it.isNotEmpty() }?.let { compactIds ->
            val v = RemoteViews(context.packageName, R.layout.widget_compact)
            statusViews(v, status, running)
            v.setTextViewText(R.id.widget_detail, detail)
            v.setViewVisibility(R.id.widget_power_on, if (running) View.VISIBLE else View.GONE)
            v.setViewVisibility(R.id.widget_power_off, if (running) View.GONE else View.VISIBLE)
            v.setOnClickPendingIntent(R.id.widget_power, toggle)
            v.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(compactIds, v)
        }

        ids(context, BoxWidget::class.java).takeIf { it.isNotEmpty() }?.let { fullIds ->
            val v = RemoteViews(context.packageName, R.layout.widget_box)
            statusViews(v, status, running)
            val since = state?.uptimeSec?.takeIf { running }?.let {
                res.getString(R.string.widget_since, LocalTime.now().minusSeconds(it).format(DateTimeFormatter.ofPattern("HH:mm")))
            }
            v.setTextViewText(R.id.widget_detail, listOfNotNull(detail, since).joinToString(" · "))
            val today = TrafficStats.days(context)[LocalDate.now()] ?: TrafficBucket.ZERO
            v.setTextViewText(
                R.id.widget_traffic,
                res.getString(R.string.widget_today, "↓ ${Format.bytes(res, today.down)}  ↑ ${Format.bytes(res, today.up)}"),
            )
            v.setViewVisibility(R.id.widget_stop, if (running) View.VISIBLE else View.GONE)
            v.setViewVisibility(R.id.widget_restart, if (running) View.VISIBLE else View.GONE)
            v.setViewVisibility(R.id.widget_start, if (!running && busy == null && state != null) View.VISIBLE else View.GONE)
            v.setOnClickPendingIntent(R.id.widget_stop, toggle)
            v.setOnClickPendingIntent(R.id.widget_start, toggle)
            v.setOnClickPendingIntent(R.id.widget_restart, broadcast(context, BoxWidget::class.java, ACTION_RESTART))
            v.setOnClickPendingIntent(R.id.widget_root, open)
            manager.updateAppWidget(fullIds, v)
        }
    }

    // Separate views per colour so the launcher resolves light / dark colours itself.
    private fun statusViews(v: RemoteViews, status: String, running: Boolean) {
        v.setTextViewText(R.id.widget_status_on, status)
        v.setTextViewText(R.id.widget_status_off, status)
        v.setViewVisibility(R.id.widget_status_on, if (running) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.widget_status_off, if (running) View.GONE else View.VISIBLE)
    }

    private fun broadcast(context: Context, cls: Class<*>, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, cls).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
