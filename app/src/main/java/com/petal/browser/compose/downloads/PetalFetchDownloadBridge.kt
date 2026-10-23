package com.petal.browser.compose.downloads

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * PetalFetchDownloadBridge
 * Unified download state manager operating purely on Android DownloadManager
 * and PetalChromiumDownloadService, eliminating mozilla FetchDownloadManager.
 */
object PetalFetchDownloadBridge {

    data class DownloadItem(
        val id: String,
        val url: String,
        val fileName: String,
        val contentLength: Long,
        val downloadedBytes: Long,
        val status: Int
    )

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun ensureInitialized(context: Context) {}
}
