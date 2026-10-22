package com.petal.browser.compose.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * PetalDownloadBridge
 *
 * Connects Chromium's DownloadManagerService (via patch 0005 JNI) to Petal's
 * Compose download UI. Replaces the old Fetch2 / aria2c download engine entirely.
 *
 * Chromium handles:
 *  - Multi-threaded parallel chunk downloading
 *  - Resume-on-reconnect (partial content, ETag matching)
 *  - SSL certificate validation
 *  - MIME type detection and safe filename generation
 *
 * This bridge exposes reactive state (StateFlow) for the Compose UI and
 * action methods (pause/resume/cancel/open) that delegate to Chromium JNI.
 */
object PetalDownloadBridge {

    private val tag = "PetalDownloadBridge"

    private val _downloads = MutableStateFlow<List<PetalDownloadItem>>(emptyList())
    val downloads: StateFlow<List<PetalDownloadItem>> = _downloads.asStateFlow()

    /** Called on app start — registers Petal as a DownloadManagerService observer. */
    fun initialize() {
        try {
            val clazz = Class.forName(
                "org.chromium.chrome.browser.download.DownloadManagerService")
            // Register our observer
            clazz.getMethod("addPetalDownloadObserver", Any::class.java)
                .invoke(null, chromiumObserver)
            // Load existing download history
            refreshAll()
        } catch (e: Exception) {
            Log.w(tag, "initialize: $e")
        }
    }

    fun pauseDownload(guid: String) {
        invokeStatic("petalPauseDownload", guid)
    }

    fun resumeDownload(guid: String) {
        invokeStatic("petalResumeDownload", guid)
    }

    fun cancelDownload(guid: String) {
        invokeStatic("petalCancelDownload", guid)
        _downloads.value = _downloads.value.filter { it.guid != guid }
    }

    fun openFile(item: PetalDownloadItem) {
        try {
            val file = File(item.filePath)
            if (!file.exists()) return
            val context = org.chromium.base.ContextUtils.getApplicationContext()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(tag, "openFile: $e")
        }
    }

    private fun refreshAll() {
        try {
            val clazz = Class.forName(
                "org.chromium.chrome.browser.download.DownloadManagerService")
            @Suppress("UNCHECKED_CAST")
            val javaList = clazz.getMethod("petalGetAllDownloads").invoke(null) as? List<Any>
            _downloads.value = javaList?.map { mapItem(it) } ?: emptyList()
        } catch (e: Exception) {
            Log.w(tag, "refreshAll: $e")
        }
    }

    // Observer passed to Chromium's DownloadManagerService
    private val chromiumObserver = object : Any() {
        // Called via reflection from DownloadManagerService.notifyPetalObservers()
        @Suppress("unused")
        fun onDownloadUpdated(javaItem: Any) {
            val item = mapItem(javaItem)
            val current = _downloads.value.toMutableList()
            val idx = current.indexOfFirst { it.guid == item.guid }
            if (idx >= 0) current[idx] = item else current.add(0, item)
            _downloads.value = current
        }
    }

    private fun mapItem(javaItem: Any): PetalDownloadItem {
        val cls = javaItem.javaClass
        return PetalDownloadItem(
            guid          = cls.getMethod("getGuid").invoke(javaItem) as? String ?: "",
            fileName      = cls.getMethod("getFileName").invoke(javaItem) as? String ?: "",
            filePath      = cls.getMethod("getFilePath").invoke(javaItem) as? String ?: "",
            mimeType      = cls.getMethod("getMimeType").invoke(javaItem) as? String ?: "*/*",
            totalBytes    = cls.getMethod("getTotalBytes").invoke(javaItem) as? Long ?: 0L,
            receivedBytes = cls.getMethod("getReceivedBytes").invoke(javaItem) as? Long ?: 0L,
            state         = mapState(cls.getMethod("getState").invoke(javaItem))
        )
    }

    private fun mapState(chromiumState: Any?): DownloadState = when (chromiumState?.toString()) {
        "COMPLETE"    -> DownloadState.COMPLETE
        "IN_PROGRESS" -> DownloadState.IN_PROGRESS
        "PAUSED"      -> DownloadState.PAUSED
        "CANCELLED"   -> DownloadState.CANCELLED
        "FAILED"      -> DownloadState.FAILED
        else          -> DownloadState.IN_PROGRESS
    }

    private fun invokeStatic(method: String, vararg args: Any?) {
        try {
            val clazz = Class.forName(
                "org.chromium.chrome.browser.download.DownloadManagerService")
            val types = args.map { it?.javaClass ?: String::class.java }.toTypedArray()
            clazz.getMethod(method, *types).invoke(null, *args)
        } catch (e: Exception) {
            Log.w(tag, "$method: $e")
        }
    }
}

// ---------------------------------------------------------------------------
// Data models
// ---------------------------------------------------------------------------

data class PetalDownloadItem(
    val guid: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val totalBytes: Long,
    val receivedBytes: Long,
    val state: DownloadState
)

enum class DownloadState { IN_PROGRESS, PAUSED, COMPLETE, CANCELLED, FAILED }
