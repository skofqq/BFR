package com.skofqq.boxy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.Environment
import com.skofqq.boxy.root.ServiceState
import com.skofqq.boxy.ui.components.PageHeader
import com.skofqq.boxy.ui.theme.Boxy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Busy { NONE, STARTING, STOPPING }

@Composable
fun HomeScreen(contentPadding: PaddingValues) {
    var env by remember { mutableStateOf<Environment?>(null) }
    var state by remember { mutableStateOf<ServiceState?>(null) }
    var busy by remember { mutableStateOf(Busy.NONE) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshKey) {
        env = BoxModule.environment()
        while (env == Environment.READY) {
            if (busy == Busy.NONE) state = BoxModule.state()
            delay(3000)
        }
    }

    fun act(kind: Busy, block: suspend () -> Unit) {
        busy = kind
        scope.launch {
            block()
            state = BoxModule.state()
            busy = Busy.NONE
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item { PageHeader(stringResource(R.string.home_title), stringResource(R.string.home_subtitle)) }
        item {
            when (val e = env) {
                null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                Environment.READY -> ServiceCard(state, busy, onStart = { act(Busy.STARTING) { BoxModule.start() } },
                    onStop = { act(Busy.STOPPING) { BoxModule.stop() } },
                    onRestart = { act(Busy.STARTING) { BoxModule.restart() } })
                else -> EnvironmentCard(e) { refreshKey++ }
            }
        }
    }
}

@Composable
private fun ServiceCard(
    state: ServiceState?,
    busy: Busy,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    val colors = Boxy.colors
    val running = state?.running == true
    val (label, labelColor) = when {
        busy == Busy.STARTING -> stringResource(R.string.status_starting) to colors.text
        busy == Busy.STOPPING -> stringResource(R.string.status_stopping) to colors.text
        state == null -> stringResource(R.string.status_checking) to colors.text
        running -> stringResource(R.string.status_running) to Color(0xFF2EA043)
        else -> stringResource(R.string.status_stopped) to colors.text
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(colors.card).padding(18.dp),
    ) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_service_status), style = MaterialTheme.typography.titleMedium, color = colors.text2)
                Text(label, style = MaterialTheme.typography.headlineMedium, color = labelColor)
                Spacer(Modifier.height(8.dp))
                val hint = if (running && state.pid != null) stringResource(R.string.home_pid, state.pid) else stringResource(R.string.home_tap_start)
                Text(
                    hint,
                    Modifier.clip(RoundedCornerShape(50)).background(colors.surface2).padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.text,
                )
            }
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(colors.surface2).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InfoLine(stringResource(R.string.home_core), state?.core)
                InfoLine(stringResource(R.string.home_mode), state?.mode)
                InfoLine(
                    stringResource(R.string.home_ipv6),
                    state?.ipv6?.let { stringResource(if (it) R.string.common_on else R.string.common_off) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val idle = busy == Busy.NONE
            ActionPill(stringResource(R.string.action_start), Color(0xFF2EA043), idle && !running, Modifier.weight(1f), onStart)
            ActionPill(stringResource(R.string.action_stop), Color(0xFFD1242F), idle && running, Modifier.weight(1f), onStop)
            ActionPill(stringResource(R.string.action_restart), Color(0xFFD4A72C), idle && running, Modifier.weight(1f), onRestart)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall, color = Boxy.colors.text2)
        Text(value ?: stringResource(R.string.common_dash), style = MaterialTheme.typography.labelLarge, color = Boxy.colors.text)
    }
}

@Composable
private fun ActionPill(text: String, tint: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tint.copy(alpha = if (enabled) 0.16f else 0.07f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = tint.copy(alpha = if (enabled) 1f else 0.45f), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EnvironmentCard(env: Environment, onRetry: () -> Unit) {
    val (title, body) = when (env) {
        Environment.NO_ROOT -> R.string.env_root_title to R.string.env_root_body
        Environment.NO_MODULE -> R.string.env_module_title to R.string.env_module_body
        else -> R.string.env_scripts_title to R.string.env_scripts_body
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(28.dp)).background(Boxy.colors.card).padding(18.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = Boxy.colors.text)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Boxy.colors.text2)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}
