package com.skofqq.boxy.ui.panel

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.skofqq.boxy.R
import com.skofqq.boxy.net.Net
import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.ui.components.HeaderAction
import com.skofqq.boxy.ui.components.PageHeader
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons

/** Web UI of the running core (metacubexd / zashboard / yacd) served by its external controller. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PanelScreen(contentPadding: PaddingValues, header: @Composable () -> Unit, onClose: () -> Unit) {
    var url by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        val api = Net.clashApi(BoxModule.readSetting("bin_name"))
        if (api == null) failed = true else url = "${api.base}/ui/"
    }
    BackHandler {
        val w = webView
        if (w != null && w.canGoBack()) w.goBack() else onClose()
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        header()
        PageHeader(stringResource(R.string.panel_title), url) {
            HeaderAction(BoxyIcons.Refresh, stringResource(R.string.action_refresh)) { webView?.reload() }
        }
        Box(Modifier.fillMaxSize().padding(horizontal = 12.dp).background(Boxy.colors.card), contentAlignment = Alignment.Center) {
            val target = url
            when {
                failed -> Text(
                    stringResource(R.string.panel_unavailable),
                    Modifier.fillMaxWidth().padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Boxy.colors.text2,
                    textAlign = TextAlign.Center,
                )
                target == null -> CircularProgressIndicator()
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            webViewClient = WebViewClient()
                            loadUrl(target)
                            webView = this
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
        }
    }
}
