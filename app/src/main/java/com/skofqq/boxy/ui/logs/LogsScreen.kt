package com.skofqq.boxy.ui.logs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.skofqq.boxy.R
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.RootFile
import com.skofqq.boxy.root.RootFiles
import com.skofqq.boxy.root.RootTask
import com.skofqq.boxy.ui.components.ConfirmDialog
import com.skofqq.boxy.ui.components.HeaderAction
import com.skofqq.boxy.ui.components.SubPageHeader
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.Tints
import com.skofqq.boxy.util.Format
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAIL_BYTES = 256 * 1024

/** Log files of the module (/data/adb/box/run): pick a file, read its tail, auto refresh, delete. */
@Composable
fun LogsScreen(contentPadding: PaddingValues, header: @Composable () -> Unit = {}, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    var files by remember { mutableStateOf<List<RootFile>?>(null) }
    var current by rememberSaveable { mutableStateOf<String?>(null) }
    var coreLog by remember { mutableStateOf<String?>(null) }
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var auto by rememberSaveable { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var deleteDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(reload) {
        val list = RootFiles.list(BoxModule.RUN_DIR).filter { !it.isDir && (it.name.endsWith(".log") || it.name.endsWith(".txt")) }
        files = list
        val core = BoxModule.readSetting("bin_name")
        coreLog = core?.let { "$it.log" }
        if (current == null || list.none { it.name == current }) {
            current = list.firstOrNull { it.name == "runs.log" }?.name ?: list.firstOrNull()?.name
        }
    }
    LaunchedEffect(owner, current, auto, reload) {
        val name = current ?: run { lines = emptyList(); return@LaunchedEffect }
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val text = RootFiles.tail("${BoxModule.RUN_DIR}/$name", TAIL_BYTES)
                lines = text.lines().map { RootTask.stripAnsi(it) }.dropLastWhile { it.isBlank() }
                if (!auto) break
                delay(2000)
            }
        }
    }
    LaunchedEffect(lines?.size) {
        val n = lines?.size ?: 0
        if (n > 0 && auto) listState.scrollToItem(n + 2)
    }

    val status = if (current == "runs.log" || current == coreLog) stringResource(R.string.logs_status_current) else stringResource(R.string.logs_status_available)
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = contentPadding) {
        item { header() }
        item {
            SubPageHeader(
                stringResource(R.string.logs_title),
                current?.let { stringResource(R.string.logs_current, it, status) } ?: stringResource(R.string.logs_subtitle),
                onBack,
            ) {
                HeaderAction(if (auto) BoxyIcons.Schedule else BoxyIcons.Refresh, stringResource(if (auto) R.string.logs_auto_on else R.string.logs_manual)) {
                    if (auto) {
                        auto = false
                    } else {
                        reload++
                    }
                }
                HeaderAction(BoxyIcons.Delete, stringResource(R.string.action_delete)) { if (current != null) deleteDialog = true }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(stringResource(if (auto) R.string.logs_auto_on else R.string.logs_auto), auto, Tints.green.fg) { auto = !auto }
                files?.forEach { f ->
                    Chip("${f.name} · ${Format.bytes(context, f.size)}", f.name == current, Boxy.colors.accent) { current = f.name }
                }
            }
        }
        val l = lines
        when {
            files == null || l == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            files?.isEmpty() == true -> item { Text(stringResource(R.string.logs_no_files), Modifier.padding(24.dp), color = Boxy.colors.text2) }
            else -> {
                item { Box(Modifier.padding(top = 8.dp)) }
                itemsIndexed(l, key = { i, _ -> i }) { _, line ->
                    Text(
                        line,
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Boxy.colors.card).padding(horizontal = 12.dp, vertical = 1.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = lineColor(line),
                    )
                }
            }
        }
    }

    if (deleteDialog) {
        val name = current ?: return
        ConfirmDialog(
            stringResource(R.string.logs_delete_title),
            stringResource(R.string.logs_delete_body, name, BoxModule.RUN_DIR),
            stringResource(R.string.action_delete),
            danger = true,
            onConfirm = {
                deleteDialog = false
                scope.launch {
                    if (!RootFiles.delete("${BoxModule.RUN_DIR}/$name")) Toast.makeText(context, R.string.logs_delete_failed, Toast.LENGTH_SHORT).show()
                    current = null
                    reload++
                }
            },
            onDismiss = { deleteDialog = false },
        )
    }
}

@Composable
private fun lineColor(line: String): Color = when {
    line.contains("Error", true) || line.contains("level=error") || line.contains("[E]") || line.contains("FATAL") -> Tints.red.fg
    line.contains("Warn", true) || line.contains("level=warning") -> Tints.amber.fg
    line.contains("Debug", true) -> Boxy.colors.text2
    else -> Boxy.colors.text
}

@Composable
private fun Chip(text: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(50)).background(if (active) color.copy(alpha = 0.15f) else Boxy.colors.card).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        color = if (active) color else Boxy.colors.text,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
    )
}

