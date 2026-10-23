package com.petal.browser.engine

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.petal.browser.browser.AlbumController
import com.petal.browser.ui.glass.ChromiumGlassBackend

object PetalTabThumbnailCache {
    private val cache = mutableMapOf<String, Bitmap>()
    fun get(id: String): Bitmap? = cache[id]
    fun getThumbnail(id: String): Bitmap? = cache[id]
    fun put(id: String, bm: Bitmap?) { if (bm != null) cache[id] = bm else cache.remove(id) }
    fun putThumbnail(id: String, bm: Bitmap?) { put(id, bm) }
    fun removeThumbnail(id: String) { cache.remove(id) }
    fun clear() { cache.clear() }
}

class PetalChromiumTabView(
    private val chromiumTab: Any?,
    private val contentView: View,
    private val incognitoMode: Boolean,
    val tabId: String
) : View(contentView.context), AlbumController {

    private val tag = "PetalChromiumTabView"

    var currentTitle: String = "New Tab"
        private set
    var currentUrl: String = "about:blank"
        private set
    var currentProgress: Float = 1.0f
        private set

    override fun getAlbumView(): View = contentView
    override fun getTitle(): String = currentTitle
    override fun getUrl(): String = currentUrl
    override fun isIncognito(): Boolean = incognitoMode
    override fun activate() {}
    override fun deactivate() {}
    override fun destroy() {
        PetalTabThumbnailCache.removeThumbnail(tabId)
    }

    fun loadUrl(url: String, headers: Map<String, String>? = null) {
        currentUrl = url
    }

    override fun canGoBack(): Boolean = false
    override fun canGoForward(): Boolean = false
    override fun goBack() {}
    override fun goForward() {}
    override fun reload() {}
    fun stop() {}

    fun updateProgress(progress: Float) {
        currentProgress = progress
    }

    fun updateTitle(title: String) {
        currentTitle = title
    }

    fun updateUrl(url: String) {
        currentUrl = url
    }

    fun captureThumbnail(callback: ((Bitmap?) -> Unit)? = null) {
        if (contentView.width <= 0 || contentView.height <= 0) {
            callback?.invoke(null)
            return
        }

        try {
            val bitmap = Bitmap.createBitmap(
                contentView.width / 2,
                contentView.height / 2,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            canvas.scale(0.5f, 0.5f)
            contentView.draw(canvas)
            PetalTabThumbnailCache.putThumbnail(tabId, bitmap)
            callback?.invoke(bitmap)
        } catch (e: Throwable) {
            callback?.invoke(null)
        }
    }

    fun getThumbnail(): Bitmap? = PetalTabThumbnailCache.getThumbnail(tabId)

    fun attachGlassBackend() {
        ChromiumGlassBackend.attachTextureListener(contentView)
    }

    override fun getFavicon(): Bitmap? = null
    fun getCachedPreviewBitmap(): Bitmap? = getThumbnail()
    fun getMediaBridge(): com.petal.browser.media.PetalMediaBridge? = null
    fun capturePreviewBitmapAsync(callback: (Bitmap?) -> Unit) {
        captureThumbnail { callback(it) }
    }
    override fun getTabId(): String = tabId
    override fun getPersistentUrl(): String = currentUrl
}
