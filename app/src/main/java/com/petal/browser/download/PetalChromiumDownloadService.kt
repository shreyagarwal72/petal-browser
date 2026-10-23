package com.petal.browser.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.petal.browser.R
import java.lang.reflect.Method

/**
 * Chromium-backed download foreground service.
 *
 * Replaces [PetalMozillaDownloadService] which depended on Mozilla Android Components
 * (AbstractFetchDownloadService / BrowserStore / Fetch2).
 *
 * This service sits as a thin Android wrapper around Chromium's own download pipeline:
 *   • DownloadManagerService  (native, JNI)
 *   • DownloadNotifier        (native, notifies this service)
 *
 * Chromium starts its own download threads internally; we only need:
 *   1. A foreground service declaration so Android doesn't kill the process.
 *   2. Notification management (progress, pause, cancel, complete).
 *   3. A Binder so Petal's DownloadManager UI can query active downloads.
 *
 * JNI wiring is done via reflection (same pattern as PetalDownloadBridge.kt)
 * to avoid compile-time dependency on Chromium's Java classes when building
 * the standalone Petal Gradle module.
 */
class PetalChromiumDownloadService : Service() {

    companion object {
        // Download actions & extras for Petal UI & LiveAlertManager
        const val ACTION_DOWNLOAD_START  = "com.petal.browser.download.START"
        const val ACTION_DOWNLOAD_PAUSE  = "com.petal.browser.download.PAUSE"
        const val ACTION_DOWNLOAD_RESUME = "com.petal.browser.download.RESUME"
        const val ACTION_DOWNLOAD_CANCEL = "com.petal.browser.download.CANCEL"

        const val EXTRA_DOWNLOAD_URL = "extra_download_url"
        const val EXTRA_FILE_NAME    = "extra_file_name"
        const val EXTRA_MIME_TYPE    = "extra_mime_type"

        @JvmStatic
        fun start(context: Context) {
            try {
                val intent = Intent(context, PetalChromiumDownloadService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "start: ${e.message}")
            }
        }

        @JvmStatic
        fun stopIfNoActiveDownloads(context: Context) {}

        @JvmStatic
        fun updatePersistentNotification(context: Context) {}

        private const val TAG = "PetalChromiumDlSvc"
        private const val NOTIFICATION_CHANNEL_ID = "petal_downloads"
        private const val NOTIFICATION_CHANNEL_NAME = "Petal Downloads"
        private const val FOREGROUND_NOTIFICATION_ID = 0x2A79  // arbitrary non-zero

        // Intent actions sent by PetalDownloadBridge
        const val ACTION_PAUSE   = "com.petal.browser.download.PAUSE"
        const val ACTION_RESUME  = "com.petal.browser.download.RESUME"
        const val ACTION_CANCEL  = "com.petal.browser.download.CANCEL"
        const val ACTION_OPEN    = "com.petal.browser.download.OPEN"

        const val EXTRA_DOWNLOAD_ID = "download_id"
    }

    // ── Binder ─────────────────────────────────────────────────────────────────
    inner class LocalBinder : Binder() {
        fun getService(): PetalChromiumDownloadService = this@PetalChromiumDownloadService
    }

    private val binder = LocalBinder()

