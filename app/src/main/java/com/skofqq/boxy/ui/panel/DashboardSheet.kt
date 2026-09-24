package com.skofqq.boxy.ui.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.net.Dashboard
import com.skofqq.boxy.net.Dashboards
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.ui.components.Badge
import com.skofqq.boxy.ui.components.BoxySheet
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.Tints
import kotlinx.coroutines.launch

/** Pick the core's web UI: Zashboard or MetaCubeXD. The chosen one is downloaded and put in place. */
@Composable
fun DashboardSheet(onDismiss: () -> Unit, onInstalled: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var core by remember { mutableStateOf("clash") }
    var current by remember { mutableStateOf<Dashboard?>(null) }
    var checked by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf<Dashboard?>(null) }
    var progress by remember { mutableStateOf(-1f) }
    var lastLog by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        core = BoxModule.readSetting("bin_name") ?: "clash"
        current = Dashboards.installed(core)
        checked = true
    }

    BoxySheet(stringResource(R.string.dashboard_title), stringResource(R.string.dashboard_sub), { if (installing == null) onDismiss() }) {
        if (core != "clash" && core != "sing-box") {
            Text(stringResource(R.string.dashboard_unsupported, core), color = Boxy.colors.text2)
            return@BoxySheet
        }
        Dashboard.entries.forEach { d ->
            val isCurrent = d == current
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (isCurrent) Boxy.colors.accent.copy(alpha = 0.14f) else Boxy.colors.card)
                    .clickable(enabled = installing == null) {
                        installing = d
                        result = null
                        progress = -1f
                        scope.launch {
                            val ok = Dashboards.install(context.cacheDir, core, d, { progress = it }, { lastLog = it })
                            result = ok
                            installing = null
                            if (ok) {
                                current = d
                                onInstalled()
                            }
                        }
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(d.title, style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text)
                    Text(
                        stringResource(if (d == Dashboard.ZASHBOARD) R.string.dashboard_zashboard_desc else R.string.dashboard_metacubexd_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Boxy.colors.text2,
                    )
                }
                when {
                    installing == d -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    isCurrent -> Badge(stringResource(R.string.dashboard_installed), Tints.green)
                    else -> Icon(BoxyIcons.Download, null, tint = Boxy.colors.accent)
                }
            }
        }
        if (!checked) Text(stringResource(R.string.about_checking), color = Boxy.colors.text2)
        if (installing != null) {
            Spacer(Modifier.height(10.dp))
            if (progress >= 0f) {
                LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth(), color = Boxy.colors.accent)
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Boxy.colors.accent)
            }
            Spacer(Modifier.height(6.dp))
            Text(lastLog, style = MaterialTheme.typography.bodySmall, color = Boxy.colors.text2, maxLines = 2)
        }
        result?.let { ok ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ok) BoxyIcons.Check else BoxyIcons.Close, null, tint = if (ok) Tints.green.fg else Tints.red.fg)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ok) stringResource(R.string.dashboard_done) else stringResource(R.string.dashboard_failed) + "\n" + lastLog,
                    color = if (ok) Boxy.colors.text else Tints.red.fg,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
