package com.careerops.mobile.diagnostics

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Very small file-based session logger for debugging on real devices without USB.
 *
 * - Writes directly to Downloads/CareerOpsMobile via MediaStore so it's accessible in Files.
 * - Uses append mode for every write (simple + robust across OEMs; slightly slower but fine).
 */
object AppLogger {
    private const val RELATIVE_DOWNLOADS_DIR = "Download/CareerOpsMobile"
    private val enabled = AtomicBoolean(false)

    @Volatile
    private var currentUri: Uri? = null

    @Volatile
    private var currentDisplayName: String? = null

    fun isEnabled(): Boolean = enabled.get()

    fun start(context: Context): String {
        val app = context.applicationContext
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val displayName = "career-ops-live-$stamp.log"
        val uri = createDownloadsFile(app, displayName)
        currentUri = uri
        currentDisplayName = displayName
        enabled.set(true)
        log(app, "logger", "started file=$displayName")
        return displayName
    }

    fun stop(context: Context) {
        val app = context.applicationContext
        log(app, "logger", "stopped")
        enabled.set(false)
        currentUri = null
        currentDisplayName = null
    }

    fun log(context: Context, tag: String, message: String) {
        if (!enabled.get()) return
        val uri = currentUri ?: return
        val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        runCatching {
            val line = "[$ts] $tag: $message\n"
            val resolver = context.applicationContext.contentResolver
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "wa" else "w"
            var out: OutputStream? = null
            try {
                out = resolver.openOutputStream(uri, mode)
                out?.write(line.toByteArray())
                out?.flush()
            } finally {
                runCatching { out?.close() }
            }
        }
    }

    fun currentLogFileName(): String? = currentDisplayName

    private fun createDownloadsFile(context: Context, displayName: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_DOWNLOADS_DIR)
            }
        }
        return runCatching {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        }.getOrNull()
    }
}

