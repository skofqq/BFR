package com.skofqq.boxy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { LIGHT, DARK, SYSTEM, MATERIAL }

enum class AppLanguage(val tag: String?) { SYSTEM(null), ENGLISH("en"), RUSSIAN("ru") }

/** Extra bottom-navigation page between Home and Tools (at most one, or none). */
enum class NavExtra { NONE, APPS, LOGS }

/** Blocks of the home page, top to bottom. */
enum class HomeSection { HERO, QUICK, LATENCY, GRID }

/** Small cards of the home metrics grid. */
enum class MetricCard { IP, SPEED, SUBSCRIPTION, SYSTEM }

/** Where the subscription card takes its numbers from. */
enum class SubscriptionSource { URL, PROVIDERS }

data class LatencyTarget(val name: String, val url: String)

val DEFAULT_LATENCY_TARGETS = listOf(
    LatencyTarget("Baidu", "https://baidu.com"),
    LatencyTarget("Cloudflare", "https://cloudflare.com"),
    LatencyTarget("Google", "https://google.com"),
)

/**
 * App preferences backed by SharedPreferences and exposed as Compose state,
 * so every screen recomposes when a value changes.
 */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("boxy", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(enumOf(sp.getString(KEY_THEME, null), ThemeMode.SYSTEM))
        private set
    var trueBlack by mutableStateOf(sp.getBoolean(KEY_TRUE_BLACK, false))
        private set
    var language by mutableStateOf(enumOf(sp.getString(KEY_LANGUAGE, null), AppLanguage.SYSTEM))
        private set
    var navExtra by mutableStateOf(enumOf(sp.getString(KEY_NAV_EXTRA, null), NavExtra.APPS))
        private set

    var hiddenSections by mutableStateOf(enumSet<HomeSection>(sp.getString(KEY_HIDDEN_SECTIONS, "")))
        private set
    var metricOrder by mutableStateOf(metricOrderOf(sp.getString(KEY_METRIC_ORDER, null)))
        private set
    var hiddenMetrics by mutableStateOf(enumSet<MetricCard>(sp.getString(KEY_HIDDEN_METRICS, "")))
        private set

    var latencyTargets by mutableStateOf(targetsOf(sp.getString(KEY_LATENCY_TARGETS, null)))
        private set
    var useClashApi by mutableStateOf(sp.getBoolean(KEY_CLASH_API, false))
        private set
    var subscriptionSource by mutableStateOf(enumOf(sp.getString(KEY_SUB_SOURCE, null), SubscriptionSource.URL))
        private set

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun updateTrueBlack(enabled: Boolean) {
        trueBlack = enabled
        sp.edit().putBoolean(KEY_TRUE_BLACK, enabled).apply()
    }

    fun updateLanguage(lang: AppLanguage) {
        language = lang
        sp.edit().putString(KEY_LANGUAGE, lang.name).commit()
    }

    /** Apps and Logs are mutually exclusive: enabling one replaces the other. */
    fun setNavPage(page: NavExtra, enabled: Boolean) {
        val next = when {
            enabled -> page
            navExtra == page -> NavExtra.NONE
            else -> navExtra
        }
        navExtra = next
        sp.edit().putString(KEY_NAV_EXTRA, next.name).apply()
    }

    fun setSectionVisible(section: HomeSection, visible: Boolean) {
        hiddenSections = if (visible) hiddenSections - section else hiddenSections + section
        sp.edit().putString(KEY_HIDDEN_SECTIONS, hiddenSections.joinToString(",") { it.name }).apply()
    }

    fun setMetricVisible(card: MetricCard, visible: Boolean) {
        hiddenMetrics = if (visible) hiddenMetrics - card else hiddenMetrics + card
        sp.edit().putString(KEY_HIDDEN_METRICS, hiddenMetrics.joinToString(",") { it.name }).apply()
    }

    fun moveMetric(card: MetricCard, delta: Int) {
        val list = metricOrder.toMutableList()
        val from = list.indexOf(card)
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from < 0 || from == to) return
        list.add(to, list.removeAt(from))
        metricOrder = list
        sp.edit().putString(KEY_METRIC_ORDER, list.joinToString(",") { it.name }).apply()
    }

    fun resetHomeLayout() {
        hiddenSections = emptySet()
        hiddenMetrics = emptySet()
        metricOrder = MetricCard.entries
        sp.edit().remove(KEY_HIDDEN_SECTIONS).remove(KEY_HIDDEN_METRICS).remove(KEY_METRIC_ORDER).apply()
    }

    fun updateLatencyTargets(targets: List<LatencyTarget>?) {
        latencyTargets = targets ?: DEFAULT_LATENCY_TARGETS
        if (targets == null) {
            sp.edit().remove(KEY_LATENCY_TARGETS).apply()
        } else {
            val arr = JSONArray()
            targets.forEach { arr.put(JSONObject().put("name", it.name).put("url", it.url)) }
            sp.edit().putString(KEY_LATENCY_TARGETS, arr.toString()).apply()
        }
    }

    fun updateUseClashApi(enabled: Boolean) {
        useClashApi = enabled
        sp.edit().putBoolean(KEY_CLASH_API, enabled).apply()
    }

    fun updateSubscriptionSource(source: SubscriptionSource) {
        subscriptionSource = source
        sp.edit().putString(KEY_SUB_SOURCE, source.name).apply()
    }

    /** Last known subscription numbers, shown until a fresh request finishes. */
    var subscriptionCache: String?
        get() = sp.getString(KEY_SUB_CACHE, null)
        set(value) = sp.edit().putString(KEY_SUB_CACHE, value).apply()

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private inline fun <reified T : Enum<T>> enumSet(csv: String?): Set<T> =
        csv.orEmpty().split(',').mapNotNull { n -> runCatching { enumValueOf<T>(n.trim()) }.getOrNull() }.toSet()

    private fun metricOrderOf(csv: String?): List<MetricCard> {
        val stored = enumSet<MetricCard>(csv).let { set -> csv.orEmpty().split(',').mapNotNull { n -> set.firstOrNull { it.name == n } } }
        return stored + MetricCard.entries.filter { it !in stored }
    }

    private fun targetsOf(json: String?): List<LatencyTarget> = runCatching {
        val arr = JSONArray(json ?: return DEFAULT_LATENCY_TARGETS)
        (0 until arr.length()).map { arr.getJSONObject(it).let { o -> LatencyTarget(o.getString("name"), o.getString("url")) } }
    }.getOrNull()?.takeIf { it.isNotEmpty() } ?: DEFAULT_LATENCY_TARGETS

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_TRUE_BLACK = "true_black"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NAV_EXTRA = "nav_extra"
        private const val KEY_HIDDEN_SECTIONS = "home_hidden_sections"
        private const val KEY_METRIC_ORDER = "home_metric_order"
        private const val KEY_HIDDEN_METRICS = "home_hidden_metrics"
        private const val KEY_LATENCY_TARGETS = "latency_targets"
        private const val KEY_CLASH_API = "use_clash_api"
        private const val KEY_SUB_SOURCE = "subscription_source"
        private const val KEY_SUB_CACHE = "subscription_cache_v1"

        /** Read before the UI exists (attachBaseContext). */
        fun storedLanguage(context: Context): AppLanguage {
            val name = context.getSharedPreferences("boxy", Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            return name?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
        }
    }
}
