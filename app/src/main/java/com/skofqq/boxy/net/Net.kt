package com.skofqq.boxy.net

import com.skofqq.boxy.root.BoxModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

data class LanAddress(val ip: String, val iface: String)

data class GeoIp(
    val ip: String,
    val countryCode: String?,
    val country: String?,
    val region: String?,
    val city: String?,
    val isp: String?,
    val asn: String?,
) {
    val location: String get() = listOfNotNull(city, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

data class SubscriptionInfo(
    val name: String?,
    val upload: Long,
    val download: Long,
    val total: Long,
    /** Unix seconds, 0 when unknown. */
    val expire: Long,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val used: Long get() = upload + download
    val remaining: Long get() = (total - used).coerceAtLeast(0)
}

data class ClashApi(val base: String, val secret: String?)

object Net {
    private const val TIMEOUT = 5000

    private fun open(url: String, method: String = "GET", ua: String = "BoxApp/Android"): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT
            readTimeout = TIMEOUT
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", ua)
        }

    /** Time to first response of a tiny GET, in ms; null when unreachable. */
    suspend fun latency(url: String): Long? = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.nanoTime()
            val c = open(normalizeUrl(url), ua = "BoxApp/Latency")
            c.setRequestProperty("Range", "bytes=0-0")
            try {
                if (c.responseCode > 0) (System.nanoTime() - start) / 1_000_000 else null
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }

    fun normalizeUrl(s: String): String {
        val t = s.trim()
        return if (t.startsWith("http://", true) || t.startsWith("https://", true)) t else "https://$t"
    }

    fun lanAddress(): LanAddress? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { it.isUp && !it.isLoopback }
            .firstNotNullOfOrNull { ni ->
                ni.inetAddresses.toList().firstOrNull { it is Inet4Address }?.let { LanAddress(it.hostAddress ?: "-", ni.name) }
            }
    }.getOrNull()

    /** Public address details from ip.sb (goes through the proxy when the service runs). */
    suspend fun geoIp(v6: Boolean): GeoIp? = withContext(Dispatchers.IO) {
        runCatching {
            val c = open(if (v6) "https://api-ipv6.ip.sb/geoip" else "https://api-ipv4.ip.sb/geoip")
            try {
                if (c.responseCode !in 200..299) return@runCatching null
                val j = JSONObject(c.inputStream.bufferedReader().readText())
                GeoIp(
                    ip = j.optString("ip").ifBlank { return@runCatching null },
                    countryCode = j.optString("country_code").ifBlank { null },
                    country = j.optString("country").ifBlank { null },
                    region = j.optString("region").ifBlank { null },
                    city = j.optString("city").ifBlank { null },
                    isp = (j.optString("isp").ifBlank { j.optString("organization") }).ifBlank { null },
                    asn = j.optLong("asn", 0).takeIf { it > 0 }?.let { "AS$it" },
                )
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }

    /** Traffic quota from the subscription-userinfo header of a subscription URL. */
    suspend fun subscription(url: String): SubscriptionInfo? = withContext(Dispatchers.IO) {
        for (method in listOf("HEAD", "GET")) {
            val info = runCatching {
                val c = open(url, method, ua = "clash")
                c.setRequestProperty("Accept", "*/*")
                c.setRequestProperty("Cache-Control", "no-cache")
                if (method == "GET") c.setRequestProperty("Range", "bytes=0-0")
                try {
                    c.responseCode
                    parseUserInfo(c.getHeaderField("subscription-userinfo"), fileName(c.getHeaderField("content-disposition")))
                } finally {
                    c.disconnect()
                }
            }.getOrNull()
            if (info != null) return@withContext info
        }
        null
    }

    fun parseUserInfo(header: String?, name: String?): SubscriptionInfo? {
        if (header.isNullOrBlank()) return null
        val m = header.split(';').mapNotNull { part ->
            val k = part.substringBefore('=', "").trim().lowercase()
            val v = part.substringAfter('=').trim().toDoubleOrNull()?.toLong()
            if (k.isEmpty() || v == null) null else k to v
        }.toMap()
        if (m.isEmpty()) return null
        return SubscriptionInfo(name, m["upload"] ?: 0, m["download"] ?: 0, m["total"] ?: 0, m["expire"] ?: 0)
    }

    private fun fileName(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.let {
            return java.net.URLDecoder.decode(it.groupValues[1], "UTF-8")
        }
        return Regex("filename=\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.get(1)
    }

    // ---- Clash API (mihomo / sing-box clash_api) ----

    /** Reads external-controller and secret from the active core config. */
    suspend fun clashApi(core: String?): ClashApi? {
        val (addr, secret) = when (core) {
            "sing-box" -> {
                val name = BoxModule.readSetting("name_sing_config") ?: "config.json"
                val text = BoxModule.readFile("${BoxModule.BOX_DIR}/sing-box/$name") ?: return null
                runCatching {
                    val api = JSONObject(text).optJSONObject("experimental")?.optJSONObject("clash_api") ?: return null
                    api.optString("external_controller") to api.optString("secret")
                }.getOrNull() ?: return null
            }
            else -> {
                val name = BoxModule.readSetting("name_clash_config") ?: "config.yaml"
                val text = BoxModule.readFile("${BoxModule.BOX_DIR}/clash/$name") ?: return null
                yamlValue(text, "external-controller") to yamlValue(text, "secret")
            }
        }
        if (addr.isNullOrBlank()) return null
        val hostPort = addr.trim().let { if (it.startsWith(":")) "127.0.0.1$it" else it.replace("0.0.0.0", "127.0.0.1") }
        return ClashApi("http://$hostPort", secret?.takeIf { it.isNotBlank() })
    }

    private fun yamlValue(text: String, key: String): String? =
        Regex("^$key:\\s*['\"]?([^'\"#\\n]*)['\"]?", RegexOption.MULTILINE).find(text)?.groupValues?.get(1)?.trim()

    private fun openApi(api: ClashApi, path: String, method: String = "GET", timeout: Int = TIMEOUT): HttpURLConnection =
        open(api.base + path, method).apply {
            readTimeout = timeout
            api.secret?.let { setRequestProperty("Authorization", "Bearer $it") }
        }

    /** Live traffic from /traffic: pairs of (down, up) bytes per second. */
    fun clashTraffic(api: ClashApi): Flow<Pair<Long, Long>> = flow {
        val c = openApi(api, "/traffic", timeout = 0)
        try {
            c.inputStream.bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (!currentCoroutineContext().isActive) break
                    val j = runCatching { JSONObject(line) }.getOrNull() ?: continue
                    emit(j.optLong("down") to j.optLong("up"))
                }
            }
        } finally {
            c.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /** Subscription quotas reported by proxy providers. */
    suspend fun clashProviders(api: ClashApi): List<SubscriptionInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val c = openApi(api, "/providers/proxies")
            try {
                val providers = JSONObject(c.inputStream.bufferedReader().readText()).optJSONObject("providers") ?: return@runCatching emptyList()
                providers.keys().asSequence().mapNotNull { key ->
                    val p = providers.optJSONObject(key) ?: return@mapNotNull null
                    val s = p.optJSONObject("subscriptionInfo") ?: return@mapNotNull null
                    SubscriptionInfo(
                        name = p.optString("name", key),
                        upload = s.optLong("Upload"),
                        download = s.optLong("Download"),
                        total = s.optLong("Total"),
                        expire = s.optLong("Expire"),
                    )
                }.toList()
            } finally {
                c.disconnect()
            }
        }.getOrDefault(emptyList())
    }

    /** Download / upload totals of live connections, skipping those routed through [excluded] chains. */
    suspend fun clashConnectionTotals(api: ClashApi, excluded: Set<String>): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        runCatching {
            val c = openApi(api, "/connections")
            try {
                val arr = JSONObject(c.inputStream.bufferedReader().readText()).optJSONArray("connections") ?: return@runCatching 0L to 0L
                var down = 0L
                var up = 0L
                for (i in 0 until arr.length()) {
                    val conn = arr.getJSONObject(i)
                    val chains = conn.optJSONArray("chains")
                    val skip = chains != null && (0 until chains.length()).any { chains.optString(it) in excluded }
                    if (!skip) {
                        down += conn.optLong("download")
                        up += conn.optLong("upload")
                    }
                }
                down to up
            } finally {
                c.disconnect()
            }
        }.getOrNull()
    }

    /** Names and URLs of proxy-providers declared in the active Clash config. */
    suspend fun providerUrls(): List<Pair<String, String>> {
        val name = BoxModule.readSetting("name_clash_config") ?: "config.yaml"
        val text = BoxModule.readFile("${BoxModule.BOX_DIR}/clash/$name") ?: return emptyList()
        val result = mutableListOf<Pair<String, String>>()
        var inProviders = false
        var current: String? = null
        text.lines().forEach { raw ->
            val line = raw.substringBefore(" #")
            if (line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("\t")) {
                inProviders = line.trim().startsWith("proxy-providers:")
                current = null
                return@forEach
            }
            if (!inProviders) return@forEach
            val indent = line.length - line.trimStart().length
            val t = line.trim()
            if (indent in 1..4 && t.endsWith(":") && !t.startsWith("-")) current = t.removeSuffix(":").trim('"', '\'')
            if (t.startsWith("url:")) {
                val url = t.removePrefix("url:").trim().trim('"', '\'')
                if (url.startsWith("http")) result += (current ?: url) to url
            }
        }
        return result
    }

    /** PUT /configs?force=true reloads the config of a running mihomo core. */
    suspend fun clashReload(api: ClashApi): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val c = openApi(api, "/configs?force=true", "PUT")
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write("{\"path\":\"\",\"payload\":\"\"}".toByteArray()) }
            try {
                c.responseCode in 200..299
            } finally {
                c.disconnect()
            }
        }.getOrDefault(false)
    }
}

/** Country code to flag emoji ("RU" -> 🇷🇺). */
fun flagEmoji(code: String?): String {
    val c = code?.uppercase()?.takeIf { it.length == 2 && it.all { ch -> ch in 'A'..'Z' } } ?: return ""
    return c.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}
