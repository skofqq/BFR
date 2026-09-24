package com.skofqq.boxy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { LIGHT, DARK, SYSTEM, MATERIAL }

enum class AppLanguage(val tag: String?) { SYSTEM(null), ENGLISH("en"), RUSSIAN("ru") }

/** Extra bottom-navigation page between Home and Tools (at most one, or none). */
enum class NavExtra { NONE, APPS, LOGS }

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

    private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_TRUE_BLACK = "true_black"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_NAV_EXTRA = "nav_extra"

        /** Read before the UI exists (attachBaseContext). */
        fun storedLanguage(context: Context): AppLanguage {
            val name = context.getSharedPreferences("boxy", Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
            return name?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM
        }
    }
}
