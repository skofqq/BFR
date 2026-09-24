package com.skofqq.boxy

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skofqq.boxy.data.NavExtra
import com.skofqq.boxy.data.Prefs
import com.skofqq.boxy.ui.components.BarItem
import com.skofqq.boxy.ui.components.BottomBar
import com.skofqq.boxy.ui.apps.AppsScreen
import com.skofqq.boxy.ui.home.HomeScreen
import com.skofqq.boxy.ui.panel.PanelScreen
import com.skofqq.boxy.ui.screens.LogsScreen
import com.skofqq.boxy.ui.screens.SettingsScreen
import com.skofqq.boxy.ui.screens.ToolsScreen
import com.skofqq.boxy.ui.theme.Boxy
import com.skofqq.boxy.ui.theme.BoxyIcons
import com.skofqq.boxy.ui.theme.BoxyTheme
import kotlinx.coroutines.launch
import java.util.Locale

private enum class Tab { HOME, APPS, LOGS, TOOLS, SETTINGS }

/** Full-screen pages opened on top of the tabs. */
private enum class Overlay { APPS, LOGS, PANEL, SUBSTORE }

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val tag = Prefs.storedLanguage(newBase).tag
        if (tag == null) {
            super.attachBaseContext(newBase)
        } else {
            val config = Configuration(newBase.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
            super.attachBaseContext(newBase.createConfigurationContext(config))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = (application as BoxyApp).prefs
        setContent {
            BoxyTheme(prefs.themeMode, prefs.trueBlack) {
                val dark = Boxy.colors.isDark
                DisposableEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(style, style)
                    onDispose {}
                }
                MainScreen(prefs)
            }
        }
    }
}

@Composable
private fun MainScreen(prefs: Prefs) {
    val tabs = buildList {
        add(Tab.HOME)
        when (prefs.navExtra) {
            NavExtra.APPS -> add(Tab.APPS)
            NavExtra.LOGS -> add(Tab.LOGS)
            NavExtra.NONE -> Unit
        }
        add(Tab.TOOLS)
        add(Tab.SETTINGS)
    }
    var current by rememberSaveable { mutableStateOf(Tab.HOME) }
    var subPage by rememberSaveable { mutableStateOf<Overlay?>(null) }
    var lastSub by rememberSaveable { mutableStateOf(Overlay.APPS) }
    subPage?.let { lastSub = it }
    val pager = rememberPagerState(initialPage = tabs.indexOf(current).coerceAtLeast(0)) { tabs.size }
    val scope = rememberCoroutineScope()

    // Keep the same tab selected when the bar gains or loses a page.
    LaunchedEffect(tabs.size) {
        val index = tabs.indexOf(current).takeIf { it >= 0 } ?: 0
        if (pager.currentPage != index) pager.scrollToPage(index)
    }
    LaunchedEffect(pager.settledPage) { current = tabs.getOrElse(pager.settledPage) { Tab.HOME } }
    BackHandler(enabled = subPage == null && pager.currentPage != 0) { scope.launch { pager.animateScrollToPage(0) } }

    val insets = WindowInsets.statusBars.asPaddingValues()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val padding = PaddingValues(top = insets.calculateTopPadding(), bottom = bottomInset + 100.dp)

    Box(Modifier.fillMaxSize().background(Boxy.colors.page)) {
        HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1, key = { tabs[it] }) { index ->
            when (tabs[index]) {
                Tab.HOME -> HomeScreen(
                    padding,
                    prefs,
                    onOpenPanel = { subPage = Overlay.PANEL },
                    onOpenLogs = {
                        val i = tabs.indexOf(Tab.LOGS)
                        if (i >= 0) scope.launch { pager.animateScrollToPage(i) } else subPage = Overlay.LOGS
                    },
                    onOpenSubStore = { subPage = Overlay.SUBSTORE },
                )
                Tab.APPS -> AppsScreen(padding)
                Tab.LOGS -> LogsScreen(padding)
                Tab.TOOLS -> ToolsScreen(padding, prefs.navExtra) { subPage = if (it == NavExtra.LOGS) Overlay.LOGS else Overlay.APPS }
                Tab.SETTINGS -> SettingsScreen(padding, prefs)
            }
        }
        val labels = mapOf(
            Tab.HOME to BarItem(stringResource(R.string.tab_home), BoxyIcons.Home),
            Tab.APPS to BarItem(stringResource(R.string.tab_apps), BoxyIcons.Apps),
            Tab.LOGS to BarItem(stringResource(R.string.tab_logs), BoxyIcons.Description),
            Tab.TOOLS to BarItem(stringResource(R.string.tab_tools), BoxyIcons.Build),
            Tab.SETTINGS to BarItem(stringResource(R.string.tab_settings), BoxyIcons.Settings),
        )
        BottomBar(
            items = tabs.map { labels.getValue(it) },
            selected = (pager.currentPage + pager.currentPageOffsetFraction).coerceIn(0f, tabs.lastIndex.toFloat()),
            onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset + 16.dp),
        )

        // Apps / Logs opened from Tools when they are not in the bar.
        AnimatedVisibility(
            visible = subPage != null,
            enter = slideInHorizontally(tween(280)) { it },
            exit = slideOutHorizontally(tween(240)) { it },
        ) {
            BackHandler { subPage = null }
            val subPadding = PaddingValues(top = insets.calculateTopPadding(), bottom = bottomInset + 24.dp)
            val header: @Composable () -> Unit = { BackButton { subPage = null } }
            Box(Modifier.fillMaxSize().background(Boxy.colors.page)) {
                when (lastSub) {
                    Overlay.LOGS -> LogsScreen(subPadding, header)
                    Overlay.APPS -> AppsScreen(subPadding, header)
                    Overlay.PANEL, Overlay.SUBSTORE -> PanelScreen(subPadding, header) { subPage = null }
                }
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 12.dp, top = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(Boxy.colors.surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BoxyIcons.ArrowBack, null, Modifier.size(18.dp), tint = Boxy.colors.text)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.action_back), style = MaterialTheme.typography.labelLarge, color = Boxy.colors.text)
    }
}
