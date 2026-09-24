package com.skofqq.boxy.ui.tools

import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.skofqq.boxy.R
import com.skofqq.boxy.root.RootFiles
import com.skofqq.boxy.ui.components.BoxyTextField
import com.skofqq.boxy.ui.components.ConfirmDialog
import com.skofqq.boxy.ui.components.HeaderAction
import com.skofqq.boxy.ui.components.SmallIconButton
import com.skofqq.boxy.ui.components.SubPageHeader
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.launch

/** Text editor for config files (sora-editor: line numbers, search, large files). */
@Composable
fun EditorScreen(contentPadding: PaddingValues, path: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf<CodeEditor?>(null) }
    var modified by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var discardDialog by remember { mutableStateOf(false) }
    val colors = Boxy.colors

    LaunchedEffect(path) { text = RootFiles.read(path) ?: "" }

    val tryBack = { if (modified) discardDialog = true else onBack() }
    BackHandler { tryBack() }

    fun save() {
        val e = editor ?: return
        scope.launch {
            val ok = RootFiles.write(path, e.text.toString())
            if (ok) modified = false
            Toast.makeText(context, if (ok) R.string.editor_saved else R.string.op_failed, Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().padding(contentPadding).imePadding()) {
        SubPageHeader(path.substringAfterLast('/') + if (modified) " •" else "", path, tryBack) {
            HeaderAction(BoxyIcons.Search, stringResource(R.string.action_search)) {
                searching = !searching
                if (!searching) editor?.searcher?.stopSearch()
            }
            HeaderAction(BoxyIcons.Save, stringResource(R.string.action_save)) { save() }
        }
        if (searching) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                BoxyTextField(
                    query,
                    { q ->
                        query = q
                        val s = editor?.searcher
                        if (q.isEmpty()) s?.stopSearch() else s?.search(q, EditorSearcher.SearchOptions(true, false))
                    },
                    stringResource(R.string.editor_search_hint),
                    Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                SmallIconButton(BoxyIcons.ExpandLess, null) { runCatching { editor?.searcher?.gotoPrevious() } }
                Spacer(Modifier.width(6.dp))
                SmallIconButton(BoxyIcons.ExpandMore, null) { runCatching { editor?.searcher?.gotoNext() } }
            }
        }
        Box(
            Modifier.fillMaxSize().padding(12.dp).clip(RoundedCornerShape(20.dp)).background(colors.card),
            contentAlignment = Alignment.Center,
        ) {
            val content = text
            if (content == null) {
                CircularProgressIndicator()
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        CodeEditor(ctx).apply {
                            typefaceText = Typeface.MONOSPACE
                            typefaceLineNumber = Typeface.MONOSPACE
                            setTextSize(13f)
                            isWordwrap = false
                            colorScheme = EditorColorScheme().apply {
                                setColor(EditorColorScheme.WHOLE_BACKGROUND, colors.card.toArgb())
                                setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, colors.card.toArgb())
                                setColor(EditorColorScheme.LINE_NUMBER, colors.text2.toArgb())
                                setColor(EditorColorScheme.LINE_NUMBER_CURRENT, colors.text.toArgb())
                                setColor(EditorColorScheme.LINE_DIVIDER, colors.outline.toArgb())
                                setColor(EditorColorScheme.TEXT_NORMAL, colors.text.toArgb())
                                setColor(EditorColorScheme.CURRENT_LINE, colors.surface2.toArgb())
                                setColor(EditorColorScheme.SELECTION_INSERT, colors.accent.toArgb())
                                setColor(EditorColorScheme.SELECTION_HANDLE, colors.accent.toArgb())
                                setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, colors.accent.copy(alpha = 0.3f).toArgb())
                                setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, colors.accent.copy(alpha = 0.25f).toArgb())
                                // Syntax colours (see ConfigLanguage): keys, strings, numbers / booleans, punctuation, comments.
                                val dark = colors.isDark
                                setColor(EditorColorScheme.KEYWORD, if (dark) 0xFF79C0FF.toInt() else 0xFF0550AE.toInt())
                                setColor(EditorColorScheme.LITERAL, if (dark) 0xFFA5D6FF.toInt() else 0xFF0A3069.toInt())
                                setColor(EditorColorScheme.FUNCTION_NAME, if (dark) 0xFFFFA657.toInt() else 0xFF953800.toInt())
                                setColor(EditorColorScheme.OPERATOR, if (dark) 0xFFFF7B72.toInt() else 0xFFCF222E.toInt())
                                setColor(EditorColorScheme.COMMENT, if (dark) 0xFF8B949E.toInt() else 0xFF6E7781.toInt())
                            }
                            val ext = path.substringAfterLast('.', "").lowercase()
                            if (ext == "json" || ext == "yaml" || ext == "yml") setEditorLanguage(ConfigLanguage(json = ext == "json"))
                            setText(content)
                            subscribeEvent(ContentChangeEvent::class.java) { _, _ -> modified = true }
                            editor = this
                        }
                    },
                    onRelease = { it.release() },
                )
            }
        }
    }

    DisposableEffect(Unit) { onDispose { editor = null } }

    if (discardDialog) {
        ConfirmDialog(
            stringResource(R.string.editor_discard_title),
            stringResource(R.string.editor_discard_body),
            stringResource(R.string.editor_discard),
            danger = true,
            onConfirm = { discardDialog = false; onBack() },
            onDismiss = { discardDialog = false },
        )
    }
}

