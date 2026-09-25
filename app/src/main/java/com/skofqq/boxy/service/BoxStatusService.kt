package com.skofqq.boxy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.skofqq.boxy.MainActivity
import com.skofqq.boxy.R
import com.skofqq.boxy.data.TrafficStats
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.ServiceState
import com.skofqq.boxy.util.Format
import com.skofqq.boxy.util.withAppLocale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Persistent notification with the Box service status and Stop / Restart actions
 * (Settings → Notifications). Polls the module every few seconds while it runs.
 */
class BoxStatusService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var busyText: Int? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLocale())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel(this)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            build(null),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        scope.launch {
            var tick = 0
            var last: Boolean? = null
            while (isActive) {
                val state = runCatching { BoxModule.state() }.getOrNull()
                if (busyText == null) notify(build(state))
                // The widget and tile follow changes made elsewhere (module action button, boot, crash).
                if (state != null && state.running != last) {
                    if (last != null) BoxControl.changed(this@BoxStatusService)
                    last = state.running
                }
                if (tick++ % 12 == 0) TrafficStats.sample(applicationContext)
                delay(5000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> act(R.string.service_status_stopping) { BoxControl.stop(applicationContext) }
            ACTION_START -> act(R.string.service_status_starting) { BoxControl.start(applicationContext) }
            ACTION_RESTART -> act(R.string.service_status_restarting) { BoxControl.restart(applicationContext) }
        }
        return START_STICKY
    }

    private fun act(text: Int, block: suspend () -> Unit) {
        busyText = text
        notify(build(null))
        scope.launch {
            block()
            busyText = null
            notify(build(BoxModule.state()))
        }
    }

    private fun notify(n: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, n)
    }

    private fun build(state: ServiceState?): Notification {
        val title = getString(
            busyText ?: when {
                state == null -> R.string.service_status_checking
                state.running -> R.string.service_status_running
                else -> R.string.service_status_stopped
            },
        )
        val text = if (state?.running == true) {
            listOfNotNull(state.core, state.mode, state.uptimeSec?.let { Format.uptime(this, it) }).joinToString(" · ")
        } else {
            null
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        if (busyText == null && state != null) {
            if (state.running) {
                b.addAction(0, getString(R.string.service_action_stop), action(ACTION_STOP))
                b.addAction(0, getString(R.string.service_action_restart), action(ACTION_RESTART))
            } else {
                b.addAction(0, getString(R.string.action_start), action(ACTION_START))
            }
        }
        return b.build()
    }

    private fun action(name: String): PendingIntent =
        PendingIntent.getService(this, name.hashCode(), Intent(this, BoxStatusService::class.java).setAction(name), PendingIntent.FLAG_IMMUTABLE)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "box_service"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.skofqq.boxy.STOP"
        private const val ACTION_START = "com.skofqq.boxy.START"
        private const val ACTION_RESTART = "com.skofqq.boxy.RESTART"

        fun createChannel(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.service_channel_name), NotificationManager.IMPORTANCE_LOW),
            )
        }

        fun sync(context: Context, enabled: Boolean) {
            val intent = Intent(context, BoxStatusService::class.java)
            if (enabled) {
                runCatching { ContextCompat.startForegroundService(context, intent) }
            } else {
                context.stopService(intent)
            }
        }
    }
}
