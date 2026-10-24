package com.petal.browser.browser

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.petal.browser.media.PetalMediaBridge

open class PetalTabViewController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), AlbumController {

    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var tabId: String = ""
    private var tabGroupId: String? = null
    private var tabGroupTitle: String? = null
    private val mediaBridge = PetalMediaBridge(context)

    data class BrowserState(
        val url: String = "",
        val title: String = "",
        val progress: Int = 100,
        val isSecure: Boolean = false,
        val loading: Boolean = false
    )

    fun interface OnBrowserStateChangeListener {
        fun onStateChanged(state: BrowserState)
    }

    private var stateChangeListener: OnBrowserStateChangeListener? = null

    fun getOnBrowserStateChanged(): OnBrowserStateChangeListener? = stateChangeListener

    fun setOnBrowserStateChanged(listener: OnBrowserStateChangeListener?) {
        this.stateChangeListener = listener
    }

    fun setOnBrowserStateChanged(listener: (BrowserState) -> Unit) {
        this.stateChangeListener = OnBrowserStateChangeListener { listener(it) }
    }

    override fun getAlbumView(): View = this
    override fun getUrl(): String? = currentUrl
    fun setUrl(url: String?) {
        currentUrl = url
        notifyState()
    }

    override fun getTitle(): String? = currentTitle
    fun setTitle(title: String?) {
        currentTitle = title
        notifyState()
    }

    override fun getPersistentUrl(): String? = currentUrl
    override fun getTabId(): String = tabId
    fun setTabId(id: String) { tabId = id }
    override fun getTabGroupId(): String? = tabGroupId
    override fun setTabGroupId(id: String?) { tabGroupId = id }
    override fun getTabGroupTitle(): String? = tabGroupTitle
    override fun setTabGroupTitle(title: String?) { tabGroupTitle = title }

    override fun getFavicon(): Bitmap? = null
    fun getCachedPreviewBitmap(): Bitmap? = null
    override fun getMediaBridge(): PetalMediaBridge = mediaBridge
    fun isAdoptedPopup(): Boolean = false
    fun captureFullPageBitmap(callback: (Bitmap?) -> Unit) { callback(null) }
    override fun setDesktopMode(enabled: Boolean) {}
    fun isPageAtTop(): Boolean = true
    fun bindTab(pair: Any?) {}
    fun setTrackingProtection(enabled: Boolean) {}
    val currentState: BrowserState get() = BrowserState(currentUrl ?: "", currentTitle ?: "", progress, isSecure)
    fun capturePreviewBitmapAsync(callback: (Bitmap?) -> Unit) { callback(null) }
    fun setUserAgent(ua: String) {}

    var progress: Int = 0
        set(value) {
            field = value
            notifyState()
        }
    var isSecure: Boolean = false
        set(value) {
            field = value
            notifyState()
        }

    fun attachLifecycle(owner: Any?) {}

    fun processBackPressed(onFallback: (() -> Unit)? = null): Boolean {
        if (canGoBack()) {
            goBack()
            return true
        }
        onFallback?.invoke()
        return false
    }

    fun processBackPressed(fallback: Boolean): Boolean {
        if (canGoBack()) {
            goBack()
            return true
        }
        return fallback
    }

    private var predecessor: AlbumController? = null
    fun getPredecessor(): AlbumController? = predecessor
    fun setPredecessor(controller: AlbumController?) { predecessor = controller }

    fun clearFindMatches() {}
    fun findAll(query: String) {}
    fun findNext(forward: Boolean) {}

    fun reloadBypassCache() { reload() }

    override fun canGoBack(): Boolean = false
    override fun goBack() {}
    override fun reload() {}
    override fun loadUrl(url: String) {
        currentUrl = url
        notifyState()
    }
    override fun activate() {}
    override fun deactivate() {}
    override fun destroy() {}

    private fun notifyState() {
        stateChangeListener?.onStateChanged(currentState)
    }

    typealias State = BrowserState

    fun captureForSwitcher(callback: java.util.function.Consumer<Bitmap?>) {
        callback.accept(null)
    }
    fun captureForSwitcher(callback: (Bitmap?) -> Unit) {
        callback(null)
    }
    fun currentState(): BrowserState = currentState
    fun loadData(data: String?, mimeType: String?, encoding: String?) {
        loadUrl("about:blank")
    }
    private var pwaManager: com.petal.browser.pwa.PetalPwaManager? = null
    fun getPwaManager(): com.petal.browser.pwa.PetalPwaManager? = pwaManager
    fun setPwaManager(manager: com.petal.browser.pwa.PetalPwaManager?) { pwaManager = manager }
    fun printToPdf(fos: java.io.FileOutputStream, callback: java.util.function.Consumer<Boolean>) {
        callback.accept(false)
    }
    fun printToPdf(fos: java.io.FileOutputStream, callback: (Boolean) -> Unit) {
        callback(false)
    }

}
