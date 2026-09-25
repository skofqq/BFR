package com.skofqq.boxy.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface CheckResult {
    data object Ok : CheckResult

    /** The core has no check command (hysteria) or its binary is missing. */
    data object Unsupported : CheckResult

    data class Failed(val output: String) : CheckResult
}

/** Runs the same config test box.service runs before starting a core. */
object ConfigCheck {
    private const val BOX = BoxModule.BOX_DIR
    private val EXTENSIONS = setOf("yaml", "yml", "json")

    private fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"

    /** Core whose config folder holds [path] directly (sub-folders such as rule-sets are not configs). */
    fun coreFor(path: String): String? {
        val parent = path.substringBeforeLast('/')
        val ext = path.substringAfterLast('.', "").lowercase()
        if (ext !in EXTENSIONS) return null
        return CORES.firstOrNull { parent == "$BOX/$it" }
    }

    suspend fun check(core: String, file: String): CheckResult = withContext(Dispatchers.IO) {
        val dir = "$BOX/$core"
        val cmd = when (core) {
            "clash" -> "b=$BOX/bin/clash; [ -x \"\$b\" ] || b=$BOX/bin/xclash/mihomo; " +
                "[ -x \"\$b\" ] || exit 127; timeout 30 \"\$b\" -t -d ${q(dir)} -f ${q(file)} 2>&1"
            "sing-box" -> "b=$BOX/bin/sing-box; [ -x \"\$b\" ] || exit 127; timeout 30 \"\$b\" check -D ${q(dir)} -c ${q(file)} 2>&1"
            "xray" -> "b=$BOX/bin/xray; [ -x \"\$b\" ] || exit 127; " +
                "XRAY_LOCATION_ASSET=${q(dir)} timeout 30 \"\$b\" run -test -c ${q(file)} 2>&1"
            "v2fly" -> "b=$BOX/bin/v2fly; [ -x \"\$b\" ] || exit 127; " +
                "V2RAY_LOCATION_ASSET=${q(dir)} timeout 30 \"\$b\" test -c ${q(file)} 2>&1"
            else -> return@withContext CheckResult.Unsupported
        }
        val r = Shell.cmd(cmd).exec()
        when {
            r.code == 127 -> CheckResult.Unsupported
            r.isSuccess -> CheckResult.Ok
            else -> CheckResult.Failed(errorText(r.out + r.err))
        }
    }

    /** Checks unsaved text: writes it next to the original (relative includes still resolve), tests, deletes. */
    suspend fun checkText(core: String, path: String, content: String): CheckResult {
        val ext = path.substringAfterLast('.', "yaml")
        val tmp = "${path.substringBeforeLast('/')}/.boxy-check.$ext"
        if (!BoxModule.writeFile(tmp, content)) return CheckResult.Unsupported
        return try {
            check(core, tmp)
        } finally {
            withContext(Dispatchers.IO) { Shell.cmd("rm -f ${q(tmp)}").exec() }
        }
    }

    /** The lines that explain the failure: mihomo / sing-box print a lot of info lines first. */
    private fun errorText(lines: List<String>): String {
        val clean = lines.map { RootTask.stripAnsi(it).trim() }.filter { it.isNotEmpty() }
        val errors = clean.filter { l ->
            val low = l.lowercase()
            "error" in low || "fatal" in low || "failed" in low || "yaml:" in low || "invalid" in low
        }
        return (errors.ifEmpty { clean }).takeLast(12).joinToString("\n")
            .replace(Regex("time=\"[^\"]*\"\\s*"), "")
            .ifBlank { "exit code != 0" }
    }
}
