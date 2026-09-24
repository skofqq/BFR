package com.skofqq.boxy.net

import com.skofqq.boxy.root.BoxModule
import com.skofqq.boxy.root.RootTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub download mirrors ("gh proxy" style: https://mirror/https://github.com/...).
 * Used by the app's own downloads and written into the module's box.tool (url_ghproxy / use_ghproxy).
 */
object Mirrors {
    val KNOWN = listOf("https://ghfast.top", "https://gh-proxy.com", "https://ghproxy.net", "https://gh.llkk.cc")

    /** Current mirror, empty for direct GitHub. Set from preferences on start and on change. */
    @Volatile
    var current: String = ""

    private val githubHosts = listOf(
        "https://github.com/", "https://raw.githubusercontent.com/", "https://gist.github.com/",
        "https://gist.githubusercontent.com/", "https://objects.githubusercontent.com/",
    )

    fun wrap(url: String, mirror: String = current): String =
        if (mirror.isNotBlank() && githubHosts.any { url.startsWith(it) }) mirror.trimEnd('/') + "/" + url else url

    /** Makes the module's own downloads (cores, geo files, WebUI) use the same mirror. */
    suspend fun applyToModule(mirror: String): Boolean {
        val tool = "${BoxModule.SCRIPTS}/box.tool"
        val use = if (mirror.isBlank()) "false" else "true"
        val m = mirror.ifBlank { KNOWN.first() }.trimEnd('/').replace("/", "\\/")
        return BoxModule.exec(
            "sed -i 's/^url_ghproxy=.*/url_ghproxy=\"$m\"/; s/^use_ghproxy=.*/use_ghproxy=\"$use\"/' $tool",
        ).first
    }

    /** Mirror configured in the module now (null when direct). */
    suspend fun moduleMirror(): String? {
        val (_, out) = BoxModule.exec("grep -E '^(url_ghproxy|use_ghproxy)=' ${BoxModule.SCRIPTS}/box.tool")
        val kv = BoxModule.parseKv(out)
        return if (kv["use_ghproxy"] == "true") kv["url_ghproxy"] else null
    }

    /** Response time of a mirror in ms (HEAD on a small GitHub file through it), null when unreachable. */
    suspend fun probe(mirror: String): Long? = Net.latency(wrap("https://github.com/favicon.ico", mirror))

    /**
     * Downloads [url] (through the mirror, falling back to the other mirrors and then GitHub itself)
     * into [target]. [onProgress] gets 0..1, or -1 when the size is unknown.
     */
    suspend fun download(url: String, target: File, onProgress: (Float) -> Unit, onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val candidates = (listOf(current) + KNOWN + "").distinct().map { wrap(url, it) }.distinct()
        for (u in candidates) {
            onLog("GET $u")
            val ok = runCatching {
                val c = URL(u).openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 30000
                c.instanceFollowRedirects = true
                c.setRequestProperty("User-Agent", "Boxy")
                try {
                    if (c.responseCode !in 200..299) {
                        onLog("HTTP ${c.responseCode}")
                        return@runCatching false
                    }
                    val total = c.contentLengthLong
                    var done = 0L
                    c.inputStream.use { input ->
                        target.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                onProgress(if (total > 0) done.toFloat() / total else -1f)
                            }
                        }
                    }
                    onLog("OK ${done / 1024} KB")
                    done > 0
                } finally {
                    c.disconnect()
                }
            }.getOrElse {
                onLog("${it.javaClass.simpleName}: ${it.message}")
                false
            }
            if (ok) return@withContext true
        }
        false
    }
}

enum class Dashboard(val title: String, val zipUrl: String, val folder: String, val marker: String) {
    ZASHBOARD("Zashboard", "https://github.com/Zephyruso/zashboard/archive/refs/heads/gh-pages.zip", "zashboard-gh-pages", "zashboard"),
    METACUBEXD("MetaCubeXD", "https://github.com/MetaCubeX/metacubexd/archive/refs/heads/gh-pages.zip", "metacubexd-gh-pages", "metacubexd"),
}

/** The core's web UI lives in <core>/dashboard (external-ui: ./dashboard); switching replaces its contents. */
object Dashboards {
    private fun dir(core: String) = "${BoxModule.BOX_DIR}/${if (core == "sing-box") "sing-box" else "clash"}/dashboard"

    suspend fun installed(core: String): Dashboard? {
        val (_, out) = BoxModule.exec("cat '${dir(core)}/index.html' 2>/dev/null | head -c 4000")
        val html = out.joinToString("\n").lowercase()
        return Dashboard.entries.firstOrNull { html.contains(it.marker) }
    }

    /** Downloads [d] and puts it in place of the current dashboard. */
    suspend fun install(cacheDir: File, core: String, d: Dashboard, onProgress: (Float) -> Unit, onLog: (String) -> Unit): Boolean {
        val zip = File(cacheDir, "${d.marker}.zip")
        if (!Mirrors.download(d.zipUrl, zip, onProgress, onLog)) return false
        val target = dir(core)
        val tmp = "$target.boxy-new"
        val cmd = """
            rm -rf '$tmp' && mkdir -p '$tmp' &&
            (unzip -o -q '${zip.absolutePath}' -d '$tmp' 2>/dev/null || busybox unzip -o -q '${zip.absolutePath}' -d '$tmp' || /data/adb/magisk/busybox unzip -o -q '${zip.absolutePath}' -d '$tmp') &&
            [ -f '$tmp/${d.folder}/index.html' ] &&
            rm -rf '$target' && mv '$tmp/${d.folder}' '$target' && rm -rf '$tmp' &&
            chown -R ${'$'}(stat -c '%u:%g' '${BoxModule.BOX_DIR}') '$target' 2>/dev/null; [ -f '$target/index.html' ]
        """.trimIndent()
        onLog("unzip → $target")
        return RootTask.run(cmd) { onLog(it) }.also { zip.delete() }
    }
}
