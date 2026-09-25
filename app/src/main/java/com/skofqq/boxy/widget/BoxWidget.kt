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
import com.skofqq.boxy.data.TrafficStats
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.ServiceState
import com.skofqq.boxy.service.BoxControl
import com.skofqq.boxy.util.withAppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Home screen widget: service state, core and mode, and a Start / Stop button. */
class BoxWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                render(context, runCatching { BoxModule.state() }.getOrNull(), busy = null)
                TrafficStats.sample(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        val pending = goAsync()
        scope.launch {
            try {
                val running = runCatching { BoxModule.state().running }.getOrDefault(false)
                render(context, null, busy = if (running) R.string.status_stopping else R.string.status_starting)
                if (running) BoxControl.stop(context) else BoxControl.start(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.skofqq.boxy.widget.TOGGLE"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Re-reads the service state and redraws every widget (no-op when none is placed). */
        fun refresh(context: Context) {
            val app = context.applicationContext
            if (ids(app).isEmpty()) return
            scope.launch { render(app, runCatching { BoxModule.state() }.getOrNull(), busy = null) }
        }

        private fun ids(context: Context): IntArray =
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, BoxWidget::class.java))

        private fun render(context: Context, state: ServiceState?, busy: Int?) {
            val ids = ids(context)
            if (ids.isEmpty()) return
            val res = context.withAppLocale()
            val views = RemoteViews(context.packageName, R.layout.widget_box)
            val running = state?.running == true
            val status = res.getString(
                busy ?: when {
                    state == null -> R.string.status_checking
                    running -> R.string.status_running
                    else -> R.string.status_stopped
                },
            )
            // Separate views per colour so the launcher resolves light / dark colours itself.
            views.setTextViewText(R.id.widget_status_on, status)
            views.setTextViewText(R.id.widget_status_off, status)
            views.setViewVisibility(R.id.widget_status_on, if (running && busy == null) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_status_off, if (running && busy == null) View.GONE else View.VISIBLE)
            views.setTextViewText(
                R.id.widget_detail,
                listOfNotNull(state?.core, state?.mode).joinToString(" · ").ifEmpty { res.getString(R.string.app_name) },
            )
            views.setTextViewText(R.id.widget_stop, res.getString(R.string.action_stop))
            views.setTextViewText(R.id.widget_start, res.getString(R.string.action_start))
            val showStop = running && busy == null
            views.setViewVisibility(R.id.widget_stop, if (showStop) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_start, if (!showStop && busy == null && state != null) View.VISIBLE else View.GONE)

            val toggle = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, BoxWidget::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_stop, toggle)
            views.setOnClickPendingIntent(R.id.widget_start, toggle)
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, open)
            AppWidgetManager.getInstance(context).updateAppWidget(ids, views)
        }
    }
}
