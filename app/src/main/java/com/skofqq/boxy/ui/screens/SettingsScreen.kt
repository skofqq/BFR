package com.skofqq.boxy.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.data.AppLanguage
import com.skofqq.boxy.data.NavExtra
import com.skofqq.boxy.data.Prefs
import com.skofqq.boxy.data.ThemeMode
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.ui.components.Choice
import com.skofqq.boxy.ui.components.ChoiceDialog
import com.skofqq.boxy.ui.components.PageHeader
import com.skofqq.boxy.ui.components.SectionCard
import com.skofqq.boxy.ui.components.SettingsRow
import com.skofqq.boxy.ui.components.SwitchRow
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons

const val GITHUB_URL = "https://github.com/skofqq/BFR"

@Composable
fun SettingsScreen(contentPadding: PaddingValues, prefs: Prefs) {
    val context = LocalContext.current
    var themeDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var moduleVersion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { moduleVersion = runCatching { BoxModule.state().moduleVersion }.getOrNull() }

    val themeChoices = buildList {
        add(Choice(ThemeMode.LIGHT, stringResource(R.string.theme_light), stringResource(R.string.theme_light_desc)))
        add(Choice(ThemeMode.DARK, stringResource(R.string.theme_dark), stringResource(R.string.theme_dark_desc)))
        add(Choice(ThemeMode.SYSTEM, stringResource(R.string.theme_system), stringResource(R.string.theme_system_desc)))
        if (Build.VERSION.SDK_INT >= 31) {
            add(Choice(ThemeMode.MATERIAL, stringResource(R.string.theme_material), stringResource(R.string.theme_material_desc)))
        }
    }
    val languageChoices = listOf(
        Choice(AppLanguage.SYSTEM, stringResource(R.string.lang_system)),
        Choice(AppLanguage.ENGLISH, "English"),
        Choice(AppLanguage.RUSSIAN, "Русский"),
    )
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle)) }
        item {
            SectionCard(stringResource(R.string.settings_appearance), stringResource(R.string.settings_appearance_sub)) {
                SettingsRow(
                    BoxyIcons.Palette,
                    stringResource(R.string.settings_theme),
                    themeChoices.firstOrNull { it.value == prefs.themeMode }?.title,
                ) { themeDialog = true }
                SwitchRow(
                    BoxyIcons.Moon,
                    stringResource(R.string.settings_true_black),
                    stringResource(R.string.settings_true_black_sub),
                    checked = prefs.trueBlack,
                    enabled = prefs.themeMode != ThemeMode.LIGHT,
                    onCheckedChange = prefs::updateTrueBlack,
                )
                SettingsRow(
                    BoxyIcons.Translate,
                    stringResource(R.string.settings_language),
                    languageChoices.first { it.value == prefs.language }.title,
                    showDivider = false,
                ) { languageDialog = true }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_navigation), stringResource(R.string.settings_navigation_sub)) {
                SwitchRow(
                    BoxyIcons.Apps,
                    stringResource(R.string.tab_apps),
                    stringResource(R.string.nav_apps_desc),
                    checked = prefs.navExtra == NavExtra.APPS,
                ) { prefs.setNavPage(NavExtra.APPS, it) }
                SwitchRow(
                    BoxyIcons.Description,
                    stringResource(R.string.tab_logs),
                    stringResource(R.string.nav_logs_desc),
                    checked = prefs.navExtra == NavExtra.LOGS,
                    showDivider = false,
                ) { prefs.setNavPage(NavExtra.LOGS, it) }
            }
        }
        item {
            SectionCard(stringResource(R.string.settings_about), stringResource(R.string.settings_about_sub)) {
                SettingsRow(BoxyIcons.Info, stringResource(R.string.settings_version), versionName) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                }
                SettingsRow(BoxyIcons.Tune, stringResource(R.string.settings_module_version), moduleVersion ?: stringResource(R.string.common_dash)) {}
                SettingsRow(BoxyIcons.Build, stringResource(R.string.settings_author), "skofqq") {}
                SettingsRow(BoxyIcons.Router, "GitHub", GITHUB_URL.removePrefix("https://"), showDivider = false) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                }
            }
        }
        item {
            Text(
                stringResource(R.string.settings_credits),
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Boxy.colors.text2,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (themeDialog) {
        ChoiceDialog(stringResource(R.string.settings_theme), themeChoices, prefs.themeMode, onSelect = {
            prefs.updateTheme(it)
            themeDialog = false
        }, onDismiss = { themeDialog = false })
    }
    if (languageDialog) {
        ChoiceDialog(stringResource(R.string.settings_language), languageChoices, prefs.language, onSelect = {
            languageDialog = false
            if (it != prefs.language) {
                prefs.updateLanguage(it)
                (context as? Activity)?.recreate()
            }
        }, onDismiss = { languageDialog = false })
    }
}
