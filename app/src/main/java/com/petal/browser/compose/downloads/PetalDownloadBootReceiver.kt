package com.petal.browser.compose.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * PetalDownloadBootReceiver
 * Automatically triggers active downloads restoration via PetalChromiumDownloadService.
 */
class PetalDownloadBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i("PetalDownloadBoot", "Received $action: restoring active background downloads")
            PetalFetchDownloadBridge.ensureInitialized(context.applicationContext)
        }
    }
}
