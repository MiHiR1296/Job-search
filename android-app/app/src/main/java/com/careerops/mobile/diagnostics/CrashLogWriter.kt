package com.careerops.mobile.diagnostics

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.careerops.mobile.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes Java/Kotlin crash reports under [Context.getFilesDir]/crash_logs.
 * Does **not** capture native SIGSEGV from JNI (process dies before handler runs).
 */
object CrashLogWriter {

    private const val CRASH_DIR = "crash_logs"
    private const val MAX_FILES = 12

    fun installUncaughtExceptionHandler(app: Context) {
        val ctx = app.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeThrowable(ctx, "uncaught", thread, throwable)
            } catch (_: Throwable) {
                // ignore secondary failures
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Log Kotlin failures that were caught (e.g. generation pipeline) so we still get a file without USB.
     */
    fun writeCaughtThrowable(context: Context, label: String, throwable: Throwable) {
        try {
            writeThrowable(context.applicationContext, "caught-$label", Thread.currentThread(), throwable)
        } catch (_: Throwable) {
        }
    }

    private fun writeThrowable(
        appContext: Context,
        prefix: String,
        thread: Thread,
        throwable: Throwable
    ) {
        val dir = File(appContext.filesDir, CRASH_DIR).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(dir, "$prefix-$stamp.txt")
        file.writer().use { w ->
            w.appendLine(headerBlock(appContext))
            w.appendLine("thread=${thread.name}")
            w.appendLine("--- stacktrace ---")
            throwable.printStackTrace(PrintWriter(w))
        }
        trimOldFiles(dir)
    }

    private fun headerBlock(context: Context): String {
        val rt = Runtime.getRuntime()
        val sw = StringWriter()
        sw.appendLine("Career Ops Mobile diagnostic")
        sw.appendLine("versionName=${BuildConfig.VERSION_NAME}")
        sw.appendLine("versionCode=${BuildConfig.VERSION_CODE}")
        sw.appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
        sw.appendLine("sdk=${Build.VERSION.SDK_INT}")
        sw.appendLine("maxMemoryMB=${rt.maxMemory() / (1024 * 1024)}")
        sw.appendLine("totalMemoryMB=${rt.totalMemory() / (1024 * 1024)}")
        sw.appendLine("freeMemoryMB=${rt.freeMemory() / (1024 * 1024)}")
        return sw.toString().trimEnd()
    }

    private fun trimOldFiles(dir: File) {
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }.orEmpty()
        if (files.size <= MAX_FILES) return
        val deleteCount = files.size - MAX_FILES
        files.sortedBy { it.lastModified() }.take(deleteCount).forEach { f ->
            runCatching { f.delete() }
        }
    }

    fun latestCrashFile(context: Context): File? {
        val dir = File(context.applicationContext.filesDir, CRASH_DIR)
        if (!dir.isDirectory) return null
        return dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.maxByOrNull { it.lastModified() }
    }

    /**
     * Builds a shareable text file (latest crash + memory header) and opens the system share sheet.
     */
    fun shareDiagnosticsBundle(activityContext: Context) {
        val app = activityContext.applicationContext
        val sb = StringBuilder()
        sb.appendLine(headerBlock(app))
        sb.appendLine()
        sb.appendLine("--- Notes ---")
        sb.appendLine(
            "If the app closed with NO error message, this file may be empty or old: " +
                "native crashes (OOM/SIGSEGV in libllama) often kill the process before Java can log. " +
                "Try API mode, smaller GGUF, or fewer background apps."
        )
        sb.appendLine()
        val latest = latestCrashFile(app)
        if (latest != null && latest.exists()) {
            sb.appendLine("--- Latest crash log: ${latest.name} ---")
            sb.appendLine(latest.readText().take(200_000))
        } else {
            sb.appendLine("(No crash_logs/*.txt yet — only uncaught Java errors or caught pipeline errors create these.)")
        }

        val outDir = File(app.cacheDir, "diagnostics_share").apply { mkdirs() }
        val out = File(outDir, "career-ops-diagnostics.txt")
        out.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            out
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Career Ops Mobile diagnostics")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("diagnostics", uri)
        }
        activityContext.startActivity(
            Intent.createChooser(send, "Share diagnostics").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }
}
