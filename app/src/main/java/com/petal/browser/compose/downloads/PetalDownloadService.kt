package com.petal.browser.compose.downloads

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * PetalDownloadService
 * Foreground service wrapper delegating directly to PetalChromiumDownloadService.
 */
class PetalDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    companion object {
        @JvmStatic
        fun start(context: Context, downloadId: Long = -1L, fileName: String? = null) {}

        @JvmStatic
        fun stopIfNoActiveDownloads(context: Context) {}

        @JvmStatic
        fun updatePersistentNotification(context: Context) {}
    }
}
