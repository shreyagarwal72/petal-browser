package com.petal.browser.compose.downloads

import android.content.Context
import com.tonyodev.fetch2.Fetch
import com.tonyodev.fetch2.FetchConfiguration
import com.tonyodev.fetch2okhttp.OkHttpDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * PetalFetchDownloadBridge
 * Unified download state manager operating on Fetch2.
 */
object PetalFetchDownloadBridge {

    data class DownloadItem(
        val id: Long = 0L,
        val fileName: String = "",
        val fileUrl: String = "",
        val progress: Float? = 0f,
        val status: Int = 0,
        val bytesDownloaded: Long = 0L,
        val totalSize: Long = 0L,
        val speedBytesPerSec: Long = 0L,
        val etaSeconds: Long = 0L,
        val localUri: String? = null,
        val timestampMs: Long = System.currentTimeMillis()
    )

    private val _downloadItems = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadItems: StateFlow<List<DownloadItem>> = _downloadItems.asStateFlow()

    private var fetch: Fetch? = null

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (fetch == null) {
            val config = FetchConfiguration.Builder(context.applicationContext)
                .setDownloadConcurrentLimit(4)
                .setHttpDownloader(OkHttpDownloader())
                .build()
            fetch = Fetch.getInstance(config)
        }
    }

    @Synchronized
    fun fetchInstance(context: Context): Fetch {
        ensureInitialized(context)
        return fetch!!
    }

    fun refresh(context: Context) {}

    fun pause(context: Context, downloadId: Long) {
        runCatching { fetchInstance(context).pause(downloadId.toInt()) }
    }

    fun resume(context: Context, downloadId: Long) {
        runCatching { fetchInstance(context).resume(downloadId.toInt()) }
    }

    fun cancel(context: Context, downloadId: Long) {
        runCatching { fetchInstance(context).cancel(downloadId.toInt()) }
    }

    fun retry(context: Context, downloadId: Long) {
        runCatching { fetchInstance(context).retry(downloadId.toInt()) }
    }

    fun deleteDownload(context: Context, item: Any?) {}
    fun deleteDownloads(context: Context, items: List<Any?>, deleteFiles: Boolean = false) {}

    fun enqueueMediaDownload(
        context: Context,
        url: String,
        suggestedFileName: String?,
        mimeType: String?,
        userAgent: String?,
        contentLength: Long = -1L,
        cookies: String? = null,
        referrer: String? = null
    ): Long = 0L

    fun enqueueGeckoDownload(
        context: Context,
        url: String,
        fileName: String,
        mimeType: String?,
        contentLength: Long,
        userAgent: String?,
        cookies: String?
    ): Long = 0L
}