    // ── Chromium JNI handles (resolved via reflection) ─────────────────────────
    private var chromiumDownloadManagerService: Any? = null
    private var pauseMethod: Method? = null
    private var resumeMethod: Method? = null
    private var cancelMethod: Method? = null

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(FOREGROUND_NOTIFICATION_ID, buildIdleNotification())
        resolveChromiumJni()
        Log.i(TAG, "PetalChromiumDownloadService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE  -> handlePause(intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1))
            ACTION_RESUME -> handleResume(intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1))
            ACTION_CANCEL -> handleCancel(intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1))
            ACTION_OPEN   -> { /* handled by PetalDownloadBridge */ }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "PetalChromiumDownloadService destroyed")
    }

    // ── Notification ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Petal Browser download progress"
                setShowBadge(false)
            }
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildIdleNotification(): Notification =
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Petal Downloads")
            .setContentText("Ready")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

    /**
     * Called by PetalDownloadBridge (via BroadcastReceiver) when Chromium's
     * DownloadManagerService fires a progress callback.
     */
    fun updateDownloadNotification(
        downloadId: Long,
        fileName: String,
        progressPercent: Int,
        bytesDownloaded: Long,
        totalBytes: Long,
        isPaused: Boolean,
        isComplete: Boolean
    ) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = (FOREGROUND_NOTIFICATION_ID + downloadId).toInt()

        val title = when {
            isComplete -> getString(R.string.download_complete_title, fileName)
            isPaused   -> getString(R.string.download_paused_title, fileName)
            else       -> getString(R.string.download_in_progress_title, fileName)
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(
                if (isComplete) R.drawable.ic_download
                else R.drawable.ic_download
            )
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!isComplete)
            .setShowWhen(isComplete)
            .setAutoCancel(isComplete)

        if (!isComplete && totalBytes > 0) {
            builder.setProgress(100, progressPercent, false)
            builder.setContentText("$progressPercent% — ${formatBytes(bytesDownloaded)} / ${formatBytes(totalBytes)}")
        } else if (!isComplete) {
            builder.setProgress(0, 0, true)
            builder.setContentText(formatBytes(bytesDownloaded))
        }

        if (!isComplete && !isPaused) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    buildActionPendingIntent(ACTION_PAUSE, downloadId)
                )
            )
        }
        if (!isComplete && isPaused) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    "Resume",
                    buildActionPendingIntent(ACTION_RESUME, downloadId)
                )
            )
        }
        if (!isComplete) {
            builder.addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    buildActionPendingIntent(ACTION_CANCEL, downloadId)
                )
            )
        }

        mgr.notify(notifId, builder.build())
    }

    // ── Chromium JNI proxy ─────────────────────────────────────────────────────

    private fun resolveChromiumJni() {
        try {
            val cls = Class.forName(
                "org.chromium.chrome.browser.download.ChromeDownloadDelegate",
                false,
                classLoader
            )
            chromiumDownloadManagerService = cls
            pauseMethod  = cls.getMethod("pauseDownload",  Long::class.java)
            resumeMethod = cls.getMethod("resumeDownload", Long::class.java)
            cancelMethod = cls.getMethod("cancelDownload", Long::class.java)
            Log.i(TAG, "Chromium download JNI resolved via reflection")
        } catch (e: Exception) {
            // Running in standalone/lint mode without Chromium — no-op
            Log.w(TAG, "Chromium download JNI not available: ${e.message}")
        }
    }

    private fun handlePause(downloadId: Long) {
        if (downloadId < 0) return
        try {
            pauseMethod?.invoke(chromiumDownloadManagerService, downloadId)
            Log.d(TAG, "Paused download $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause $downloadId: ${e.message}")
        }
    }

    private fun handleResume(downloadId: Long) {
        if (downloadId < 0) return
        try {
            resumeMethod?.invoke(chromiumDownloadManagerService, downloadId)
            Log.d(TAG, "Resumed download $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume $downloadId: ${e.message}")
        }
    }

    private fun handleCancel(downloadId: Long) {
        if (downloadId < 0) return
        try {
            cancelMethod?.invoke(chromiumDownloadManagerService, downloadId)
            Log.d(TAG, "Cancelled download $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel $downloadId: ${e.message}")
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun buildActionPendingIntent(action: String, downloadId: Long): PendingIntent {
        val intent = Intent(this, PetalChromiumDownloadService::class.java).apply {
            this.action = action
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        return PendingIntent.getService(
            this,
            downloadId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "?"
        return when {
            bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576L     -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1_024L         -> "%.1f KB".format(bytes / 1_024.0)
            else                    -> "$bytes B"
        }
    }
}
