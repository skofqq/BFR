package com.skofqq.boxy.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.data.HomeSection
import com.skofqq.boxy.data.MetricCard
import com.skofqq.boxy.data.Prefs
import com.skofqq.boxy.net.GeoIp
import com.skofqq.boxy.net.flagEmoji
import com.skofqq.boxy.root.CORES
import com.skofqq.boxy.root.IpsetStatus
import com.skofqq.boxy.root.NETWORK_MODES
import com.skofqq.boxy.ui.components.BoxySheet
import com.skofqq.boxy.ui.components.InfoRow
import com.skofqq.boxy.ui.components.OptionRow
import com.skofqq.boxy.ui.components.SheetButtons
import com.skofqq.boxy.ui.components.SheetGroup
import com.skofqq.boxy.ui.components.SmallIconButton
import com.skofqq.boxy.ui.components.SwitchRow
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.Tints
import com.skofqq.boxy.util.Format

@Composable
fun HomeSheets(sheet: HomeSheet, vm: HomeViewModel, prefs: Prefs, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dash = stringResource(R.string.common_dash)
    when (sheet) {
        HomeSheet.NONE -> Unit

        HomeSheet.CORE -> BoxySheet(stringResource(R.string.sheet_core_title), stringResource(R.string.sheet_core_subtitle), onDismiss) {
            CORES.forEach { core ->
                val current = vm.state?.core == core
                OptionRow(core, if (current) stringResource(R.string.sheet_current) else null, current) {
                    vm.setSetting("bin_name", core)
                    onDismiss()
                }
            }
        }

        HomeSheet.MODE -> BoxySheet(stringResource(R.string.sheet_mode_title), stringResource(R.string.sheet_mode_subtitle), onDismiss) {
            NETWORK_MODES.forEach { mode ->
                val current = vm.state?.mode == mode
                OptionRow(mode, modeDescription(mode), current) {
                    vm.setSetting("network_mode", mode)
                    onDismiss()
                }
            }
        }

        HomeSheet.IPV6 -> BoxySheet(stringResource(R.string.sheet_ipv6_title), stringResource(R.string.sheet_ipv6_subtitle), onDismiss) {
            listOf(true, false).forEach { on ->
                OptionRow(stringResource(if (on) R.string.common_on else R.string.common_off), null, vm.state?.ipv6 == on) {
                    vm.setSetting("ipv6", on.toString())
                    onDismiss()
                }
            }
        }

        HomeSheet.DETAILS -> BoxySheet(stringResource(R.string.details_title), vm.state?.core, onDismiss) {
            val d = vm.details
            SheetGroup {
                InfoRow(stringResource(R.string.details_pid), d?.pid ?: vm.state?.pid ?: dash)
                InfoRow(stringResource(R.string.details_core_version), d?.coreVersion ?: dash)
                InfoRow(stringResource(R.string.details_memory), d?.memoryBytes?.let { Format.bytes(context, it) } ?: dash)
                InfoRow(stringResource(R.string.details_current_cpu), d?.currentCpu ?: dash)
                InfoRow(stringResource(R.string.details_cpu_affinity), d?.cpuAffinity ?: dash)
            }
            SheetButtons(stringResource(R.string.home_reload_config), { vm.reloadConfig(); onDismiss() })
        }

        HomeSheet.GEO -> BoxySheet(stringResource(R.string.geo_title), stringResource(R.string.geo_subtitle), onDismiss) {
            GeoGroup(stringResource(R.string.geo_ipv4), vm.geo4, vm.geoLoading)
            Spacer(Modifier.height(12.dp))
            GeoGroup(stringResource(R.string.geo_ipv6), vm.geo6, vm.geoLoading)
            SheetButtons(stringResource(R.string.action_refresh), { vm.loadGeoDetails(); vm.refreshIp() })
        }

        HomeSheet.SPEED -> BoxySheet(stringResource(R.string.speed_title), stringResource(R.string.speed_details), onDismiss) {
            val s = vm.speed
            SheetGroup {
                Sparkline(s.history, Tints.teal.fg, Modifier.fillMaxWidth().height(90.dp).padding(16.dp))
                InfoRow(stringResource(R.string.speed_down), Format.speed(context, s.down), Tints.blue.fg)
                InfoRow(stringResource(R.string.speed_up), Format.speed(context, s.up), Tints.green.fg)
                InfoRow(stringResource(R.string.speed_fastest_down), Format.speed(context, s.fastestDown))
                InfoRow(stringResource(R.string.speed_fastest_up), Format.speed(context, s.fastestUp))
            }
        }

        HomeSheet.SUBSCRIPTION -> BoxySheet(stringResource(R.string.card_subscription), null, onDismiss) {
            if (vm.subscriptions.isEmpty()) {
                SheetGroup {
                    InfoRow(stringResource(R.string.sub_sheet_none), "")
                    Text(
                        stringResource(R.string.sub_sheet_go_tools),
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Boxy.colors.text2,
                    )
                }
            }
            vm.subscriptions.forEachIndexed { i, sub ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                SheetGroup {
                    Text(
                        sub.name ?: "#${i + 1}",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = Boxy.colors.text,
                    )
                    InfoRow(stringResource(R.string.sub_upload), Format.bytes(context, sub.upload))
                    InfoRow(stringResource(R.string.sub_download), Format.bytes(context, sub.download))
                    InfoRow(stringResource(R.string.sub_remaining), if (sub.total > 0) Format.bytes(context, sub.remaining) else dash)
                    InfoRow(stringResource(R.string.sub_used), Format.bytes(context, sub.used))
                    InfoRow(stringResource(R.string.sub_total), if (sub.total > 0) Format.bytes(context, sub.total) else dash)
                    InfoRow(stringResource(R.string.sub_expire_label), Format.date(sub.expire))
                    InfoRow(stringResource(R.string.sub_updated_label), Format.dateTime(sub.updatedAt))
                }
            }
            SheetButtons(stringResource(R.string.action_refresh), vm::refreshSubscription)
        }

        HomeSheet.SYSTEM -> BoxySheet(stringResource(R.string.sysenv_title), null, onDismiss) {
            val e = vm.systemEnv
            SheetGroup {
                InfoRow(stringResource(R.string.sysenv_android), e?.android ?: dash)
                InfoRow(stringResource(R.string.sysenv_kernel), e?.kernel ?: dash)
                InfoRow(stringResource(R.string.sysenv_memory), e?.totalMemoryBytes?.let { Format.bytes(context, it) } ?: dash)
                InfoRow(
                    stringResource(R.string.sysenv_ipset),
                    when (e?.ipset) {
                        IpsetStatus.AVAILABLE -> stringResource(R.string.ipset_available)
                        IpsetStatus.MISSING_BINARY -> stringResource(R.string.ipset_missing_binary)
                        IpsetStatus.NOT_SUPPORTED -> stringResource(R.string.ipset_not_supported)
                        null -> dash
                    },
                    when (e?.ipset) {
                        IpsetStatus.AVAILABLE -> Tints.green.fg
                        null -> Boxy.colors.text
                        else -> Tints.amber.fg
                    },
                )
            }
            vm.system?.let { s ->
                Spacer(Modifier.height(12.dp))
                SheetGroup {
                    InfoRow(stringResource(R.string.system_cpu), String.format(java.util.Locale.getDefault(), "%.1f%%", s.cpuPercent))
                    InfoRow(stringResource(R.string.system_ram), Format.bytes(context, s.rssBytes))
                }
            }
        }

        HomeSheet.LAYOUT -> LayoutSheet(prefs, onDismiss)
    }
}

