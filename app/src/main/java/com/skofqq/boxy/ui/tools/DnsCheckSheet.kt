package com.skofqq.boxy.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.net.GeoIp
import com.skofqq.boxy.net.Net
import com.skofqq.boxy.net.flagEmoji
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.DnsWhoami
import com.skofqq.boxy.ui.components.BoxySheet
import com.skofqq.boxy.ui.components.SheetGroup
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.Tints
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * DNSCrypt check: the address whoami.akamai.net sees for a query made the way apps make it (through the
 * core), straight at dnscrypt-proxy and past Boxy. Matching first two addresses mean apps resolve via DNSCrypt.
 */
@Composable
fun DnsCheckSheet(port: String, onDismiss: () -> Unit) {
    var run by remember { mutableStateOf(0) }
    var result by remember { mutableStateOf<DnsWhoami?>(null) }
    val geo = remember { mutableStateMapOf<String, GeoIp?>() }
    LaunchedEffect(run) {
        result = null
        val r = BoxModule.dnsWhoami(port)
        result = r
        coroutineScope {
            listOfNotNull(r.viaCore, r.viaDnscrypt, r.direct).distinct().filter { it !in geo }
                .map { ip -> async { ip to Net.geoIpOf(ip) } }.awaitAll()
                .forEach { (ip, g) -> geo[ip] = g }
        }
    }

    BoxySheet(stringResource(R.string.dnscheck_title), stringResource(R.string.dnscheck_sub), onDismiss) {
        val r = result
        if (r == null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = Boxy.colors.accent)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.dnscheck_running), color = Boxy.colors.text2)
            }
            return@BoxySheet
        }
        val ok = r.viaCore != null && r.viaCore == r.viaDnscrypt
        val tint = when {
            ok -> Tints.green
            r.coreFakeIp -> Tints.amber
            else -> Tints.red
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(tint.bg).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (ok) BoxyIcons.CheckCircle else BoxyIcons.Block, null, Modifier.size(22.dp), tint = tint.fg)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(
                    when {
                        ok -> R.string.dnscheck_ok
                        r.coreFakeIp -> R.string.dnscheck_fakeip
                        r.viaDnscrypt == null -> R.string.dnscheck_no_dnscrypt
                        else -> R.string.dnscheck_bad
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Boxy.colors.text,
            )
        }
        Spacer(Modifier.height(12.dp))
        SheetGroup {
            ResolverRow(stringResource(R.string.dnscheck_core), r.viaCore, geo)
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Boxy.colors.outline)
            ResolverRow("dnscrypt-proxy (127.0.0.1:$port)", r.viaDnscrypt, geo)
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Boxy.colors.outline)
            ResolverRow(stringResource(R.string.dnscheck_direct), r.direct, geo)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(18.dp)).background(Boxy.colors.card).clickable { run++ },
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.dnscheck_again), color = Boxy.colors.text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ResolverRow(label: String, ip: String?, geo: Map<String, GeoIp?>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Boxy.colors.text2)
        Text(
            ip ?: stringResource(R.string.dnscheck_no_answer),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (ip == null) Boxy.colors.text2 else Boxy.colors.text,
        )
        val g = ip?.let { geo[it] }
        if (g != null) {
            Text(
                listOfNotNull(g.isp, g.country).joinToString(", ").let { "${flagEmoji(g.countryCode)} $it".trim() },
                style = MaterialTheme.typography.bodySmall,
                color = Boxy.colors.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
