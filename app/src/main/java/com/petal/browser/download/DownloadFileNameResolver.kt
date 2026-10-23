package com.petal.browser.download

import android.webkit.URLUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DownloadFileNameResolver {
    @JvmStatic
    fun resolve(
        scope: CoroutineScope,
        url: String,
        callback: (fileName: String, mimeType: String?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            val fileName = URLUtil.guessFileName(url, null, null)
            launch(Dispatchers.Main) {
                callback(fileName, null)
            }
        }
    }
}
