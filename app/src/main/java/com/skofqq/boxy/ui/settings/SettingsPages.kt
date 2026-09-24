package com.skofqq.boxy.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.BoxyApp
import com.skofqq.boxy.R
import com.skofqq.boxy.data.Backup
import com.skofqq.boxy.data.BackupScope
import com.skofqq.boxy.data.DEFAULT_LATENCY_TARGETS
import com.skofqq.boxy.data.LatencyTarget
import com.skofqq.boxy.data.Prefs
import com.skofqq.boxy.data.ThemeMode
import com.skofqq.boxy.net.AppRelease
import com.skofqq.boxy.net.ModuleUpdate
import com.skofqq.boxy.net.Updates
import com.skofqq.boxy.ui.components.Badge
import com.skofqq.boxy.ui.components.BoxySheet
import com.skofqq.boxy.ui.components.BoxyTextField
import com.skofqq.boxy.ui.components.ConfirmDialog
import com.skofqq.boxy.ui.components.InfoRow
import com.skofqq.boxy.ui.components.SectionCard
import com.skofqq.boxy.ui.components.SheetButtons
import com.skofqq.boxy.ui.components.SheetGroup
import com.skofqq.boxy.ui.components.SubPageHeader
import com.skofqq.boxy.ui.components.SwitchRow
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.Tints
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SystemBarsSheet(prefs: Prefs, onDismiss: () -> Unit) {
    BoxySheet(stringResource(R.string.settings_system_bars), stringResource(R.string.settings_bars_opaque_sub), onDismiss) {
        SheetGroup {
            SwitchRow(BoxyIcons.Web, stringResource(R.string.settings_bars_opaque_status), null, prefs.opaqueStatusBar) { prefs.updateOpaqueStatusBar(it) }
            SwitchRow(BoxyIcons.Web, stringResource(R.string.settings_bars_opaque_nav), null, prefs.opaqueNavBar, showDivider = false) { prefs.updateOpaqueNavBar(it) }
        }
        SheetButtons(stringResource(R.string.action_done), onDismiss)
    }
}

/** Blur, liquid glass, UI scale and true black. */
@Composable
fun AppearanceScreen(contentPadding: PaddingValues, prefs: Prefs, onBack: () -> Unit) {
    val blurSupported = Build.VERSION.SDK_INT >= 31
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SubPageHeader(stringResource(R.string.settings_appearance_more), stringResource(R.string.settings_appearance_more_sub), onBack) }
        item {
            SectionCard(stringResource(R.string.settings_glass), stringResource(R.string.settings_glass_sub)) {
                SwitchRow(
                    BoxyIcons.AutoAwesome,
                    stringResource(R.string.settings_blur_effects),
                    stringResource(if (blurSupported) R.string.settings_blur_effects_sub else R.string.settings_blur_unsupported),
                    prefs.blurEffects && blurSupported,
                    enabled = blurSupported,
                ) { prefs.updateBlurEffects(it) }
                SwitchRow(
                    BoxyIcons.Web,
                    stringResource(R.string.settings_sheet_blur),
                    stringResource(R.string.settings_sheet_blur_sub),
                    prefs.sheetBlur && blurSupported,
                    enabled = blurSupported && prefs.blurEffects,
                ) { prefs.updateSheetBlur(it) }
                SwitchRow(
                    BoxyIcons.Palette,
                    stringResource(R.string.settings_glass_translucent),
                    stringResource(R.string.settings_glass_translucent_sub),
                    prefs.glassTranslucent,
                    enabled = blurSupported && prefs.blurEffects,
                    showDivider = false,
                ) { prefs.updateGlassTranslucent(it) }
                SliderRow(
                    stringResource(R.string.settings_blur_strength),
                    stringResource(R.string.settings_blur_strength_sub),
                    prefs.blurStrength,
                    enabled = blurSupported && prefs.blurEffects,
                ) { prefs.updateBlurStrength(it) }
                SliderRow(
                    stringResource(R.string.settings_lens_strength),
                    stringResource(R.string.settings_lens_strength_sub),
                    prefs.lensStrength,
                    enabled = Build.VERSION.SDK_INT >= 33 && prefs.blurEffects,
                ) { prefs.updateLensStrength(it) }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_display), null) {
                SliderRow(
                    stringResource(R.string.settings_ui_scale, prefs.uiScale),
                    stringResource(R.string.settings_ui_scale_sub),
                    (prefs.uiScale - 80) / 40f,
                    steps = 7,
                ) { prefs.updateUiScale(80 + Math.round(it * 40 / 5f) * 5) }
                SwitchRow(
                    BoxyIcons.Moon,
                    stringResource(R.string.settings_true_black),
                    stringResource(R.string.settings_true_black_sub),
                    checked = prefs.trueBlack,
                    enabled = prefs.themeMode != ThemeMode.LIGHT,
                    showDivider = false,
                ) { prefs.updateTrueBlack(it) }
            }
        }
    }
}

