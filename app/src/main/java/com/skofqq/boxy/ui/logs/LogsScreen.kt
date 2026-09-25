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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.skofqq.boxy.R
import com.skofqq.boxy.ui.components.PinnedLazyPage
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
    val palette = logPalette()
    PinnedLazyPage(contentPadding, header = {
header()
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
Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(stringResource(if (auto) R.string.logs_auto_on else R.string.logs_auto), auto, Tints.green.fg) { auto = !auto }
                files?.forEach { f ->
                    Chip("${f.name} · ${Format.bytes(context, f.size)}", f.name == current, Boxy.colors.accent) { current = f.name }
                }
            }
}, state = listState) {
        val l = lines
        when {
            files == null || l == null -> item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            files?.isEmpty() == true -> item { Text(stringResource(R.string.logs_no_files), Modifier.padding(24.dp), color = Boxy.colors.text2) }
            else -> {
                item { Box(Modifier.padding(top = 8.dp)) }
                itemsIndexed(l, key = { i, _ -> i }) { _, line ->
                    Text(
                        remember(line, palette) { highlight(line, palette) },
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Boxy.colors.card).padding(horizontal = 12.dp, vertical = 1.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = Boxy.colors.text,
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

/** Colours of the log highlighter. */
private data class LogPalette(
    val time: Color,
    val info: Color,
    val warn: Color,
    val error: Color,
    val debug: Color,
    val key: Color,
    val tag: Color,
    val address: Color,
    val string: Color,
)

@Composable
private fun logPalette() = LogPalette(
    time = Boxy.colors.text2,
    info = Tints.blue.fg,
    warn = Tints.amber.fg,
    error = Tints.red.fg,
    debug = Tints.teal.fg,
    key = if (Boxy.colors.isDark) Color(0xFFCE93D8) else Color(0xFF9C27B0),
    tag = Tints.orange.fg,
    address = Tints.green.fg,
    string = if (Boxy.colors.isDark) Color(0xFF90CAF9) else Color(0xFF1A237E),
)

// Groups: 1 date/time, 2 level tag in brackets, 3 key=, 4 bare level word, 5 [tag], 6 ip[:port] / host:port, 7 "string".
private val LOG_TOKENS = Regex(
    "(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?|\\b\\d{1,2}:\\d{2}(?::\\d{2}(?:\\.\\d+)?)?(?: ?[aApP][mM])?\\b|[+-]\\d{4}(?= \\d{4}-))" +
        "|(\\[(?:Info|Warning|Warn|Error|Debug|Fatal|I|W|E|D)\\])" +
        "|\\b(time|level|msg|err|error|proxy|rule|rulePayload)=" +
        "|\\b(INFO|WARN|WARNING|ERROR|DEBUG|FATAL|TRACE|info|warning|error|debug|fatal)\\b" +
        "|(\\[[A-Za-z][\\w./ -]{0,24}\\])" +
        "|(\\b\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d+)?\\b|\\[[0-9a-fA-F:]+\\](?::\\d+)?|\\b[\\w.-]+\\.[a-z]{2,}:\\d+\\b)" +
        "|(\"[^\"]*\")",
)

private fun levelColor(word: String, p: LogPalette): Color = when (word.trim('[', ']').lowercase()) {
    "error", "e", "fatal" -> p.error
    "warn", "warning", "w" -> p.warn
    "debug", "d", "trace" -> p.debug
    else -> p.info
}

/** Token-level colouring of box.log, mihomo / sing-box / xray logs. */
private fun highlight(line: String, p: LogPalette): AnnotatedString = buildAnnotatedString {
    val lower = line.lowercase()
    val lineIsError = "[error]" in lower || "level=error" in lower || "level=fatal" in lower || " fatal " in lower || " error [" in lower
    var pos = 0
    for (m in LOG_TOKENS.findAll(line)) {
        if (m.range.first > pos) {
            val plain = line.substring(pos, m.range.first)
            if (lineIsError) withStyle(SpanStyle(color = p.error)) { append(plain) } else append(plain)
        }
        val g = m.groups
        val text = m.value
        when {
            g[1] != null -> withStyle(SpanStyle(color = p.time)) { append(text) }
            g[2] != null -> withStyle(SpanStyle(color = levelColor(text, p), fontWeight = FontWeight.Bold)) { append(text) }
            g[3] != null -> withStyle(SpanStyle(color = p.key)) { append(text) }
            g[4] != null -> withStyle(SpanStyle(color = levelColor(text, p), fontWeight = FontWeight.Bold)) { append(text) }
            g[5] != null -> withStyle(SpanStyle(color = p.tag)) { append(text) }
            g[6] != null -> withStyle(SpanStyle(color = p.address)) { append(text) }
            // mihomo quotes its timestamp: time="2026-...".
            text.length > 5 && text[1].isDigit() && text[5] == '-' -> withStyle(SpanStyle(color = p.time)) { append(text) }
            else -> withStyle(SpanStyle(color = if (lineIsError) p.error else p.string)) { append(text) }
        }
        pos = m.range.last + 1
    }
    if (pos < line.length) {
        val rest = line.substring(pos)
        if (lineIsError) withStyle(SpanStyle(color = p.error)) { append(rest) } else append(rest)
    }
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

