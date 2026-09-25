package com.skofqq.boxy.ui.logs

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LogShare {
    suspend fun share(context: Context, path: String): Boolean {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val out = File(dir, "boxy-" + path.substringAfterLast('/'))
            val uid = context.applicationInfo.uid
            val q = { s: String -> "'" + s.replace("'", "'\\''") + "'" }
            val ok = Shell.cmd("cp ${q(path)} ${q(out.path)} && chown $uid:$uid ${q(out.path)} && chmod 600 ${q(out.path)}").exec().isSuccess
            out.takeIf { ok && it.canRead() }
        } ?: return false
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, file.name)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, file.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