@Composable
private fun SliderRow(title: String, subtitle: String, value: Float, enabled: Boolean = true, steps: Int = 0, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text.copy(alpha = if (enabled) 1f else 0.45f))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Boxy.colors.text2.copy(alpha = if (enabled) 1f else 0.45f))
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local) },
            enabled = enabled,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = Boxy.colors.accent, activeTrackColor = Boxy.colors.accent),
        )
    }
}

/** Name and URL of each latency target shown on the home page. */
@Composable
fun LatencyTargetsScreen(contentPadding: PaddingValues, prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    var targets by remember { mutableStateOf(prefs.latencyTargets) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SubPageHeader(stringResource(R.string.settings_latency_targets), stringResource(R.string.settings_latency_targets_sub), onBack) }
        targets.forEachIndexed { i, t ->
            item {
                SectionCard("#${i + 1}", null) {
                    Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoxyTextField(t.name, { v -> targets = targets.toMutableList().also { it[i] = t.copy(name = v) } }, stringResource(R.string.settings_latency_name), Modifier.fillMaxWidth())
                        BoxyTextField(t.url, { v -> targets = targets.toMutableList().also { it[i] = t.copy(url = v) } }, stringResource(R.string.settings_latency_url), Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp)) {
                SheetButtons(
                    stringResource(R.string.action_save),
                    {
                        val clean = targets.map { LatencyTarget(it.name.trim().ifBlank { it.url }, it.url.trim()) }.filter { it.url.isNotBlank() }
                        prefs.updateLatencyTargets(clean.ifEmpty { null })
                        Toast.makeText(context, R.string.saved, Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    stringResource(R.string.action_reset),
                    {
                        targets = DEFAULT_LATENCY_TARGETS
                        prefs.updateLatencyTargets(null)
                    },
                )
            }
        }
    }
}

/** Export / import module files and app preferences as a zip. */
@Composable
fun BackupScreen(contentPadding: PaddingValues, prefs: Prefs, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restoreTab by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var detected by remember { mutableStateOf<BackupScope?>(null) }
    var detectFailed by remember { mutableStateOf(false) }
    var scopeChoice by remember { mutableStateOf(BackupScope.BOTH) }
    var confirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val ok = Backup.export(context, uri, prefs)
                busy = false
                Toast.makeText(context, if (ok) R.string.backup_export_ok else R.string.backup_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pickedUri = uri
            scope.launch {
                val d = Backup.detect(context, uri)
                detected = d
                detectFailed = d == null
                if (d != null) scopeChoice = d
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SubPageHeader(stringResource(R.string.settings_backup), stringResource(R.string.settings_backup_sub), onBack) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(50)).background(Boxy.colors.card).padding(4.dp)) {
                listOf(false to R.string.backup_tab_backup, true to R.string.backup_tab_restore).forEach { (r, label) ->
                    val active = restoreTab == r
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (active) Boxy.colors.accent.copy(alpha = 0.15f) else Boxy.colors.card)
                            .clickable { restoreTab = r }.padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(label), color = if (active) Boxy.colors.accent else Boxy.colors.text2, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (!restoreTab) {
            item {
                SectionCard(stringResource(R.string.backup_tab_backup), stringResource(R.string.backup_export_desc)) {
                    Box(Modifier.padding(horizontal = 18.dp)) {
                        SheetButtons(stringResource(R.string.backup_export), {
                            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                            exportLauncher.launch("boxy-backup-$stamp.zip")
                        })
                    }
                }
            }
        } else {
            item {
                SectionCard(stringResource(R.string.backup_file_title), null) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(16.dp)).background(Boxy.colors.surface2).clickable {
                        pickLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }.padding(16.dp)) {
                        val pickedName = remember(pickedUri) {
                            pickedUri?.let { uri ->
                                runCatching {
                                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                                        if (c.moveToFirst()) c.getString(0) else null
                                    }
                                }.getOrNull() ?: uri.lastPathSegment
                            }
                        }
                        Text(pickedName ?: stringResource(R.string.backup_file_pick), color = Boxy.colors.text)
                        when {
                            detectFailed -> Text(stringResource(R.string.backup_detect_failed), color = Tints.red.fg, style = MaterialTheme.typography.bodySmall)
                            detected != null -> Text(stringResource(R.string.backup_detected, scopeName(detected!!)), color = Boxy.colors.text2, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (detected != null) {
                item {
                    SectionCard(stringResource(R.string.backup_scope_title), null) {
                        val options = when (detected) {
                            BackupScope.BOTH -> listOf(BackupScope.BOTH, BackupScope.MODULES, BackupScope.APPS)
                            else -> listOf(detected!!)
                        }
                        options.forEach { s ->
                            Row(
                                Modifier.fillMaxWidth().clickable { scopeChoice = s }.padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(scopeLabel(s), Modifier.weight(1f), color = Boxy.colors.text)
                                if (s == scopeChoice) Badge("✓", Tints.blue)
                            }
                        }
                        Box(Modifier.padding(horizontal = 18.dp)) {
                            SheetButtons(stringResource(R.string.backup_restore), { confirm = true })
                        }
                    }
                }
            }
        }
        if (busy) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }

    if (confirm) {
        ConfirmDialog(
            stringResource(R.string.backup_restore),
            stringResource(R.string.backup_restore_confirm, scopeName(scopeChoice)),
            stringResource(R.string.backup_continue),
            onConfirm = {
                confirm = false
                val uri = pickedUri ?: return@ConfirmDialog
                busy = true
                scope.launch {
                    val ok = Backup.restore(context, uri, scopeChoice, prefs)
                    busy = false
                    Toast.makeText(context, if (ok) R.string.backup_restore_ok else R.string.backup_failed, Toast.LENGTH_SHORT).show()
                    if (ok && scopeChoice != BackupScope.MODULES) {
                        (context.applicationContext as BoxyApp).reloadPrefs()
                        (context as? Activity)?.recreate()
                    }
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun scopeName(s: BackupScope): String = stringResource(
    when (s) {
        BackupScope.BOTH -> R.string.backup_scope_both_name
        BackupScope.MODULES -> R.string.backup_scope_modules
        BackupScope.APPS -> R.string.backup_scope_apps
    },
)

@Composable
private fun scopeLabel(s: BackupScope): String = stringResource(
    when (s) {
        BackupScope.BOTH -> R.string.backup_scope_both
        BackupScope.MODULES -> R.string.backup_scope_modules_only
        BackupScope.APPS -> R.string.backup_scope_apps_only
    },
)

private data class License(val name: String, val license: String, val url: String)

private val LICENSES = listOf(
    License("Jetpack Compose, AndroidX", "Apache License 2.0", "https://developer.android.com/jetpack/androidx"),
    License("Kotlin, kotlinx.coroutines", "Apache License 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    License("libsu (topjohnwu)", "Apache License 2.0", "https://github.com/topjohnwu/libsu"),
    License("Sora Editor (Rosemoe)", "LGPL-2.1", "https://github.com/Rosemoe/sora-editor"),
    License("Material Icons (Google)", "Apache License 2.0", "https://fonts.google.com/icons"),
    License("Eva Icons (Akveo)", "MIT License", "https://github.com/akveo/eva-icons"),
    License("Box for Root module (taamarin)", "GPL-3.0", "https://github.com/taamarin/box_for_magisk"),
    License("BFR design (boxproxy)", "Design reference", "https://github.com/boxproxy"),
)

@Composable
fun LicensesScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SubPageHeader(stringResource(R.string.settings_licenses), stringResource(R.string.settings_licenses_sub), onBack) }
        item {
            SectionCard(null) {
                LICENSES.forEach { l ->
                    Column(
                        Modifier.fillMaxWidth().clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(l.url))) } }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                    ) {
                        Text(l.name, style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text)
                        Text(l.license, style = MaterialTheme.typography.bodySmall, color = Boxy.colors.text2)
                    }
                }
            }
        }
    }
}

/** Latest app builds from GitHub releases. */
@Composable
fun AppUpdateSheet(currentVersion: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var releases by remember { mutableStateOf<List<AppRelease>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val r = Updates.appReleases()
        if (r == null) failed = true else releases = r
    }
    BoxySheet(stringResource(R.string.app_name), stringResource(R.string.about_current_version, currentVersion), onDismiss) {
        val list = releases
        when {
            failed -> Text(stringResource(R.string.about_check_failed), color = Tints.red.fg)
            list == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 12.dp))
                Text(stringResource(R.string.about_checking), color = Boxy.colors.text2)
            }
            list.isEmpty() -> Text(stringResource(R.string.about_no_releases), color = Boxy.colors.text2)
            else -> {
                val stable = list.firstOrNull { !it.prerelease }
                val pre = list.firstOrNull { it.prerelease }
                val newest = listOfNotNull(stable, pre).maxWithOrNull { a, b -> if (Updates.isNewer(a.version, b.version)) 1 else -1 }
                val hasUpdate = newest != null && Updates.isNewer(newest.version, currentVersion)
                Text(
                    stringResource(if (hasUpdate) R.string.about_update_available else R.string.about_latest),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasUpdate) Tints.green.fg else Boxy.colors.text,
                )
                Spacer(Modifier.height(10.dp))
                listOfNotNull(stable, pre).forEach { r ->
                    SheetGroup(Modifier.padding(bottom = 10.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.version, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text)
                            Badge(stringResource(if (r.prerelease) R.string.about_prerelease else R.string.about_stable), if (r.prerelease) Tints.amber else Tints.green)
                        }
                        InfoRow(stringResource(R.string.about_date), r.date)
                        r.commit?.let { InfoRow(stringResource(R.string.about_commit), it) }
                        if (r.notes.isNotBlank()) {
                            Text(
                                r.notes,
                                Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable { expanded = !expanded },
                                style = MaterialTheme.typography.bodySmall,
                                color = Boxy.colors.text2,
                                maxLines = if (expanded) Int.MAX_VALUE else 6,
                            )
                        }
                        if (r.prerelease) {
                            Text(stringResource(R.string.about_prerelease_warning), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = Tints.amber.fg)
                        }
                        Row(Modifier.padding(horizontal = 16.dp)) {
                            SheetButtons(
                                stringResource(if (r.apkUrl != null) R.string.about_download_app else R.string.about_view_browser),
                                { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.apkUrl ?: r.pageUrl))) } },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/** Module version and the update offered by its updateJson. */
@Composable
fun ModuleUpdateSheet(version: String?, installed: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var result by remember { mutableStateOf<Pair<ModuleUpdate?, Long>?>(null) }
    var checked by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        result = Updates.moduleUpdate()
        checked = true
    }
    BoxySheet(stringResource(R.string.about_module), stringResource(R.string.about_current_version, version ?: "—"), onDismiss) {
        when {
            !installed -> Text(stringResource(R.string.settings_module_not_installed), color = Tints.red.fg)
            !checked -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.padding(end = 12.dp))
                Text(stringResource(R.string.about_checking), color = Boxy.colors.text2)
            }
            result == null -> Text(stringResource(R.string.about_check_failed), color = Tints.red.fg)
            result?.first == null -> Text(stringResource(R.string.about_latest), style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text)
            else -> {
                val u = result!!.first!!
                SheetGroup {
                    InfoRow(stringResource(R.string.about_update_available), u.version, Tints.green.fg)
                    InfoRow("versionCode", u.versionCode.toString())
                }
                u.changelogUrl?.let { url ->
                    SheetButtons(stringResource(R.string.about_view_browser), { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } })
                }
                u.zipUrl?.let { url ->
                    SheetButtons(stringResource(R.string.about_download_module), { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } })
                }
            }
        }
    }
}
