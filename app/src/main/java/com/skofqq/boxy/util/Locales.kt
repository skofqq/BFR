package com.skofqq.boxy.util

import android.content.Context
import android.content.res.Configuration
import com.skofqq.boxy.data.Prefs
import java.util.Locale

/** Context with the language chosen in Settings (system language when "Follow system"). */
fun Context.withAppLocale(): Context {
    val tag = Prefs.storedLanguage(this).tag ?: return this
    val config = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
    return createConfigurationContext(config)
}