@Composable
private fun modeDescription(mode: String): String = when (mode) {
    "redirect" -> "TCP + UDP (direct)"
    "tproxy" -> "TCP + UDP"
    "mixed" -> "redirect (TCP) + tun (UDP)"
    "enhance" -> "redirect (TCP) + tproxy (UDP)"
    else -> "TCP + UDP (auto-route)"
}

@Composable
private fun GeoGroup(title: String, geo: GeoIp?, loading: Boolean) {
    val dash = stringResource(R.string.common_dash)
    SheetGroup {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text)
            Text(
                when {
                    geo != null -> flagEmoji(geo.countryCode)
                    loading -> stringResource(R.string.geo_loading)
                    else -> stringResource(R.string.geo_unavailable)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Boxy.colors.text2,
            )
        }
        InfoRow(stringResource(R.string.geo_ip), geo?.ip ?: dash)
        InfoRow(stringResource(R.string.geo_location), geo?.location?.ifBlank { null } ?: dash)
        InfoRow(stringResource(R.string.geo_isp), geo?.isp ?: dash)
        InfoRow(stringResource(R.string.geo_asn), geo?.asn ?: dash)
    }
}

@Composable
private fun LayoutSheet(prefs: Prefs, onDismiss: () -> Unit) {
    BoxySheet(stringResource(R.string.layout_title), null, onDismiss) {
        Text(stringResource(R.string.layout_sections), style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text2, modifier = Modifier.padding(bottom = 8.dp))
        SheetGroup {
            val sections = HomeSection.entries
            sections.forEachIndexed { i, section ->
                val (title, icon) = when (section) {
                    HomeSection.HERO -> R.string.layout_section_hero to BoxyIcons.Home
                    HomeSection.QUICK -> R.string.layout_section_quick to BoxyIcons.Apps
                    HomeSection.LATENCY -> R.string.layout_section_latency to BoxyIcons.Router
                    HomeSection.GRID -> R.string.layout_section_grid to BoxyIcons.Tune
                }
                SwitchRow(
                    icon,
                    stringResource(title),
                    checked = section !in prefs.hiddenSections,
                    showDivider = i < sections.lastIndex,
                ) { prefs.setSectionVisible(section, it) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.layout_metrics), style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text2, modifier = Modifier.padding(bottom = 8.dp))
        SheetGroup {
            prefs.metricOrder.forEachIndexed { i, card ->
                val title = when (card) {
                    MetricCard.IP -> R.string.card_ip
                    MetricCard.SPEED -> R.string.card_speed
                    MetricCard.SUBSCRIPTION -> R.string.card_subscription
                    MetricCard.SYSTEM -> R.string.card_system
                }
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text)
                    }
                    SmallIconButton(BoxyIcons.ExpandLess, stringResource(R.string.layout_move_up), enabled = i > 0) { prefs.moveMetric(card, -1) }
                    Spacer(Modifier.width(6.dp))
                    SmallIconButton(BoxyIcons.ExpandMore, stringResource(R.string.layout_move_down), enabled = i < prefs.metricOrder.lastIndex) { prefs.moveMetric(card, 1) }
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Switch(
                        checked = card !in prefs.hiddenMetrics,
                        onCheckedChange = { prefs.setMetricVisible(card, it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = Boxy.colors.accent),
                    )
                }
            }
        }
        SheetButtons(stringResource(R.string.action_done), onDismiss, stringResource(R.string.action_reset), prefs::resetHomeLayout)
    }
}

