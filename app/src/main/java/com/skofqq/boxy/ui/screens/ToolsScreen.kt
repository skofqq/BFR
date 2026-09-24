package com.skofqq.boxy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.data.NavExtra
import com.skofqq.boxy.ui.components.PageHeader
import com.skofqq.boxy.ui.components.SectionCard
import com.skofqq.boxy.ui.components.SettingsRow
import com.skofqq.boxy.ui.theme.BoxyIcons

/**
 * Tools hub. Apps and Logs appear here only while they are not in the bottom bar,
 * so every page stays reachable whatever the navigation settings are.
 */
@Composable
fun ToolsScreen(contentPadding: PaddingValues, navExtra: NavExtra, onOpenPage: (NavExtra) -> Unit) {
    val context = LocalContext.current
    val soon = stringResource(R.string.coming_soon)
    val notReady = { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { PageHeader(stringResource(R.string.tools_title), stringResource(R.string.tools_subtitle)) }
        item {
            SectionCard(stringResource(R.string.tools_config), stringResource(R.string.tools_config_sub)) {
                SettingsRow(BoxyIcons.Description, stringResource(R.string.tools_row_manage), stringResource(R.string.tools_row_manage_sub), onClick = notReady)
                SettingsRow(BoxyIcons.Check, stringResource(R.string.tools_row_select), stringResource(R.string.tools_row_select_sub), showDivider = false, onClick = notReady)
            }
        }
        item {
            SectionCard(stringResource(R.string.tools_network), stringResource(R.string.tools_network_sub)) {
                SettingsRow(BoxyIcons.Router, stringResource(R.string.tools_row_open), stringResource(R.string.tools_row_open_sub), showDivider = false, onClick = notReady)
            }
        }
        item {
            SectionCard(stringResource(R.string.tools_update), stringResource(R.string.tools_update_sub)) {
                SettingsRow(BoxyIcons.Download, stringResource(R.string.tools_update), stringResource(R.string.tools_update_sub), showDivider = false, onClick = notReady)
            }
        }
        val hidden = listOf(NavExtra.APPS, NavExtra.LOGS).filter { it != navExtra }
        if (hidden.isNotEmpty()) {
            item {
                SectionCard(stringResource(R.string.tools_more), stringResource(R.string.tools_more_sub)) {
                    hidden.forEachIndexed { i, page ->
                        val last = i == hidden.lastIndex
                        when (page) {
                            NavExtra.APPS -> SettingsRow(BoxyIcons.Apps, stringResource(R.string.tab_apps), stringResource(R.string.tools_row_apps_sub), showDivider = !last) { onOpenPage(page) }
                            else -> SettingsRow(BoxyIcons.Description, stringResource(R.string.tab_logs), stringResource(R.string.tools_row_logs_sub), showDivider = !last) { onOpenPage(page) }
                        }
                    }
                }
            }
        }
    }
}
