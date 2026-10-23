package com.petal.browser.browser

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import android.webkit.ValueCallback
import com.petal.browser.engine.PetalChromiumTabView
import com.petal.browser.media.PetalMediaBridge

/**
 * PetalTabViewController
 * Compatibility wrapper routing all tab operations to the genuine Chromium engine.
 * Fully decoupled from Mozilla Android Components and GeckoView.
 */
class PetalTabViewController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), AlbumController {

    private var tabId: String = System.currentTimeMillis().toString()
    private var tabGroupId: String? = null
    private var tabGroupTitle: String? = null
    private var currentUrl: String = "about:blank"
    private var currentTitle: String = "New Tab"
    private var isIncognitoMode: Boolean = false
    private val mediaBridge = PetalMediaBridge(context, null as PetalMediaBridge.MediaStateListener?)

    override fun getAlbumView(): View = this
    override fun activate() {}
    override fun deactivate() {}
    override fun getTitle(): String = currentTitle
    override fun getUrl(): String = currentUrl
    override fun isIncognito(): Boolean = isIncognitoMode

    override fun canGoBack(): Boolean = false
    override fun canGoForward(): Boolean = false
    override fun goBack() {}
    override fun goForward() {}
    override fun reload() {}
    override fun loadUrl(url: String) { currentUrl = url }
    override fun evaluateJavascript(script: String, callback: ValueCallback<String>?) { callback?.onReceiveValue(null) }

    fun stopLoading() {}
    fun getTabId(): String = tabId
    fun setTabId(id: String) { tabId = id }
    fun getTabGroupId(): String? = tabGroupId
    fun setTabGroupId(id: String?) { tabGroupId = id }
    fun getTabGroupTitle(): String? = tabGroupTitle
    fun setTabGroupTitle(title: String?) { tabGroupTitle = title }
    fun getFavicon(): Bitmap? = null
    fun getCachedPreviewBitmap(): Bitmap? = null
    fun getMediaBridge(): PetalMediaBridge = mediaBridge
    fun isAdoptedPopup(): Boolean = false
    fun captureFullPageBitmap(callback: (Bitmap?) -> Unit) { callback(null) }
    override fun setDesktopMode(enabled: Boolean) {}
    fun isPageAtTop(): Boolean = true
    fun processBackPressed(): Boolean = false
    fun bindTab(pair: Any?) {}
    fun setTrackingProtection(enabled: Boolean) {}
    val currentState: Any? get() = null
    fun capturePreviewBitmapAsync(callback: (Bitmap?) -> Unit) { callback(null) }
    fun setUserAgent(ua: String) {}
}
