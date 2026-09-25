package com.skofqq.boxy.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.R
import com.skofqq.boxy.ui.components.BoxySheet
import com.skofqq.boxy.ui.components.IconInfoRow
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.Tints

private const val MODULE_URL = "https://github.com/taamarin/box_for_magisk"
private const val AUTHOR_URL = "https://github.com/skofqq"
private const val BFR_URL = "https://github.com/boxproxy"

/** App card like BFR's: icon, name, version and links to the module, the app and the author. */
@Composable
fun AppInfoSheet(
    versionName: String,
    moduleVersion: String?,
    appUpdate: String?,
    onCheckUpdates: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val open = { url: String -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    val link = Tints.green.fg
    BoxySheet("", null, onDismiss) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(Boxy.colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(112.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Boxy.colors.text)
            Text(versionName, style = MaterialTheme.typography.titleMedium, color = Boxy.colors.text2)
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Boxy.colors.card).padding(vertical = 8.dp)) {
            IconInfoRow(
                BoxyIcons.SystemUpdate,
                stringResource(R.string.about_check_updates),
                appUpdate?.let { "→ $it" } ?: "",
                link,
                onCheckUpdates,
            )
            IconInfoRow(BoxyIcons.Info, stringResource(R.string.about_module), "GitHub", link) { open(MODULE_URL) }
            IconInfoRow(BoxyIcons.Code, stringResource(R.string.about_app_repo), "GitHub", link) { open(GITHUB_URL) }
            IconInfoRow(
                BoxyIcons.Laptop,
                stringResource(R.string.settings_module_version),
                moduleVersion ?: stringResource(R.string.common_dash),
                if (moduleVersion != null) link else Boxy.colors.text,
            ) { open("$GITHUB_URL/releases") }
            IconInfoRow(BoxyIcons.Person, stringResource(R.string.settings_author), "skofqq") { open(AUTHOR_URL) }
            IconInfoRow(BoxyIcons.Person, stringResource(R.string.about_based_on), "boxproxy, taamarin") { open(BFR_URL) }
        }
    }
}
