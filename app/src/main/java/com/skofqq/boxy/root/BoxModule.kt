package com.skofqq.boxy.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the device offers for running the Box service. */
enum class Environment { READY, NO_ROOT, NO_MODULE, NO_SCRIPTS }

data class ServiceState(
    val running: Boolean,
    val pid: String?,
    val core: String?,
    val mode: String?,
    val ipv6: Boolean?,
    val moduleVersion: String?,
)

/**
 * Thin wrapper around the Box for Root module files and scripts.
 * Paths and commands follow the module's own action.sh / box.service.
 */
object BoxModule {
    const val BOX_DIR = "/data/adb/box"
    const val MODULE_DIR = "/data/adb/modules/box_for_root"
    private const val SCRIPTS = "$BOX_DIR/scripts"
    private const val PID_FILE = "$BOX_DIR/run/box.pid"
    private const val SETTINGS = "$BOX_DIR/settings.ini"

    suspend fun environment(): Environment = withContext(Dispatchers.IO) {
        // A shell opened before the user answered the root prompt stays non-root; reopen it once.
        var shell = Shell.getShell()
        if (!shell.isRoot) {
            shell.close()
            shell = Shell.getShell()
        }
        if (!shell.isRoot) return@withContext Environment.NO_ROOT
        when {
            !exists(MODULE_DIR) -> Environment.NO_MODULE
            !exists("$SCRIPTS/box.service") -> Environment.NO_SCRIPTS
            else -> Environment.READY
        }
    }

    suspend fun state(): ServiceState = withContext(Dispatchers.IO) {
        val pid = sh("cat $PID_FILE 2>/dev/null").firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val running = pid != null && exists("/proc/$pid")
        val ini = readIni()
        val version = sh("grep '^version=' $MODULE_DIR/module.prop 2>/dev/null").firstOrNull()?.substringAfter('=')
        ServiceState(
            running = running,
            pid = pid.takeIf { running },
            core = ini["bin_name"],
            mode = ini["network_mode"],
            ipv6 = ini["ipv6"]?.let { it == "true" },
            moduleVersion = version,
        )
    }

    /** Same order as the module's action button: service first, then iptables. */
    suspend fun start(): Boolean = run("$SCRIPTS/box.service start && $SCRIPTS/box.iptables enable")

    suspend fun stop(): Boolean = run("$SCRIPTS/box.iptables disable; $SCRIPTS/box.service stop")

    suspend fun restart(): Boolean = stop().let { start() }

    private suspend fun run(cmd: String): Boolean = withContext(Dispatchers.IO) {
        Shell.cmd("$cmd >/dev/null 2>&1").exec().isSuccess
    }

    private fun readIni(): Map<String, String> =
        sh("grep -E '^(bin_name|network_mode|ipv6)=' $SETTINGS 2>/dev/null").mapNotNull { line ->
            val key = line.substringBefore('=', "").trim()
            val value = line.substringAfter('=').trim().trim('"', '\'')
            key.takeIf { it.isNotEmpty() }?.let { it to value }
        }.toMap()

    private fun exists(path: String): Boolean = Shell.cmd("[ -e '$path' ]").exec().isSuccess

    private fun sh(cmd: String): List<String> = Shell.cmd(cmd).exec().out
}
