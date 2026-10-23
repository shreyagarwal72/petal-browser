package com.petal.browser.download

import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DownloadFileNameResolver {
    @JvmStatic
    fun resolve(
        scope: CoroutineScope,
        url: String,
        fallbackMimeType: String? = null,
        callback: (fileName: String, mimeType: String?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val mimeType = fallbackMimeType ?: run {
                val ext = MimeTypeMap.getFileExtensionFromUrl(url)
                if (!ext.isNullOrBlank()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) else null
            }
            val fileName = URLUtil.guessFileName(url, null, mimeType)
            launch(Dispatchers.Main) {
                callback(fileName, mimeType)
            }
        }
    }
}
